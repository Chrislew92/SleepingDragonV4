package com.ironmind.sleepingdragon.domain

data class GameResponse(
    val displayText: String,
    val speakText: String,
    val autoAdvanceNarration: String? = null,
    val fairyWhisper: String? = null
)