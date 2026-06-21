package com.ironmind.sleepingdragon

import com.ironmind.sleepingdragon.core.AppConstants
import com.ironmind.sleepingdragon.domain.GameResponse
import com.ironmind.sleepingdragon.domain.NarrationPlan
import com.ironmind.sleepingdragon.domain.NarratorRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrationPlanTest {

    @Test
    fun responseWithFairy_buildsTwoSegmentsInOrder() {
        val segments = NarrationPlan.segmentsForResponse(
            GameResponse(
                displayText = "Nicht verstanden.",
                speakText = "Das habe ich nicht verstanden.",
                fairyWhisper = "Die Gute Fee sagt leise: Sage Weiter."
            )
        )

        assertEquals(2, segments.size)
        assertEquals(NarratorRole.OLD_MAN, segments[0].role)
        assertEquals(NarratorRole.FAIRY, segments[1].role)
        assertEquals(AppConstants.FAIRY_PRE_PAUSE_MS, segments[0].pauseAfterMs)
        assertEquals("Sage Weiter.", segments[1].text)
    }

    @Test
    fun responseWithoutFairy_buildsSingleOldManSegment() {
        val segments = NarrationPlan.segmentsForResponse(
            GameResponse(
                displayText = "OK",
                speakText = "Verstanden."
            )
        )

        assertEquals(1, segments.size)
        assertEquals(NarratorRole.OLD_MAN, segments[0].role)
        assertEquals(null, segments[0].pauseAfterMs)
    }

    @Test
    fun fairySpeakText_stripsNarratorPrefix() {
        val spoken = NarrationPlan.fairySpeakText(
            "Die Gute Fee flüstert: Versuche Weiter — ich halte das Mikro für dich bereit."
        )
        assertTrue(spoken.startsWith("Versuche"))
        assertTrue(!spoken.contains("Die Gute Fee"))
    }
}