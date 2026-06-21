package com.ironmind.sleepingdragon.core

import com.ironmind.sleepingdragon.domain.NarratorRole

sealed class SessionPhase {
    data object Idle : SessionPhase()
    data object Narrating : SessionPhase()
    data object Listening : SessionPhase()
    data object Processing : SessionPhase()
    data object Paused : SessionPhase()
    data class Error(val message: String) : SessionPhase()
}

data class SessionUiState(
    val phase: SessionPhase = SessionPhase.Idle,
    val storyText: String = "",
    val partialText: String = "",
    val micLabel: String = "",
    val xpLine: String = "",
    val fairyText: String = "",
    val isMicActive: Boolean = false,
    val isNarratorSpeaking: Boolean = false,
    val activeNarrator: NarratorRole? = null
)