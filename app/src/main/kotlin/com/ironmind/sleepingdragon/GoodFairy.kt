package com.ironmind.sleepingdragon

class GoodFairy {

    private var failedAttempts = 0
    private var wisdomLevel = 1

    fun onCommandFailed(choiceHints: List<String>): String? {
        failedAttempts++
        if (failedAttempts < 3) return null

        failedAttempts = 0
        wisdomLevel = (wisdomLevel + 1).coerceAtMost(10)

        val hint = choiceHints.firstOrNull() ?: "Weiter"
        return when {
            wisdomLevel >= 6 ->
                "Die Gute Fee flüstert: Versuche $hint — ich halte das Mikro für dich bereit."
            else ->
                "Die Gute Fee sagt leise: Sage $hint."
        }
    }

    fun onCommandSuccess() {
        failedAttempts = 0
    }

    fun wisdom(): Int = wisdomLevel
}