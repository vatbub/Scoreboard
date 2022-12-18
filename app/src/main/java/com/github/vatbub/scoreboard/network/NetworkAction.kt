package com.github.vatbub.scoreboard.network

import com.github.vatbub.matchmaking.common.data.GameData
import com.github.vatbub.scoreboard.data.Game
import com.github.vatbub.scoreboard.data.GameMode
import com.github.vatbub.scoreboard.data.getById
import com.github.vatbub.scoreboard.network.NetworkAction.Companion.Constants.GameConstants.gameIdKey
import com.github.vatbub.scoreboard.network.NetworkAction.Companion.Constants.GameConstants.gameModeKey
import com.github.vatbub.scoreboard.network.NetworkAction.Companion.Constants.GameConstants.gameNameKey
import com.github.vatbub.scoreboard.network.NetworkAction.Companion.Constants.GameConstants.scoreArrayKey
import com.github.vatbub.scoreboard.network.NetworkAction.Companion.Constants.GameConstants.scoreLineIndexKey
import com.github.vatbub.scoreboard.network.NetworkAction.Companion.Constants.PlayerConstants.playerIdKey
import com.github.vatbub.scoreboard.network.NetworkAction.Companion.Constants.PlayerConstants.playerNameKey
import com.github.vatbub.scoreboard.network.NetworkAction.Companion.Constants.actionNameKey

private fun <T : Any> GameData.getOptional(key: String, typeClass: Class<T>) =
        this[key, null, typeClass]

private fun <T : Any> GameData.getOrThrow(key: String, typeClass: Class<T>) = this.getOptional(key, typeClass)
        ?: throw IllegalArgumentException("Illegal game data packet received: Does not contain $key")

sealed class NetworkAction(private val actionName: String) {
    companion object {
        object Constants {
            const val actionNameKey = "actionName"

            object PlayerConstants {
                const val playerNameKey = "playerName"
                const val playerIdKey = "playerId"
            }

            object GameConstants {
                const val gameModeKey = "gameMode"
                const val gameNameKey = "gameName"
                const val gameIdKey = "gameId"
                const val scoreArrayKey = "scoreArray"
                const val scoreLineIndexKey = "scoreLineIndex"
            }
        }

        fun fromNetworkGameData(gameData: GameData): NetworkAction {
            return when (val actionName = gameData.getOrThrow(actionNameKey, String::class.java)) {
                "PlayerAdded" -> PlayerAdded.fromGameData(gameData)
                "PlayerRemoved" -> PlayerRemoved.fromGameData(gameData)
                "PlayersCleared" -> PlayersCleared()
                "GameModeChanged" -> GameModeChanged.fromGameData(gameData)
                "GameNameChanged" -> GameNameChanged.fromGameData(gameData)
                "GameDeleted" -> GameDeleted.fromGameData(gameData)
                "ScoreLineAdded" -> ScoreLineAdded.fromGameData(gameData)
                "ScoreLineModified" -> ScoreLineModified.fromGameData(gameData)
                "ScoreLineRemoved" -> ScoreLineRemoved.fromGameData(gameData)
                "PlayerNameChanged" -> PlayerNameChanged.fromGameData(gameData)
                else -> throw IllegalArgumentException("Illegal game data packet received: actionName $actionName is unknown")
            }
        }
    }

    fun toNetworkGameData(connectionId: String): GameData {
        val gameData = GameData(connectionId, mutableMapOf(actionNameKey to actionName))
        addActionSpecificData(gameData)
        return gameData
    }

    abstract fun addActionSpecificData(gameData: GameData)

    abstract fun applyToGame(game: Game)

    class PlayerAdded(private val playerName: String?, private val playerId: Int) : NetworkAction("PlayerAdded") {
        override fun addActionSpecificData(gameData: GameData) {
            if (playerName != null)
                gameData[playerNameKey] = playerName
            gameData[playerIdKey] = playerId
        }

        override fun applyToGame(game: Game) {
            game.createPlayer(playerName, playerId)
        }

        companion object {
            fun fromGameData(gameData: GameData) =
                    PlayerAdded(gameData.getOptional(playerNameKey, String::class.java),
                            gameData.getOrThrow(playerIdKey, Int::class.java))
        }
    }

