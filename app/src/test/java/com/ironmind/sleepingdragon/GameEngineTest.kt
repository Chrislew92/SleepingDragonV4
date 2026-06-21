package com.ironmind.sleepingdragon

import com.ironmind.sleepingdragon.data.GameSave
import com.ironmind.sleepingdragon.data.GameSaveStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var store: FakeStore
    private lateinit var engine: GameEngine

    @Before
    fun setup() {
        store = FakeStore()
        engine = GameEngine(store)
    }

    @Test
    fun wakeCommand_advancesFromSplash() {
        val response = engine.processInput("drache erwache")
        assertTrue(response.speakText.contains("Drache"))
        assertEquals("l0_prologue", engine.currentNodeId)
    }

    @Test
    fun statusCommand_reportsXp() {
        engine.processInput("drache erwache")
        val response = engine.processInput("status")
        assertTrue(response.speakText.contains("XP"))
    }

    @Test
    fun pauseAndResume_works() {
        engine.processInput("drache erwache")
        engine.processInput("pause")
        assertTrue(engine.isPaused)
        val resumed = engine.processInput("weiter")
        assertTrue(!engine.isPaused)
        assertTrue(resumed.displayText.contains("Fortgesetzt"))
        assertTrue(resumed.speakText.contains("Fortgesetzt"))
    }

    @Test
    fun pausedState_blocksStoryChoices() {
        engine.processInput("drache erwache")
        engine.processInput("pause")
        val blocked = engine.processInput("ja")
        assertTrue(engine.isPaused)
        assertEquals("l0_prologue", engine.currentNodeId)
        assertTrue(blocked.speakText.contains("pausiert"))
    }

    @Test
    fun pauseState_persistsAcrossEngineReload() {
        engine.processInput("drache erwache")
        engine.processInput("pause")
        val reloaded = GameEngine(store)
        assertTrue(reloaded.isPaused)
        assertEquals("l0_prologue", reloaded.currentNodeId)
    }

    @Test
    fun repeatedFailures_triggerFairyWhisper() {
        engine.processInput("drache erwache")
        engine.processInput("xyz")
        engine.processInput("abc")
        val third = engine.processInput("qwe")
        assertTrue(third.fairyWhisper?.contains("Gute Fee") == true)
        assertTrue(third.speakText.contains("nicht verstanden"))
    }

    @Test
    fun reset_clearsProgress() {
        engine.processInput("drache erwache")
        engine.reset()
        assertEquals("splash", engine.currentNodeId)
        assertEquals(0, engine.xp)
    }

    private class FakeStore : GameSaveStore {
        private var save = GameSave()

        override fun load(): GameSave = save

        override fun save(save: GameSave) {
            this.save = save
        }

        override fun clear() {
            save = GameSave()
        }
    }
}