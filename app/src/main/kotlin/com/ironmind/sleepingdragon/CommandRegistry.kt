package com.ironmind.sleepingdragon

import com.ironmind.sleepingdragon.domain.GameResponse

class CommandRegistry(private val gameEngine: GameEngine) {
    fun processCommand(input: String): GameResponse = gameEngine.processInput(input)
}