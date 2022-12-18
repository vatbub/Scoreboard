package com.github.vatbub.scoreboard.network

import com.github.vatbub.matchmaking.common.data.GameData
import com.github.vatbub.scoreboard.data.Game
import com.github.vatbub.scoreboard.data.XmlUtils.serializeToString
import org.jdom2.input.SAXBuilder
import java.io.StringReader

private const val gameDataKey = "gameData"

internal fun Game.toNetworkGameData(connectionId: String) = GameData(connectionId, mutableMapOf(
        gameDataKey to this.toXml().serializeToString()
))

fun Game.updateFromNetworkGameData(gameData: GameData) {
    val xmlString = gameData[gameDataKey, null, String::class.java]
            ?: throw IllegalArgumentException("Received an illegal game data packet")
    StringReader(xmlString).use {
        val document = SAXBuilder().build(it)
        this.updateFromXml(document)
    }
}