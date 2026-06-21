package com.ironmind.sleepingdragon

import org.junit.Assert.assertEquals
import org.junit.Test

class DreamXpTest {

    @Test
    fun shortSleep_noXp() {
        val xp = DreamXp.calculate(sleepStartedAtMs = 0L, nowMs = 2 * 3_600_000L)
        assertEquals(0, xp)
    }

    @Test
    fun threeHours_300Xp() {
        val xp = DreamXp.calculate(sleepStartedAtMs = 0L, nowMs = 3 * 3_600_000L + 1)
        assertEquals(300, xp)
    }

    @Test
    fun fiveHours_maxXp() {
        val xp = DreamXp.calculate(sleepStartedAtMs = 0L, nowMs = 6 * 3_600_000L)
        assertEquals(500, xp)
    }
}