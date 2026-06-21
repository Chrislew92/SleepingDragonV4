package com.ironmind.sleepingdragon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceMatcherTest {

    @Test
    fun matchCommand_findsWeiter() {
        val match = VoiceMatcher.matchCommand("ja ich sage weiter bitte", listOf("weiter", "start"))
        assertEquals("weiter", match)
    }

    @Test
    fun matchCommand_returnsNullForPhantom() {
        assertNull(VoiceMatcher.matchCommand("ab", emptyList()))
    }

    @Test
    fun isBargeIn_detectsDracheErwache() {
        assertNotNull(VoiceMatcher.matchCommand("drache erwache jetzt", emptyList(), includeBargeIn = true))
    }

    @Test
    fun normalize_stripsPunctuation() {
        assertEquals("weiter bitte", VoiceMatcher.normalize("Weiter, bitte!"))
    }
}