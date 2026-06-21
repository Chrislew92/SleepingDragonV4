package com.ironmind.sleepingdragon.data

import android.content.Context
import com.ironmind.sleepingdragon.core.AppConstants

class GameSaveRepository(context: Context) : GameSaveStore {

    private val prefs = context.applicationContext.getSharedPreferences(
        AppConstants.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun load(): GameSave = GameSave(
        playerName = prefs.getString(KEY_PLAYER_NAME, "Wanderer") ?: "Wanderer",
        xp = prefs.getInt(KEY_XP, 0),
        nodeId = prefs.getString(KEY_NODE_ID, "splash") ?: "splash",
        sleepStartedAtMs = prefs.getLong(KEY_SLEEP_STARTED, 0L),
        eventLog = prefs.getString(KEY_EVENT_LOG, "")
            ?.split(AppConstants.EVENT_LOG_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList(),
        isPaused = prefs.getBoolean(KEY_PAUSED, false)
    )

    override fun save(save: GameSave) {
        prefs.edit()
            .putString(KEY_PLAYER_NAME, save.playerName)
            .putInt(KEY_XP, save.xp)
            .putString(KEY_NODE_ID, save.nodeId)
            .putLong(KEY_SLEEP_STARTED, save.sleepStartedAtMs)
            .putString(KEY_EVENT_LOG, save.eventLog.joinToString(AppConstants.EVENT_LOG_SEPARATOR))
            .putBoolean(KEY_PAUSED, save.isPaused)
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_PLAYER_NAME = "player_name"
        private const val KEY_XP = "xp"
        private const val KEY_NODE_ID = "node_id"
        private const val KEY_SLEEP_STARTED = "sleep_started_at"
        private const val KEY_EVENT_LOG = "event_log"
        private const val KEY_PAUSED = "is_paused"
    }
}