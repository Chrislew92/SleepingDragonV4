package com.ironmind.sleepingdragon.domain

import com.ironmind.sleepingdragon.core.AppConstants

object NarrationPlan {

    fun segmentsForResponse(response: GameResponse): List<SpeakSegment> {
        val segments = mutableListOf<SpeakSegment>()
        val speakText = response.speakText.trim()
        val fairy = response.fairyWhisper?.trim().orEmpty()

        if (speakText.isNotEmpty()) {
            segments += SpeakSegment(
                text = speakText,
                role = NarratorRole.OLD_MAN,
                pauseAfterMs = if (fairy.isNotEmpty()) AppConstants.FAIRY_PRE_PAUSE_MS else null
            )
        }
        if (fairy.isNotEmpty()) {
            segments += SpeakSegment(
                text = fairySpeakText(fairy),
                role = NarratorRole.FAIRY
            )
        }
        return segments
    }

    fun fairySpeakText(fairyWhisper: String): String {
        val trimmed = fairyWhisper.trim()
        val prefixes = listOf(
            "Die Gute Fee flüstert:",
            "Die Gute Fee sagt leise:"
        )
        for (prefix in prefixes) {
            if (trimmed.startsWith(prefix, ignoreCase = true)) {
                return trimmed.substring(prefix.length).trim()
            }
        }
        return trimmed
    }
}