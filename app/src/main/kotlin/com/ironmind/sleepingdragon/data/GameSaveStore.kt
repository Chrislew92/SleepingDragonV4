package com.ironmind.sleepingdragon.data

interface GameSaveStore {
    fun load(): GameSave
    fun save(save: GameSave)
    fun clear()
}