package com.ironmind.sleepingdragon.domain

object VoiceCommands {
    val pause = listOf("pause", "stopp", "halt")
    val resume = listOf("weiter", "fortsetzen", "resume")
    val repeat = listOf("nochmal", "wiederholen", "repeat")
    val summary = listOf("zusammenfassen", "recap", "zusammenfassung")
    val status = listOf("status", "fortschritt", "xp")
    val help = listOf("hilfe", "befehle", "help")
    val wake = listOf("drache erwache", "start", "beginnen")
    val bargeIn = wake + pause + resume + repeat + status + help
}