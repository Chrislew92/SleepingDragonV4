package com.ironmind.sleepingdragon

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), VoiceEngine.Listener {

    private lateinit var voiceEngine: VoiceEngine
    private lateinit var gameEngine: GameEngine
    private lateinit var commandRegistry: CommandRegistry

    private lateinit var micStatusText: TextView
    private lateinit var partialText: TextView
    private lateinit var storyText: TextView
    private lateinit var xpText: TextView
    private lateinit var fairyText: TextView

    private var sessionActive = false
    private var resumeListeningOnForeground = false
    private var awaitingAdvanceNarration: String? = null
    private var processingCommand = false

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginBedMode()
        } else {
            storyText.text = getString(R.string.mic_denied)
            micStatusText.text = getString(R.string.mic_off)
            micStatusText.setTextColor(Color.parseColor("#E05A5A"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        micStatusText = findViewById(R.id.micStatusText)
        partialText = findViewById(R.id.partialText)
        storyText = findViewById(R.id.storyText)
        xpText = findViewById(R.id.xpText)
        fairyText = findViewById(R.id.fairyText)

        gameEngine = GameEngine(this)
        commandRegistry = CommandRegistry(gameEngine)
        voiceEngine = VoiceEngine(this)
        voiceEngine.choiceHintsProvider = { gameEngine.activeChoiceHints() }
        voiceEngine.setListener(this)

        findViewById<Button>(R.id.listenButton).setOnClickListener {
            ensureMicAndBegin()
        }
        findViewById<Button>(R.id.resetButton).setOnClickListener {
            voiceEngine.pause()
            gameEngine.reset()
            updateXp()
            beginBedMode()
        }

        updateXp()
        ensureMicAndBegin()
    }

    private fun ensureMicAndBegin() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> beginBedMode()

            else -> requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun beginBedMode() {
        sessionActive = true
        processingCommand = false
        partialText.text = ""
        fairyText.text = ""
        storyText.text = gameEngine.currentNarration()
        voiceEngine.setGameMode(gameEngine.isGameMode())
        updateXp()
        narrate(gameEngine.currentNarration())
    }

    private fun narrate(text: String, thenListen: Boolean = true) {
        storyText.text = text
        voiceEngine.setGameMode(gameEngine.isGameMode())
        voiceEngine.speak(text) {
            if (!sessionActive) return@speak
            val advance = awaitingAdvanceNarration
            if (advance != null) {
                awaitingAdvanceNarration = null
                narrate(advance, thenListen)
                return@speak
            }
            if (thenListen) {
                voiceEngine.startListening()
            }
        }
    }

    private fun dispatchCommand(rawInput: String) {
        if (processingCommand || !sessionActive) return
        processingCommand = true

        val response = commandRegistry.processCommand(rawInput)
        handleGameResponse(response)
    }

    private fun handleGameResponse(response: GameResponse) {
        updateXp()
        partialText.text = ""
        storyText.text = response.displayText
        fairyText.text = response.fairyWhisper.orEmpty()
        voiceEngine.setGameMode(gameEngine.isGameMode())

        awaitingAdvanceNarration = response.autoAdvanceNarration
        voiceEngine.speak(buildSpeakQueue(response)) {
            processingCommand = false
            if (!sessionActive) return@speak

            val advance = awaitingAdvanceNarration
            if (advance != null) {
                awaitingAdvanceNarration = null
                storyText.text = advance
                voiceEngine.speak(advance) {
                    processingCommand = false
                    if (sessionActive) voiceEngine.startListening()
                }
            } else if (sessionActive) {
                voiceEngine.startListening()
            }
        }
    }

    private fun buildSpeakQueue(response: GameResponse): String {
        val fairy = response.fairyWhisper
        return if (fairy.isNullOrBlank()) {
            response.speakText
        } else {
            "${response.speakText} $fairy"
        }
    }

    private fun updateXp() {
        val node = gameEngine.currentNode()
        xpText.text = getString(
            R.string.xp_status,
            gameEngine.xp,
            node.chapter,
            gameEngine.playerName
        )
    }

    override fun onListeningChanged(active: Boolean) {
        if (active) {
            micStatusText.text = getString(R.string.mic_active)
            micStatusText.setTextColor(ContextCompat.getColor(this, R.color.dragon_gold))
        } else if (!processingCommand) {
            micStatusText.text = getString(R.string.mic_waiting)
            micStatusText.setTextColor(ContextCompat.getColor(this, R.color.dragon_muted))
        }
    }

    override fun onPartialResult(text: String) {
        partialText.text = getString(R.string.partial_prefix, text)
    }

    override fun onEarlyCommand(text: String) {
        partialText.text = getString(R.string.heard_prefix, text)
        dispatchCommand(text)
    }

    override fun onFinalResult(text: String) {
        partialText.text = getString(R.string.heard_prefix, text)
        dispatchCommand(text)
    }

    override fun onError(message: String) {
        partialText.text = message
    }

    override fun onSpeakingChanged(active: Boolean) {
        if (active) {
            micStatusText.text = getString(R.string.narrator_speaking)
            micStatusText.setTextColor(ContextCompat.getColor(this, R.color.dragon_text))
        }
    }

    override fun onPause() {
        resumeListeningOnForeground = sessionActive
        sessionActive = false
        voiceEngine.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (
            resumeListeningOnForeground &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            sessionActive = true
            voiceEngine.startListening(allowDuringSpeech = true)
        }
    }

    override fun onDestroy() {
        sessionActive = false
        voiceEngine.shutdown()
        super.onDestroy()
    }
}