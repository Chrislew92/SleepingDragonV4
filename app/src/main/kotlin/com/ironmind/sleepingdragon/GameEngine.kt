package com.ironmind.sleepingdragon

import com.ironmind.sleepingdragon.core.AppConstants
import com.ironmind.sleepingdragon.data.GameSave
import com.ironmind.sleepingdragon.data.GameSaveStore
import com.ironmind.sleepingdragon.domain.GameResponse
import com.ironmind.sleepingdragon.domain.VoiceCommands

class GameEngine(private val saveRepository: GameSaveStore) {

    val goodFairy = GoodFairy()

    private var save: GameSave = saveRepository.load()

    val playerName: String get() = save.playerName
    val xp: Int get() = save.xp
    val currentNodeId: String get() = save.nodeId
    val isPaused: Boolean get() = save.isPaused

    fun currentNode(): StoryNode =
        StoryRepository.nodes[save.nodeId] ?: StoryRepository.nodes.getValue("splash")

    fun currentNarration(): String = formatText(currentNode().narratorText)

    fun activeChoiceHints(): List<String> = currentNode().choices.flatMap { it.voiceHints }

    fun isGameMode(): Boolean = save.nodeId != "splash"

    fun processInput(rawInput: String): GameResponse {
        val input = rawInput.lowercase().trim()

        if (save.isPaused) {
            if (matchesAny(input, VoiceCommands.resume)) {
                save = save.copy(isPaused = false)
                persist()
                goodFairy.onCommandSuccess()
                return GameResponse(
                    displayText = "Fortgesetzt.",
                    speakText = "Fortgesetzt. Wir machen weiter.",
                    autoAdvanceNarration = currentNarration()
                )
            }
            return GameResponse(
                displayText = "Pausiert. Sage Weiter zum Fortsetzen.",
                speakText = "Das Spiel ist pausiert. Sage Weiter."
            )
        }

        if (input.isBlank()) return unrecognized("")

        when {
            matchesAny(input, VoiceCommands.pause) -> {
                save = save.copy(isPaused = true)
                persist()
                goodFairy.onCommandSuccess()
                return GameResponse(
                    displayText = "Pausiert.",
                    speakText = "Pausiert. Sage Weiter, wenn du bereit bist."
                )
            }
            matchesAny(input, VoiceCommands.repeat) -> {
                goodFairy.onCommandSuccess()
                val text = currentNarration()
                return GameResponse(displayText = text, speakText = text)
            }
            matchesAny(input, VoiceCommands.summary) -> {
                goodFairy.onCommandSuccess()
                val summary = if (save.eventLog.isEmpty()) {
                    "Noch keine Ereignisse. Sage Weiter."
                } else {
                    "Deine letzten Schritte: " + save.eventLog.takeLast(5).joinToString(". ")
                }
                return GameResponse(displayText = summary, speakText = summary)
            }
            matchesAny(input, VoiceCommands.status) -> {
                goodFairy.onCommandSuccess()
                val node = currentNode()
                val text =
                    "Sleeping Dragon ${AppConstants.VERSION_NAME}. Kapitel ${node.chapter}. " +
                    "XP: ${save.xp}. Name: ${save.playerName}. Weisheit der Fee: ${goodFairy.wisdom()}."
                return GameResponse(displayText = text, speakText = text)
            }
            matchesAny(input, VoiceCommands.help) -> {
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
            save = save.copy(playerName = name)
            persist()
            goodFairy.onCommandSuccess()
            val text = "Verstanden. Ich nenne dich $name."
            logEvent("Name gesetzt: $name")
            return GameResponse(
                displayText = text,
                speakText = text,
                autoAdvanceNarration = currentNarration()
            )
        }

        val choice = findChoice(currentNode(), input)
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

        var nextSave = save.copy(xp = save.xp + choice.xpReward)
        if (choice.nextNodeId == "sleep") {
            nextSave = nextSave.copy(sleepStartedAtMs = System.currentTimeMillis())
        }

        val responseText = formatText(choice.response)
        nextSave = logEventOn(nextSave, responseText)

        choice.nextNodeId?.let { nextId ->
            nextSave = nextSave.copy(nodeId = nextId)
            save = nextSave
            persist()
            return GameResponse(
                displayText = responseText,
                speakText = responseText,
                autoAdvanceNarration = currentNarration()
            )
        }

        save = nextSave
        persist()
        return GameResponse(displayText = responseText, speakText = responseText)
    }

    private fun wakeFromSleep(): GameResponse {
        val dreamXp = if (save.sleepStartedAtMs > 0L) {
            DreamXp.calculate(save.sleepStartedAtMs)
        } else {
            300
        }
        save = save.copy(
            xp = save.xp + dreamXp,
            sleepStartedAtMs = 0L,
            nodeId = "l8_paywall"
        )
        persist()

        val message = DreamXp.morningMessage(dreamXp, save.playerName)
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
            val match = pattern.find(input.trim()) ?: continue
            val name = match.groupValues[1].trim()
            if (name.isNotBlank()) return name.take(AppConstants.MAX_PLAYER_NAME_LENGTH)
        }
        return null
    }

    private fun formatText(template: String): String =
        template.replace("{name}", save.playerName)

    private fun matchesAny(input: String, triggers: List<String>): Boolean =
        triggers.any { input.contains(it) }

    private fun logEvent(text: String) {
        val log = save.eventLog.toMutableList()
        log.add(text.take(120))
        while (log.size > AppConstants.MAX_EVENT_LOG_ENTRIES) {
            log.removeAt(0)
        }
        save = save.copy(eventLog = log)
    }

    private fun logEventOn(base: GameSave, text: String): GameSave {
        val log = base.eventLog.toMutableList()
        log.add(text.take(120))
        while (log.size > AppConstants.MAX_EVENT_LOG_ENTRIES) {
            log.removeAt(0)
        }
        return base.copy(eventLog = log)
    }

    private fun persist() {
        saveRepository.save(save)
    }

    fun reset() {
        save = GameSave()
        goodFairy.onCommandSuccess()
        saveRepository.clear()
        saveRepository.save(save)
    }
}