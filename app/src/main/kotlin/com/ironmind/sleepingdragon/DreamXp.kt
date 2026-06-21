package com.ironmind.sleepingdragon

object DreamXp {

    fun calculate(sleepStartedAtMs: Long, nowMs: Long = System.currentTimeMillis()): Int {
        val hours = (nowMs - sleepStartedAtMs) / 3_600_000.0
        return when {
            hours < 3.0 -> 0
            hours < 4.0 -> 300
            hours < 5.0 -> 400
            else -> 500
        }
    }

    fun morningMessage(xp: Int, name: String): String =
        if (xp <= 0) {
            "Guten Morgen, $name. Die Nacht war kurz — kein Traum-XP heute."
        } else {
            "Guten Morgen, $name. Der Drache schenkt dir $xp Traum-XP für die Nacht."
        }
}