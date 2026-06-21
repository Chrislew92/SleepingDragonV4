package com.ironmind.sleepingdragon

import android.content.Context
import android.content.SharedPreferences

data class GameResponse(
    val displayText: String,
    val speakText: String,
    val autoAdvanceNarration: String? = null,
    val fairyWhisper: String? = null
)

class GameEngine(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sleeping_dragon_v4", Context.MODE_PRIVATE)

    val goodFairy = GoodFairy()

    var playerName: String = prefs.getString("player_name", "Wanderer") ?: "Wanderer"
        private set

    var xp: Int = prefs.getInt("xp", 0)
        private set

    var currentNodeId: String = prefs.getString("node_id", "splash") ?: "splash"
        private set

    var isPaused: Boolean = false
        private set

    private var sleepStartedAtMs: Long = prefs.getLong("sleep_started_at", 0L)

    private val eventLog = mutableListOf<String>()

    init {
        eventLog.addAll(
            prefs.getString("event_log", "")?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    fun currentNode(): StoryNode =
        StoryRepository.nodes[currentNodeId] ?: StoryRepository.nodes.getValue("splash")

    fun currentNarration(): String = formatText(currentNode().narratorText)

    fun activeChoiceHints(): List<String> = currentNode().choices.flatMap { it.voiceHints }

    fun isGameMode(): Boolean = currentNodeId != "splash"

    fun processInput(rawInput: String): GameResponse {
        if (isPaused) {
            if (matchesAny(rawInput, listOf("weiter", "fortsetzen", "resume"))) {
                isPaused = false
                goodFairy.onCommandSuccess()
                return GameResponse(
                    displayText = "Fortgesetzt.",
                    speakText = "Wir machen weiter.",
                    autoAdvanceNarration = currentNarration()
                )
            }
            return GameResponse(
                displayText = "Pausiert. Sage Weiter zum Fortsetzen.",
                speakText = "Das Spiel ist pausiert. Sage Weiter."
            )
        }

        val input = rawInput.lowercase().trim()
        if (input.isBlank()) {
            return unrecognized("")
        }

        when {
            matchesAny(input, listOf("pause", "stopp", "halt")) -> {
                isPaused = true
                goodFairy.onCommandSuccess()
                return GameResponse(
                    displayText = "Pausiert.",
                    speakText = "Pausiert. Sage Weiter, wenn du bereit bist."
                )
            }
            matchesAny(input, listOf("nochmal", "wiederholen", "repeat")) -> {
                goodFairy.onCommandSuccess()
                val text = currentNarration()
                return GameResponse(displayText = text, speakText = text)
            }
            matchesAny(input, listOf("zusammenfassen", "recap", "zusammenfassung")) -> {
                goodFairy.onCommandSuccess()
                val summary = if (eventLog.isEmpty()) {
                    "Noch keine Ereignisse. Sage Weiter."
                } else {
                    "Deine letzten Schritte: " + eventLog.takeLast(5).joinToString(". ")
                }
                return GameResponse(displayText = summary, speakText = summary)
            }
            matchesAny(input, listOf("status", "fortschritt", "xp")) -> {
                goodFairy.onCommandSuccess()
                val node = currentNode()
                val text =
                    "Sleeping Dragon V4.1.0. Kapitel ${node.chapter}. " +
                    "XP: $xp. Name: $playerName. Weisheit der Fee: ${goodFairy.wisdom()}."
                return GameResponse(displayText = text, speakText = text)
            }
            matchesAny(input, listOf("hilfe", "befehle", "help")) -> {
                goodFairy.onCommandSuccess()
                val hints = activeChoiceHints().take(2).joinToString(" oder ")
                val text = if (hints.isBlank()) {
                    "Sage Weiter, Pause, Nochmal, Zusammenfassen oder Status."
                } else {
                    "Sage $hints — oder Pause, Status, Nochmal."
                }
                return GameResponse(displayText = text, speakText = text)
            }
        }

        extractPlayerName(rawInput)?.let { name ->
            playerName = name
            persist()
            goodFairy.onCommandSuccess()
            val text = "Verstanden. Ich nenne dich $playerName."
            logEvent("Name gesetzt: $playerName")
            return GameResponse(
                displayText = text,
                speakText = text,
                autoAdvanceNarration = currentNarration()
            )
        }

        val node = currentNode()
        val choice = findChoice(node, input)
        if (choice != null) {
            goodFairy.onCommandSuccess()
            return applyChoice(choice)
        }

        return unrecognized(rawInput)
    }

    private fun unrecognized(rawInput: String): GameResponse {
        val hints = activeChoiceHints()
        val fairy = goodFairy.onCommandFailed(hints)
        val hint = hints.take(2).joinToString(" oder ")
        val text = if (hint.isBlank()) {
            "Befehl nicht erkannt: $rawInput"
        } else {
            "Das habe ich nicht verstanden. Versuche: $hint"
        }
        return GameResponse(
            displayText = text,
            speakText = text,
            fairyWhisper = fairy
        )
    }

    private fun applyChoice(choice: StoryChoice): GameResponse {
        if (choice.response == "PLACEHOLDER_WAKE") {
            return wakeFromSleep()
        }

        if (choice.nextNodeId == "sleep") {
            sleepStartedAtMs = System.currentTimeMillis()
            persist()
        }

        xp += choice.xpReward
        val responseText = formatText(choice.response)
        logEvent(responseText)

        choice.nextNodeId?.let { nextId ->
            currentNodeId = nextId
            persist()
            val nextNarration = currentNarration()
            return GameResponse(
                displayText = responseText,
                speakText = responseText,
                autoAdvanceNarration = nextNarration
            )
        }

        persist()
        return GameResponse(displayText = responseText, speakText = responseText)
    }

    private fun wakeFromSleep(): GameResponse {
        val dreamXp = if (sleepStartedAtMs > 0L) {
            DreamXp.calculate(sleepStartedAtMs)
        } else {
            300
        }
        xp += dreamXp
        sleepStartedAtMs = 0L
        currentNodeId = "l8_paywall"
        persist()

        val message = DreamXp.morningMessage(dreamXp, playerName)
        logEvent(message)
        return GameResponse(
            displayText = message,
            speakText = message,
            autoAdvanceNarration = currentNarration()
        )
    }

    private fun findChoice(node: StoryNode, input: String): StoryChoice? {
        val normalized = VoiceMatcher.normalize(input)
        return node.choices
            .sortedByDescending { it.voiceHints.maxOfOrNull { hint -> hint.length } ?: 0 }
            .find { choice ->
                choice.voiceHints.any { hint ->
                    val h = VoiceMatcher.normalize(hint)
                    normalized.contains(h) || h.contains(normalized)
                }
            }
    }

    private fun extractPlayerName(input: String): String? {
        val patterns = listOf(
            Regex("""mein name ist\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""ich heiße\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""name ist\s+(.+)""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(input.trim())
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.isNotBlank()) return name.take(24)
            }
        }
        return null
    }

    private fun formatText(template: String): String =
        template.replace("{name}", playerName)

    private fun matchesAny(input: String, triggers: List<String>): Boolean =
        triggers.any { input.contains(it) }

    private fun logEvent(text: String) {
        eventLog.add(text.take(120))
        if (eventLog.size > 20) {
            eventLog.removeAt(0)
        }
    }

    fun reset() {
        playerName = "Wanderer"
        xp = 0
        currentNodeId = "splash"
        isPaused = false
        sleepStartedAtMs = 0L
        eventLog.clear()
        persist()
    }

    private fun persist() {
        prefs.edit()
            .putString("player_name", playerName)
            .putInt("xp", xp)
            .putString("node_id", currentNodeId)
            .putLong("sleep_started_at", sleepStartedAtMs)
            .putString("event_log", eventLog.joinToString("|||"))
            .apply()
    }
}