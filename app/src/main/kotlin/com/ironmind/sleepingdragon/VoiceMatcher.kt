package com.ironmind.sleepingdragon

object VoiceMatcher {

    private val bargeInTriggers = listOf(
        "drache erwache",
        "start",
        "weiter",
        "pause",
        "stopp",
        "halt",
        "status",
        "nochmal",
        "hilfe"
    )

    fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("""[^a-zäöüß0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    fun matchCommand(
        rawInput: String,
        choiceHints: List<String>,
        includeBargeIn: Boolean = true
    ): String? {
        val input = normalize(rawInput)
        if (input.isBlank() || isPhantom(input)) return null

        val candidates = buildList {
            addAll(choiceHints.map { normalize(it) })
            if (includeBargeIn) addAll(bargeInTriggers)
        }.distinct().sortedByDescending { it.length }

        return candidates.firstOrNull { hint ->
            hint.isNotBlank() && (input.contains(hint) || hint.contains(input))
        }
    }

    fun isBargeIn(rawInput: String): Boolean =
        matchCommand(rawInput, emptyList(), includeBargeIn = true) != null

    private fun isPhantom(input: String): Boolean {
        if (input.length <= 2) return true
        if (input.length <= 4 && !input.contains(Regex("""[aeiouyäöü]"""))) return true
        return false
    }
}