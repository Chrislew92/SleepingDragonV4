package com.ironmind.sleepingdragon.data

data class GameSave(
    val playerName: String = "Wanderer",
    val xp: Int = 0,
    val nodeId: String = "splash",
    val sleepStartedAtMs: Long = 0L,
    val eventLog: List<String> = emptyList(),
    val isPaused: Boolean = false
)