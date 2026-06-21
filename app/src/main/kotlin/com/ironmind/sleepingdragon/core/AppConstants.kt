package com.ironmind.sleepingdragon.core

object AppConstants {
    const val VERSION_NAME = "4.2.0"
    const val VERSION_CODE = 4200
    const val PREFS_NAME = "sleeping_dragon_v4"

    const val ECHO_GUARD_MS = 320L
    const val SESSION_RENEWAL_MS = 40L
    const val PARTIAL_CONFIRM_MS = 180L
    const val ERROR_RETRY_MS = 400L
    const val COMMAND_DEBOUNCE_MS = 2_000L

    const val MAX_PLAYER_NAME_LENGTH = 24
    const val MAX_EVENT_LOG_ENTRIES = 20
    const val EVENT_LOG_SEPARATOR = "|||"
}