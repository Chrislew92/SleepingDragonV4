package com.ironmind.sleepingdragon.core

object AppConstants {
    const val VERSION_NAME = "4.2.1"
    const val VERSION_CODE = 4201
    const val PREFS_NAME = "sleeping_dragon_v4"

    const val ECHO_GUARD_MS = 320L
    const val SESSION_RENEWAL_MS = 40L
    const val PARTIAL_CONFIRM_MS = 180L
    const val ERROR_RETRY_MS = 400L
    const val COMMAND_DEBOUNCE_MS = 2_000L

    const val NARRATOR_BEAT_PAUSE_MS = 280L
    const val FAIRY_PRE_PAUSE_MS = 370L
    const val NARRATOR_SCENE_PAUSE_MS = 780L

    const val NARRATOR_PITCH_OLD_MAN = 0.72f
    const val NARRATOR_RATE_OLD_MAN = 0.88f
    const val NARRATOR_VOLUME_OLD_MAN = 0.82f

    const val NARRATOR_PITCH_FAIRY = 1.18f
    const val NARRATOR_RATE_FAIRY = 0.82f
    const val NARRATOR_VOLUME_FAIRY = 0.68f

    const val MAX_PLAYER_NAME_LENGTH = 24
    const val MAX_EVENT_LOG_ENTRIES = 20
    const val EVENT_LOG_SEPARATOR = "|||"
}