    class PlayerRemoved(private val playerId: Int) : NetworkAction("PlayerRemoved") {
        override fun addActionSpecificData(gameData: GameData) {
            gameData[playerIdKey] = playerId
        }

        override fun applyToGame(game: Game) {
            game.players.remove(game.players.first { it.id == playerId })
        }

        companion object {
            fun fromGameData(gameData: GameData) =
                    PlayerRemoved(gameData.getOrThrow(playerIdKey, Int::class.java))
        }
    }

    class PlayersCleared : NetworkAction("PlayersCleared") {
        override fun addActionSpecificData(gameData: GameData) {}
        override fun applyToGame(game: Game) {
            game.players.clear()
        }
    }

    class GameModeChanged(private val newMode: GameMode) : NetworkAction("GameModeChanged") {
        override fun addActionSpecificData(gameData: GameData) {
            gameData[gameModeKey] = newMode.toString()
        }

        override fun applyToGame(game: Game) {
            game.mode = newMode
        }

        companion object {
            fun fromGameData(gameData: GameData) =
                    GameModeChanged(GameMode.valueOf(gameData.getOrThrow(gameModeKey, String::class.java)))
        }
    }

    class GameNameChanged(private val newName: String) : NetworkAction("GameNameChanged") {
        override fun addActionSpecificData(gameData: GameData) {
            gameData[gameNameKey] = newName
        }

        override fun applyToGame(game: Game) {
            game.name = newName
        }

        companion object {
            fun fromGameData(gameData: GameData) =
                    GameNameChanged(gameData.getOrThrow(gameNameKey, String::class.java))
        }
    }

    class GameDeleted(private val gameId: Int) : NetworkAction("GameDeleted") {
        override fun addActionSpecificData(gameData: GameData) {
            gameData[gameIdKey] = gameId
        }

        override fun applyToGame(game: Game) {
            game.gameManager?.deleteGame(game)
        }

        companion object {
            fun fromGameData(gameData: GameData) =
                    GameDeleted(gameData.getOrThrow(gameIdKey, Int::class.java))
        }
    }

    class ScoreLineAdded(private val scores: List<Long>) : NetworkAction("ScoreLineAdded") {
        override fun addActionSpecificData(gameData: GameData) {
            gameData[scoreArrayKey] = scores
        }

        override fun applyToGame(game: Game) {
            game.addScoreLine(scores)
        }

        companion object {
            fun fromGameData(gameData: GameData) =
                    ScoreLineAdded(gameData.getOrThrow(scoreArrayKey, listOf<Long>().javaClass))
        }
    }

    class ScoreLineModified(private val index: Int, private val scores: List<Long>) : NetworkAction("ScoreLineModified") {
        override fun addActionSpecificData(gameData: GameData) {
            gameData[scoreArrayKey] = scores
            gameData[scoreLineIndexKey] = index
        }

        override fun applyToGame(game: Game) {
            game.modifyScoreLineAt(index, scores)
        }

        companion object {
            fun fromGameData(gameData: GameData) =
                    ScoreLineModified(gameData.getOrThrow(scoreLineIndexKey, Int::class.java),
                            gameData.getOrThrow(scoreArrayKey, listOf<Long>().javaClass))
        }
    }

    class ScoreLineRemoved(private val index: Int) : NetworkAction("ScoreLineRemoved") {
        override fun addActionSpecificData(gameData: GameData) {
            gameData[scoreLineIndexKey] = index
        }

        override fun applyToGame(game: Game) {
            game.removeScoreLineAt(index)
        }

        companion object {
            fun fromGameData(gameData: GameData) =
                    ScoreLineRemoved(gameData.getOrThrow(scoreLineIndexKey, Int::class.java))
        }
    }

    class PlayerNameChanged(private val playerId: Int, private val newName: String?) : NetworkAction("PlayerNameChanged") {
        override fun addActionSpecificData(gameData: GameData) {
            gameData[playerIdKey] = playerId
            if (newName != null)
                gameData[playerNameKey] = newName
        }

        override fun applyToGame(game: Game) {
            game.players.getById(playerId).name = newName
        }

        companion object {
            fun fromGameData(gameData: GameData) =
                    PlayerNameChanged(gameData.getOrThrow(playerIdKey, Int::class.java),
                            gameData.getOptional(playerNameKey, String::class.java))
        }
    }
}