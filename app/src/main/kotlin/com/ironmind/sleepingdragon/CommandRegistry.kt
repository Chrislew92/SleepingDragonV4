package com.ironmind.sleepingdragon

class CommandRegistry(private val gameEngine: GameEngine) {

    fun processCommand(input: String): GameResponse = gameEngine.processInput(input)
}