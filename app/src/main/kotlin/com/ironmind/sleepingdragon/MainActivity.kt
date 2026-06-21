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
import com.ironmind.sleepingdragon.core.BedModeSession
import com.ironmind.sleepingdragon.core.SessionPhase
import com.ironmind.sleepingdragon.core.SessionUiState

class MainActivity : AppCompatActivity(), BedModeSession.Observer {

    private lateinit var session: BedModeSession

    private lateinit var micStatusText: TextView
    private lateinit var partialText: TextView
    private lateinit var storyText: TextView
    private lateinit var xpText: TextView
    private lateinit var fairyText: TextView

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            session.start()
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

        session = BedModeSession(this)
        session.setObserver(this)

        findViewById<Button>(R.id.listenButton).setOnClickListener {
            ensureMicAndStart()
        }
        findViewById<Button>(R.id.resetButton).setOnClickListener {
            session.resetAndStart()
        }

        ensureMicAndStart()
    }

    private fun ensureMicAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> session.start()

            else -> requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onUiStateChanged(state: SessionUiState) {
        storyText.text = state.storyText.ifBlank { getString(R.string.hint) }
        partialText.text = state.partialText
        fairyText.text = state.fairyText
        xpText.text = state.xpLine.ifBlank {
            getString(
                R.string.xp_status,
                session.gameEngine.xp,
                session.gameEngine.currentNode().chapter,
                session.gameEngine.playerName
            )
        }
        renderMicStatus(state)
    }

    private fun renderMicStatus(state: SessionUiState) {
        when {
            state.phase is SessionPhase.Error -> {
                micStatusText.text = getString(R.string.mic_off)
                micStatusText.setTextColor(Color.parseColor("#E05A5A"))
            }
            state.isNarratorSpeaking -> {
                micStatusText.text = getString(R.string.narrator_speaking)
                micStatusText.setTextColor(ContextCompat.getColor(this, R.color.dragon_text))
            }
            state.isMicActive -> {
                micStatusText.text = getString(R.string.mic_active)
                micStatusText.setTextColor(ContextCompat.getColor(this, R.color.dragon_gold))
            }
            state.phase is SessionPhase.Paused -> {
                micStatusText.text = getString(R.string.mic_waiting)
                micStatusText.setTextColor(ContextCompat.getColor(this, R.color.dragon_muted))
            }
            else -> {
                micStatusText.text = getString(R.string.mic_waiting)
                micStatusText.setTextColor(ContextCompat.getColor(this, R.color.dragon_muted))
            }
        }
    }

    override fun onPause() {
        session.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            session.resumeIfNeeded()
        }
    }

    override fun onDestroy() {
        session.destroy()
        super.onDestroy()
    }
}