package com.ironmind.sleepingdragon.domain

import com.ironmind.sleepingdragon.core.AppConstants

enum class NarratorRole(
    val pitch: Float,
    val speechRate: Float,
    val volume: Float
) {
    OLD_MAN(
        pitch = AppConstants.NARRATOR_PITCH_OLD_MAN,
        speechRate = AppConstants.NARRATOR_RATE_OLD_MAN,
        volume = AppConstants.NARRATOR_VOLUME_OLD_MAN
    ),
    FAIRY(
        pitch = AppConstants.NARRATOR_PITCH_FAIRY,
        speechRate = AppConstants.NARRATOR_RATE_FAIRY,
        volume = AppConstants.NARRATOR_VOLUME_FAIRY
    )
}

data class SpeakSegment(
    val text: String,
    val role: NarratorRole = NarratorRole.OLD_MAN,
    val pauseAfterMs: Long? = null
)