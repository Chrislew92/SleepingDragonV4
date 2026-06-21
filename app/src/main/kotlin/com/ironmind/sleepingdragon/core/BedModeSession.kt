package com.ironmind.sleepingdragon.core

import android.content.Context
import com.ironmind.sleepingdragon.CommandRegistry
import com.ironmind.sleepingdragon.GameEngine
import com.ironmind.sleepingdragon.VoiceEngine
import com.ironmind.sleepingdragon.data.GameSaveRepository
import android.os.Handler
import android.os.Looper
import com.ironmind.sleepingdragon.domain.GameResponse
import com.ironmind.sleepingdragon.domain.NarrationPlan
import com.ironmind.sleepingdragon.domain.NarratorRole

class BedModeSession(context: Context) : VoiceEngine.Listener {

    interface Observer {
        fun onUiStateChanged(state: SessionUiState)
    }

    private val appContext = context.applicationContext
    private val saveRepository = GameSaveRepository(appContext)
    val gameEngine = GameEngine(saveRepository)
    private val commandRegistry = CommandRegistry(gameEngine)
    val voiceEngine = VoiceEngine(appContext)

    private var observer: Observer? = null
    private var active = false
    private var resumeOnForeground = false
    private var processingCommand = false
    private var lastHandledInput = ""
    private var lastHandledAt = 0L
    private var awaitingAdvanceNarration: String? = null

    private var uiState = SessionUiState()
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        voiceEngine.choiceHintsProvider = { gameEngine.activeChoiceHints() }
        voiceEngine.setListener(this)
    }

    fun setObserver(observer: Observer?) {
        this.observer = observer
        publishUi()
    }

    fun start() {
        active = true
        processingCommand = false
        lastHandledInput = ""
        lastHandledAt = 0L
        awaitingAdvanceNarration = null
        voiceEngine.setGameMode(gameEngine.isGameMode())
        publish(
            uiState.copy(
                storyText = gameEngine.currentNarration(),
                partialText = "",
                fairyText = "",
                xpLine = buildXpLine()
            )
        )
        narrate(gameEngine.currentNarration())
    }

    fun resetAndStart() {
        voiceEngine.pause()
        gameEngine.reset()
        start()
    }

    fun pause() {
        resumeOnForeground = active
        active = false
        voiceEngine.pause()
        publishPhase(SessionPhase.Paused)
    }

    fun resumeIfNeeded() {
        if (!resumeOnForeground) return
        active = true
        resumeOnForeground = false
        voiceEngine.startListening(allowDuringSpeech = true)
    }

    fun destroy() {
        active = false
        voiceEngine.shutdown()
        observer = null
    }

    private fun narrate(text: String) {
        publish(
            uiState.copy(
                phase = SessionPhase.Narrating,
                storyText = text,
                partialText = ""
            )
        )
        voiceEngine.setGameMode(gameEngine.isGameMode())
        voiceEngine.speak(text, NarratorRole.OLD_MAN) {
            if (!active) return@speak
            val advance = awaitingAdvanceNarration
            if (advance != null) {
                awaitingAdvanceNarration = null
                narrate(advance)
                return@speak
            }
            if (active) voiceEngine.startListening()
        }
    }

    private fun dispatchCommand(rawInput: String) {
        if (processingCommand || !active) return

        val normalized = rawInput.lowercase().trim()
        if (normalized.isBlank()) return

        val now = System.currentTimeMillis()
        if (normalized == lastHandledInput && now - lastHandledAt < AppConstants.COMMAND_DEBOUNCE_MS) {
            return
        }

        processingCommand = true
        lastHandledInput = normalized
        lastHandledAt = now
        publishPhase(SessionPhase.Processing)

        handleGameResponse(commandRegistry.processCommand(rawInput))
    }

    private fun handleGameResponse(response: GameResponse) {
        awaitingAdvanceNarration = response.autoAdvanceNarration
        val segments = NarrationPlan.segmentsForResponse(response)

        publish(
            uiState.copy(
                storyText = response.displayText,
                partialText = "",
                fairyText = response.fairyWhisper.orEmpty(),
                xpLine = buildXpLine()
            )
        )
        voiceEngine.setGameMode(gameEngine.isGameMode())

        if (segments.isEmpty()) {
            finishResponseAndListen()
            return
        }

        voiceEngine.speakSequence(segments) {
            finishResponseAndListen()
        }
    }

    private fun finishResponseAndListen() {
        processingCommand = false
        if (!active) return

        val advance = awaitingAdvanceNarration
        if (advance != null) {
            awaitingAdvanceNarration = null
            publish(uiState.copy(storyText = advance, fairyText = ""))
            mainHandler.postDelayed({
                if (!active) return@postDelayed
                voiceEngine.speak(advance, NarratorRole.OLD_MAN) {
                    processingCommand = false
                    if (active) voiceEngine.startListening()
                }
            }, AppConstants.NARRATOR_SCENE_PAUSE_MS)
        } else {
            voiceEngine.startListening()
        }
    }

    private fun buildXpLine(): String {
        val node = gameEngine.currentNode()
        return "${gameEngine.xp} XP · Kapitel ${node.chapter} · ${gameEngine.playerName}"
    }

    private fun publishPhase(phase: SessionPhase) {
        publish(uiState.copy(phase = phase))
    }

    private fun publish(state: SessionUiState) {
        uiState = state
        publishUi()
    }

    private fun publishUi() {
        observer?.onUiStateChanged(uiState)
    }

    override fun onListeningChanged(activeListening: Boolean) {
        publish(
            uiState.copy(
                phase = if (activeListening) SessionPhase.Listening else uiState.phase,
                isMicActive = activeListening,
                isNarratorSpeaking = false
            )
        )
    }

    override fun onPartialResult(text: String) {
        publish(uiState.copy(partialText = "…$text"))
    }

    override fun onEarlyCommand(text: String) {
        publish(uiState.copy(partialText = "Gehört: $text"))
        dispatchCommand(text)
    }

    override fun onFinalResult(text: String) {
        publish(uiState.copy(partialText = "Gehört: $text"))
        dispatchCommand(text)
    }

    override fun onError(message: String) {
        publish(uiState.copy(phase = SessionPhase.Error(message), partialText = message))
    }

    override fun onSpeakingChanged(activeSpeaking: Boolean) {
        publish(
            uiState.copy(
                phase = if (activeSpeaking) SessionPhase.Narrating else uiState.phase,
                isNarratorSpeaking = activeSpeaking,
                isMicActive = false,
                activeNarrator = if (activeSpeaking) uiState.activeNarrator else null
            )
        )
    }

    override fun onNarratorRoleChanged(role: NarratorRole?) {
        publish(uiState.copy(activeNarrator = role))
    }
}