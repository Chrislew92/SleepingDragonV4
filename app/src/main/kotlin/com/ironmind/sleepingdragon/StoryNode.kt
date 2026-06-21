package com.ironmind.sleepingdragon

data class StoryChoice(
    val voiceHints: List<String>,
    val response: String,
    val xpReward: Int = 0,
    val nextNodeId: String? = null
)

data class StoryNode(
    val id: String,
    val chapter: Int,
    val narratorText: String,
    val choices: List<StoryChoice> = emptyList(),
    val isPaywall: Boolean = false,
    val isCliffhanger: Boolean = false
)