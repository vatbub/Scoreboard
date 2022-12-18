package com.github.vatbub.scoreboard.network

import android.content.Context
import androidx.core.content.edit
import com.github.vatbub.matchmaking.common.data.GameData
import com.github.vatbub.matchmaking.common.data.User
import com.github.vatbub.matchmaking.jvmclient.Client
import com.github.vatbub.matchmaking.jvmclient.EndpointConfiguration
import com.github.vatbub.scoreboard.data.Game
import com.github.vatbub.scoreboard.data.GameMode
import com.github.vatbub.scoreboard.data.Player
import com.github.vatbub.scoreboard.util.getRandomHexString
import java.net.URL

private val matchmakingClients = mutableMapOf<Game, Client>()
private val matchmakingGameEventHandlers = mutableMapOf<Game, NetworkManager.GameNetworkEventHandler>()

private object Lock

private const val userNameKey = "networkUserName"
private val serverUrl = URL("https://vatbubmatchmakingstaging.herokuapp.com/")
private val networkConfigurations = listOf(
        EndpointConfiguration.HttpPollingEndpointConfig(serverUrl),
        EndpointConfiguration.WebsocketEndpointConfig(serverUrl))

internal val Game.networkEventHandler: NetworkManager.GameNetworkEventHandler
    get() = matchmakingGameEventHandlers.getOrPut(this) { NetworkManager.GameNetworkEventHandler(this) }

internal val Game.matchmakingClient: Client
    get() = matchmakingClients.getOrPut(this) {
        Client(networkConfigurations,
                this.networkEventHandler::onConnectedUsersChange,
                this.networkEventHandler::onGameStateChange,
                this.networkEventHandler::onGameStarted,
                this.networkEventHandler::onDataToBeSentToHostChange)
    }

private fun getOrCreateUserName(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("NetworkManagerPrefs", Context.MODE_PRIVATE)
    val userName1 = sharedPreferences.getString(userNameKey, null)
    if (userName1 != null) return userName1
    synchronized(Lock) {
        val userName2 = sharedPreferences.getString(userNameKey, null)
        if (userName2 != null) return userName2
        val userName = getRandomHexString()
        sharedPreferences.edit {
            putString(userNameKey, userName)
        }
        return userName
    }
}

fun Game.startSharing(context: Context, onGameIdAvailable: (gameId: String) -> Unit) {
    synchronized(Lock) {
        if (this.isShared)
            throw IllegalStateException("Cannot share game, game is already shared")
        if (this.matchmakingClient.connected) {
            this.createRoom(context, onGameIdAvailable)
            return
        }
        this.matchmakingClient.requestConnectionId { this.createRoom(context, onGameIdAvailable) }
    }
}

private fun Game.createRoom(context: Context, onGameIdAvailable: (gameId: String) -> Unit) {
    this.matchmakingClient.createRoom(getOrCreateUserName(context), maxRoomSize = 1000) { roomId ->
        this.isHostOfSharedGame = true
        this.sharedGameId = roomId
        this.networkEventHandler.sendState()
        onGameIdAvailable(roomId)
    }
}

internal object NetworkManager {
    fun releaseNetworkResources() {
        synchronized(Lock) {
            while (matchmakingClients.isNotEmpty()) {
                matchmakingClients.remove(matchmakingClients.keys.first())?.disconnect()
            }
            while (matchmakingClients.isNotEmpty()) {
                matchmakingGameEventHandlers.remove(matchmakingClients.keys.first())?.cleanup()
            }
        }
    }

    class GameNetworkEventHandler(private val game: Game) {
        private var lastGameState: GameData? = null
        private val matchmakingClient by lazy { game.matchmakingClient }
        private val connectionId: String
            get() = matchmakingClient.connectionId
                    ?: throw IllegalStateException("Cannot send network data: Not yet connected")

        fun cleanup() {
            // TODO
        }

        fun onPlayerAdded(player: Player) = sendStateOrNetworkAction {
            NetworkAction.PlayerAdded(player.name, player.id)
        }

        fun onPlayerRemoved(player: Player) = sendStateOrNetworkAction {
            NetworkAction.PlayerRemoved(player.id)
        }

        fun onPlayersCleared() = sendStateOrNetworkAction {
            NetworkAction.PlayersCleared()
        }

        fun onGameModeChanged(newMode: GameMode) = sendStateOrNetworkAction {
            NetworkAction.GameModeChanged(newMode)
        }

        fun onGameNameChanged(newName: String) = sendStateOrNetworkAction {
            NetworkAction.GameNameChanged(newName)
        }

        fun onGameDeleted(game: Game) = sendStateOrNetworkAction {
            NetworkAction.GameDeleted(game.id)
        }

        fun onScoreLineAdded(scores: List<Long>) = sendStateOrNetworkAction {
            NetworkAction.ScoreLineAdded(scores)
        }

        fun onScoreLineModified(index: Int, scores: List<Long>) = sendStateOrNetworkAction {
            NetworkAction.ScoreLineModified(index, scores)
        }

        fun onScoreLineRemoved(index: Int) = sendStateOrNetworkAction {
            NetworkAction.ScoreLineRemoved(index)
        }

        fun onPlayerNameChanged(playerId: Int, newName: String?) = sendStateOrNetworkAction {
            NetworkAction.PlayerNameChanged(playerId, newName)
        }

        private fun sendStateOrNetworkAction(networkActionGenerator: () -> NetworkAction) {
            if (game.isHostOfSharedGame == true)
                sendState()
            else
                sendNetworkAction(networkActionGenerator())
        }

        internal fun sendState(gameData: GameData = game.toNetworkGameData(connectionId), processedData: List<GameData> = listOf()) {
            if (game.isHostOfSharedGame == true)
                matchmakingClient.updateGameState(gameData, processedData)
        }

        private fun sendNetworkAction(networkAction: NetworkAction) {
            matchmakingClient.sendDataToHost(listOf(networkAction.toNetworkGameData(connectionId)))
        }

        fun onConnectedUsersChange(oldValue: List<User>?, newValue: List<User>) {
            // TODO
        }

        fun onGameStateChange(oldValue: GameData?, newValue: GameData) {
            val amITheHost = game.isHostOfSharedGame ?: return
            if (amITheHost) return
            lastGameState = newValue
            game.updateFromNetworkGameData(newValue)
        }

        fun onGameStarted() {}

        fun onDataToBeSentToHostChange(oldValue: List<GameData>?, newValue: List<GameData>) {
            val amITheHost = game.isHostOfSharedGame ?: return
            // TODO: Optimize for already processed data

            // reset to last transmitted game state
            val lastGameState = this.lastGameState
            if (lastGameState != null)
                game.updateFromNetworkGameData(lastGameState)

            // extrapolate the game state
            newValue
                    .map { NetworkAction.fromNetworkGameData(it) }
                    .forEach { it.applyToGame(game) }

            if (amITheHost) {
                val newGameState = game.toNetworkGameData(connectionId)
                this.lastGameState = newGameState
                sendState(newGameState, newValue)
            }
        }
    }
}