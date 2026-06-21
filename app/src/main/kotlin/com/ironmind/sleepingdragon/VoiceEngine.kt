package com.ironmind.sleepingdragon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.ironmind.sleepingdragon.core.AppConstants
import com.ironmind.sleepingdragon.domain.NarratorRole
import com.ironmind.sleepingdragon.domain.SpeakSegment
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class VoiceEngine(private val context: Context) {

    interface Listener {
        fun onListeningChanged(active: Boolean)
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onEarlyCommand(text: String)
        fun onError(message: String)
        fun onSpeakingChanged(active: Boolean)
        fun onNarratorRoleChanged(role: NarratorRole?) {}
    }

    companion object {
        private const val ECHO_GUARD_MS = AppConstants.ECHO_GUARD_MS
        private const val SESSION_RENEWAL_MS = AppConstants.SESSION_RENEWAL_MS
        private const val PARTIAL_CONFIRM_MS = AppConstants.PARTIAL_CONFIRM_MS
        private const val ERROR_RETRY_MS = AppConstants.ERROR_RETRY_MS
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var listener: Listener? = null

    private var tts: TextToSpeech? = null
    private val isTtsReady = AtomicBoolean(false)
    private val isSpeaking = AtomicBoolean(false)
    private val isListening = AtomicBoolean(false)
    private val isShutdown = AtomicBoolean(false)
    private val keepAlive = AtomicBoolean(false)

    private var speechRecognizer: SpeechRecognizer? = null
    private var pendingSpeakComplete: (() -> Unit)? = null
    private var lastSpokenText: String = ""
    private var sequenceGeneration = 0
    private var activeSequenceGeneration = 0
    private var activeNarratorRole: NarratorRole? = null

    var choiceHintsProvider: (() -> List<String>)? = null

    private var pendingPartialText: String? = null
    private var partialConfirmRunnable: Runnable? = null

    private val wakeRecognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
    }

    private val gameRecognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
    }

    private var wakeMode = true

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.GERMAN)
                isTtsReady.set(
                    result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                )
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking.set(true)
                        notifySpeaking(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        finishSpeaking()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        finishSpeaking()
                    }
                })
            }
        }
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun isSpeechAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun setGameMode(active: Boolean) {
        wakeMode = !active
    }

    fun speak(
        text: String,
        role: NarratorRole = NarratorRole.OLD_MAN,
        onComplete: (() -> Unit)? = null
    ) {
        if (isShutdown.get()) return

        lastSpokenText = text
        keepAlive.set(true)
        cancelPartialConfirm()

        if (!isTtsReady.get()) {
            mainHandler.postDelayed({ speak(text, role, onComplete) }, 250)
            return
        }

        applyNarratorRole(role)
        pendingSpeakComplete = onComplete
        val utteranceId = "sd_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, role.volume)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)

        mainHandler.postDelayed({
            if (!isShutdown.get() && keepAlive.get() && activeSequenceGeneration == 0) {
                startListening(allowDuringSpeech = true)
            }
        }, ECHO_GUARD_MS)
    }

    fun speakSequence(segments: List<SpeakSegment>, onComplete: (() -> Unit)? = null) {
        if (isShutdown.get()) {
            onComplete?.invoke()
            return
        }
        val cleaned = segments.filter { it.text.isNotBlank() }
        if (cleaned.isEmpty()) {
            onComplete?.invoke()
            return
        }

        val generation = ++sequenceGeneration
        activeSequenceGeneration = generation
        speakSegmentChain(cleaned, 0, generation) {
            activeSequenceGeneration = 0
            onComplete?.invoke()
        }
    }

    private fun speakSegmentChain(
        segments: List<SpeakSegment>,
        index: Int,
        generation: Int,
        onComplete: (() -> Unit)?
    ) {
        if (isShutdown.get() || generation != sequenceGeneration) return

        val segment = segments[index]
        speak(segment.text, segment.role) {
            if (isShutdown.get() || generation != sequenceGeneration) return@speak

            val pauseAfter = segment.pauseAfterMs ?: defaultPauseAfter(segment.role, index, segments.size)
            if (index + 1 < segments.size) {
                mainHandler.postDelayed({
                    speakSegmentChain(segments, index + 1, generation, onComplete)
                }, pauseAfter)
            } else {
                onComplete?.invoke()
            }
        }
    }

    private fun defaultPauseAfter(role: NarratorRole, index: Int, total: Int): Long =
        when {
            index >= total - 1 -> 0L
            role == NarratorRole.OLD_MAN -> AppConstants.NARRATOR_BEAT_PAUSE_MS
            else -> AppConstants.FAIRY_PRE_PAUSE_MS
        }

    private fun applyNarratorRole(role: NarratorRole) {
        activeNarratorRole = role
        notifyNarratorRole(role)
        tts?.setPitch(role.pitch)
        tts?.setSpeechRate(role.speechRate)
    }

    fun startListening(allowDuringSpeech: Boolean = false) {
        if (isShutdown.get()) return
        if (isSpeaking.get() && !allowDuringSpeech) return
        if (!isSpeechAvailable()) {
            listener?.onError("Spracherkennung auf diesem Gerät nicht verfügbar.")
            return
        }

        keepAlive.set(true)

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            speechRecognizer?.setRecognitionListener(recognitionListener)
        }

        try {
            speechRecognizer?.cancel()
            val intent = if (wakeMode) wakeRecognitionIntent else gameRecognitionIntent
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            listener?.onError("Mikrofon konnte nicht gestartet werden.")
        }
    }

    fun stopListening() {
        isListening.set(false)
        speechRecognizer?.cancel()
        cancelPartialConfirm()
        notifyListening(false)
    }

    fun stopSpeaking() {
        sequenceGeneration++
        activeSequenceGeneration = 0
        if (isSpeaking.getAndSet(false)) {
            tts?.stop()
            notifySpeaking(false)
            notifyNarratorRole(null)
            pendingSpeakComplete?.invoke()
            pendingSpeakComplete = null
        }
    }

    fun pause() {
        keepAlive.set(false)
        stopListening()
        stopSpeaking()
    }

    fun shutdown() {
        isShutdown.set(true)
        keepAlive.set(false)
        pause()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
        isTtsReady.set(false)
        listener = null
        pendingSpeakComplete = null
        choiceHintsProvider = null
    }

    private fun finishSpeaking() {
        isSpeaking.set(false)
        notifySpeaking(false)
        notifyNarratorRole(null)
        val callback = pendingSpeakComplete
        pendingSpeakComplete = null
        mainHandler.post {
            callback?.invoke()
            if (keepAlive.get() && !isShutdown.get() && activeSequenceGeneration == 0) {
                mainHandler.postDelayed({ startListening() }, ECHO_GUARD_MS)
            }
        }
    }

    private fun scheduleSessionRenewal() {
        if (!keepAlive.get() || isShutdown.get()) return
        mainHandler.postDelayed({
            if (keepAlive.get() && !isListening.get() && !isShutdown.get()) {
                startListening(allowDuringSpeech = isSpeaking.get())
            }
        }, SESSION_RENEWAL_MS)
    }

    private fun cancelPartialConfirm(clearPending: Boolean = true) {
        partialConfirmRunnable?.let { mainHandler.removeCallbacks(it) }
        partialConfirmRunnable = null
        if (clearPending) pendingPartialText = null
    }

    private fun evaluatePartial(text: String) {
        val hints = choiceHintsProvider?.invoke() ?: emptyList()
        val matched = VoiceMatcher.matchCommand(text, hints)
        if (matched == null) return

        if (isSpeaking.get() && !VoiceMatcher.isBargeIn(text)) return

        if (pendingPartialText == text && partialConfirmRunnable != null) return

        cancelPartialConfirm(clearPending = false)
        pendingPartialText = text

        val runnable = Runnable {
            if (pendingPartialText == text && !isShutdown.get()) {
                stopListening()
                if (isSpeaking.get()) stopSpeaking()
                listener?.onEarlyCommand(text)
            }
            cancelPartialConfirm(clearPending = true)
        }
        partialConfirmRunnable = runnable
        mainHandler.postDelayed(runnable, PARTIAL_CONFIRM_MS)
    }

    private fun isLikelyEcho(raw: String): Boolean {
        if (lastSpokenText.isBlank()) return false
        val spoken = VoiceMatcher.normalize(lastSpokenText)
        val heard = VoiceMatcher.normalize(raw)
        if (heard.length < 8) return false
        return spoken.contains(heard) || heard.contains(spoken.take(heard.length.coerceAtLeast(1)))
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening.set(true)
            notifyListening(true)
        }

        override fun onBeginningOfSpeech() {
            if (isSpeaking.get()) {
                stopSpeaking()
            }
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() {
            isListening.set(false)
            notifyListening(false)
        }

        override fun onError(error: Int) {
            isListening.set(false)
            notifyListening(false)
            if (isShutdown.get() || !keepAlive.get()) return

            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> scheduleSessionRenewal()
                SpeechRecognizer.ERROR_CLIENT -> Unit
                else -> {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO ->
                            "Mikrofon-Fehler. Prüfe die Berechtigung."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Netzwerk-Fehler bei der Spracherkennung."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Mikrofon-Berechtigung fehlt."
                        else -> "Spracherkennung unterbrochen."
                    }
                    listener?.onError(message)
                    mainHandler.postDelayed({ scheduleSessionRenewal() }, ERROR_RETRY_MS)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            isListening.set(false)
            notifyListening(false)
            cancelPartialConfirm()

            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()

            if (text.isNullOrBlank()) {
                scheduleSessionRenewal()
                return
            }
            if (isLikelyEcho(text)) {
                scheduleSessionRenewal()
                return
            }

            stopListening()
            if (isSpeaking.get()) stopSpeaking()
            listener?.onFinalResult(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
            if (text.isNullOrBlank()) return
            if (isLikelyEcho(text)) return

            listener?.onPartialResult(text)
            evaluatePartial(text)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun notifyListening(active: Boolean) {
        mainHandler.post { listener?.onListeningChanged(active) }
    }

    private fun notifySpeaking(active: Boolean) {
        mainHandler.post { listener?.onSpeakingChanged(active) }
    }

    private fun notifyNarratorRole(role: NarratorRole?) {
        activeNarratorRole = role
        mainHandler.post { listener?.onNarratorRoleChanged(role) }
    }
}