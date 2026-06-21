package com.ironmind.sleepingdragon

object StoryRepository {

    val nodes: Map<String, StoryNode> = mapOf(
        "splash" to StoryNode(
            id = "splash",
            chapter = 0,
            narratorText =
                "Schließe die Augen. Ich bin der alte Mann zwischen deinen Gedanken und der Nacht. " +
                "In der Ferne schläft ein Drache. Sage Drache erwache — oder Start — wenn du bereit bist.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("drache erwache", "start", "weiter", "beginnen"),
                    response = "Der Drache öffnet ein Auge in deiner Vorstellung. Die Geschichte beginnt.",
                    nextNodeId = "l0_prologue"
                )
            )
        ),
        "l0_prologue" to StoryNode(
            id = "l0_prologue",
            chapter = 1,
            narratorText =
                "Kapitel Eins. Seite Eins. In der Ferne schläft ein Drache — seine Träume sind Gold und Gefahr. " +
                "Sage Weiter, wenn du bereit bist, in die Vergessene Zelle zu fallen.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("weiter", "ja", "bereit"),
                    response =
                        "Die erste Seite wendet sich. Kälte steigt aus dem Stein — " +
                        "und dein Name wird zu einem Atemzug in der Dunkelheit.",
                    xpReward = 3,
                    nextNodeId = "l1_cell"
                )
            )
        ),
        "l1_cell" to StoryNode(
            id = "l1_cell",
            chapter = 1,
            narratorText =
                "Du erwachst auf feuchtem Stein. Modrige Kälte kriecht durch die Kleidung. " +
                "Nur meine Stimme: Atme, {name}. Du bist in der Vergessenen Zelle. " +
                "Sage Mein Name ist — oder Weiter.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("mein name ist", "ich heiße", "name ist"),
                    response = "{name}. Der Name hallt von nassen Wänden wider. Irgendwo lauscht etwas.",
                    xpReward = 5,
                    nextNodeId = "l2_corridor"
                ),
                StoryChoice(
                    voiceHints = listOf("weiter"),
                    response = "Du nickst in der Finsternis. Ich nenne dich Wanderer — bis du mir deinen Namen anvertraust.",
                    xpReward = 5,
                    nextNodeId = "l2_corridor"
                )
            )
        ),
        "l2_corridor" to StoryNode(
            id = "l2_corridor",
            chapter = 2,
            narratorText =
                "Der Korridor dehnt sich vor dir. Eine Eichentür mit eisernem Schloss. " +
                "Sage Schloss knacken — oder Ich schleiche leise weiter.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("schloss knacken", "knacke das schloss", "knacken"),
                    response = "Deine Finger finden den Rhythmus des Schlosses. Ein Klick. Noch einer. Die Tür öffnet sich.",
                    xpReward = 14,
                    nextNodeId = "l3_guard"
                ),
                StoryChoice(
                    voiceHints = listOf("schleiche", "leise weiter", "schleichen"),
                    response = "Deine Sohlen küssen den Stein. Der Korridor schluckt jedes Geräusch.",
                    xpReward = 12,
                    nextNodeId = "l3_guard"
                )
            )
        ),
        "l3_guard" to StoryNode(
            id = "l3_guard",
            chapter = 3,
            narratorText =
                "Die Wachkammer riecht nach altem Bier. Ein Wächter schnarcht tief. " +
                "Sage Ich schleiche leise weiter — oder Ich lausche.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("lausche", "ich lausche", "horchen"),
                    response = "Wir atmen zusammen. Du wirst leiser als der Schatten an der Wand.",
                    xpReward = 10,
                    nextNodeId = "l4_runes"
                ),
                StoryChoice(
                    voiceHints = listOf("schleiche", "leise weiter", "schleichen"),
                    response = "Du gleitest am Wächter vorbei. Sein Atem streicht an deiner Schulter.",
                    xpReward = 12,
                    nextNodeId = "l4_runes"
                )
            )
        ),
        "l4_runes" to StoryNode(
            id = "l4_runes",
            chapter = 4,
            narratorText =
                "Ein Runentor versperrt den Gang — warme und kalte Symbole pulsieren im Takt eines fernen Herzens. " +
                "Sage Runen lesen — oder Ich schleiche leise weiter — oder Ich greife an.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("runen lesen", "runen", "lesen"),
                    response = "Die Runen flüstern. Zwei Gefühle, ein Tor. Es öffnet sich mit einem Seufzer.",
                    xpReward = 8,
                    nextNodeId = "l5_dragon_breath"
                ),
                StoryChoice(
                    voiceHints = listOf("schleiche", "leise weiter", "schleichen"),
                    response = "Du gleitest am Runentor vorbei — die Symbole glühen, aber keiner ruft.",
                    xpReward = 14,
                    nextNodeId = "l5_dragon_breath"
                ),
                StoryChoice(
                    voiceHints = listOf("greife an", "angriff", "angreifen"),
                    response = "Deine Klinge trifft Stein. Der Schlag hallt — viel zu laut.",
                    xpReward = 35,
                    nextNodeId = "l5_dragon_breath"
                )
            )
        ),
        "l5_dragon_breath" to StoryNode(
            id = "l5_dragon_breath",
            chapter = 5,
            isCliffhanger = true,
            narratorText =
                "Licht — ein Schacht, zwanzig Schritte entfernt. Dann hörst du es: Atmen. Langsam. Regelmäßig. " +
                "Der Drache wartet. Sage Angriff, Schleichen oder Reden.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("angriff", "greife an", "angreifen"),
                    response = "Deine Klinge trifft Schuppen wie Stein. Er öffnet ein Auge. Die Zeit erstarrt.",
                    xpReward = 20,
                    nextNodeId = "l5_cliffhanger_end"
                ),
                StoryChoice(
                    voiceHints = listOf("schleichen", "schleiche"),
                    response = "Drei Schritte näher. Sein Atem wird wärmer. Die Welt hält den Atem an.",
                    xpReward = 15,
                    nextNodeId = "l5_cliffhanger_end"
                ),
                StoryChoice(
                    voiceHints = listOf("reden", "sprechen"),
                    response = "Wer stört meinen Schlaf? Die Frage hängt in der Luft — ohne Antwort.",
                    xpReward = 12,
                    nextNodeId = "l5_cliffhanger_end"
                )
            )
        ),
        "l5_cliffhanger_end" to StoryNode(
            id = "l5_cliffhanger_end",
            chapter = 5,
            isCliffhanger = true,
            narratorText =
                "Die Klaue hebt sich. Langsam. Unaufhaltsam. Und dann — Schwarz. Nicht Tod. Pause. " +
                "Wirst du den Schacht erreichen, {name}? Sage Weiter.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("weiter"),
                    response = "Der Vorhang fällt. Aber morgen Nacht geht der Film weiter.",
                    xpReward = 5,
                    nextNodeId = "l6_descent"
                )
            )
        ),
        "l6_descent" to StoryNode(
            id = "l6_descent",
            chapter = 6,
            narratorText =
                "Die Klaue erstarrt in der Luft. Der Schacht öffnet sich. Eiserne Sprossen, kalt wie Erinnerungen. " +
                "Du steigst, {name}. Sage Weiter — oder Ich schleiche leise weiter.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("weiter"),
                    response = "Stufe für Stufe. Deine Hände kennen den Rhythmus der Flucht.",
                    xpReward = 10,
                    nextNodeId = "l7_dragon_eye"
                ),
                StoryChoice(
                    voiceHints = listOf("schleiche", "leise weiter", "schleichen"),
                    response = "Leise wie Staub im Schacht. Die Sprossen tragen dich — ohne zu fragen.",
                    xpReward = 14,
                    nextNodeId = "l7_dragon_eye"
                )
            )
        ),
        "l7_dragon_eye" to StoryNode(
            id = "l7_dragon_eye",
            chapter = 7,
            isCliffhanger = true,
            narratorText =
                "Du erreichst die Schwelle. Ein Auge, halb geöffnet, groß wie ein Mond in der Finsternis. " +
                "Der Drache träumt dein Gesicht. Sage Angriff, Schleichen oder Reden.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("angriff", "greife an"),
                    response = "Die Pupille verengt sich. Ein Funke — dann Stille.",
                    xpReward = 22,
                    nextNodeId = "l7_cliffhanger_end"
                ),
                StoryChoice(
                    voiceHints = listOf("schleichen", "schleiche"),
                    response = "Ein Atemzug Abstand zur Ewigkeit. Er blinzelt — und die Welt steht still.",
                    xpReward = 16,
                    nextNodeId = "l7_cliffhanger_end"
                ),
                StoryChoice(
                    voiceHints = listOf("reden", "sprechen"),
                    response = "Du kennst den Weg — aber kennst du das Ende? Schwarz. Pause.",
                    xpReward = 14,
                    nextNodeId = "l7_cliffhanger_end"
                )
            )
        ),
        "l7_cliffhanger_end" to StoryNode(
            id = "l7_cliffhanger_end",
            chapter = 7,
            isCliffhanger = true,
            narratorText =
                "Das Lid fällt — nicht ganz. So weit hast du es geschafft, {name}. Sieben Kapitel — eine verschlossene Tür. " +
                "Flüstere Vorschau — oder sage Weiter zur Paywall.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("weiter"),
                    response = "Der Vorhang fällt. Kapitel acht bis zwölf liegt im Premium-Akt.",
                    xpReward = 10,
                    nextNodeId = "l8_paywall"
                ),
                StoryChoice(
                    voiceHints = listOf("vorschau", "premium"),
                    response = "Sechzig Sekunden Vorgeschmack: Der Drache spricht deinen Namen.",
                    nextNodeId = "l8_paywall"
                )
            )
        ),
        "l8_paywall" to StoryNode(
            id = "l8_paywall",
            chapter = 8,
            isPaywall = true,
            narratorText =
                "Ein Schuldschein aus Stein, {name}. Sieben Kapitel überlebt — der Ausgang wartet hinter der Tür. " +
                "Kapitel acht bis zwölf: der Drache. Der Handel. Dein Ende. " +
                "Zwei Komma neunundneunzig Euro. Sage Vorschau — Gute Nacht — oder Neu starten.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("vorschau", "premium"),
                    response =
                        "Vorgeschmack: Hinter der Tür wartet der Handel mit dem Drachen. " +
                        "Gold gegen Erinnerung. Mut gegen Ende. Premium ab Kapitel acht.",
                    xpReward = 5
                ),
                StoryChoice(
                    voiceHints = listOf("gute nacht", "schlafen", "einschlafen"),
                    response = "Der Drache atmet langsamer. Gute Nacht, {name}.",
                    xpReward = 5,
                    nextNodeId = "sleep"
                ),
                StoryChoice(
                    voiceHints = listOf("neu starten", "von vorne", "restart"),
                    response = "Die Geschichte beginnt von vorn. Schließe die Augen.",
                    nextNodeId = "splash"
                )
            )
        ),
        "sleep" to StoryNode(
            id = "sleep",
            chapter = 7,
            narratorText =
                "Die Nacht hüllt dich ein. Der Drache träumt weiter. " +
                "Sage Drache erwache am Morgen — für Traum-XP.",
            choices = listOf(
                StoryChoice(
                    voiceHints = listOf("drache erwache", "aufwachen", "morgen"),
                    response = "PLACEHOLDER_WAKE",
                    nextNodeId = "l8_paywall"
                )
            )
        )
    )
}