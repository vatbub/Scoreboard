/*
 * Copyright (c) 2019 Frederik Kammel <vatbub123@googlemail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.vatbub.scoreboard.data

import android.content.Context
import androidx.annotation.StringRes
import com.github.vatbub.scoreboard.R
import com.github.vatbub.scoreboard.network.networkEventHandler
import org.jdom2.Attribute
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Text
import java.util.*
import kotlin.properties.Delegates

class GameManager(internal val callingContext: Context) {
    companion object {
        private const val gameListPrefKey = "games"
        private val instances = HashMap<Context, GameManager>()

        /**
         * Instantiates a new GameManagerOld for the specified context or returns the existing instance if one has already been initialized.
         *
         * @param callingContext The context to get the GameManagerOld for
         * @return The GameManagerOld for the specified context
         */
        @Suppress("DEPRECATION")
        operator fun get(callingContext: Context): GameManager = getInstance(callingContext)

        @Deprecated(message = "Deprecated syntax", replaceWith = ReplaceWith("GameManager[callingContext]"))
        fun getInstance(callingContext: Context) =
                instances.getOrPut(callingContext) { GameManager(callingContext) }

        /**
         * Resets the GameManagerOld instance for the specified context. Calling [.getInstance] will cause a new instance to be crated.
         *
         * @param context The context of the instance to reset
         * @return The id of the currently active game or -1 if [.getCurrentlyActiveGame]`== null`
         */
        fun resetInstance(context: Context): Int {
            synchronized(instances) {
                val res = GameManager[context].currentlyActiveGame?.id ?: -1
                instances.remove(context)
                return res
            }
        }
    }

    private object SharedPrefKeys {
        internal val gameManagerTitle = "GameManager"
        internal val currentlyActiveGameSharedPrefsKey = "currentlyActiveGame.id"
    }

    private val _games = ObservableMutableList(restoreData(),
            { _, _ -> saveGameList() },
            { _, _, _ -> saveGameList() },
            { _, _ -> saveGameList() },
            { saveGameList() })

    private val gameIDs
        get() = List(_games.size) { _games[it].id }
    val games
        get() = _games.toList()

    /**
     * Returns the game that is currently active or `null` if no game is active.
     *
     * @return The game that is currently active or `null` if no game is active.
     */
    var currentlyActiveGame: Game? = null
        private set

    private val nextGameId: Int
        get() {
            val ids = gameIDs
            return if (ids.isEmpty()) 0 else Collections.max(ids) + 1
        }

    operator fun get(id: Int) = _games.find { it.id == id }

    /**
     * Activates the game with the specified id.
     *
     * @param id The id of the game to activate
     */
    fun activateGame(id: Int) = activateGame(this[id])

    /**
     * Activates the specified game
     *
     * @param gameToBeActivated The game to be activated or `null` to indicate that no game shall be active.
     */
    fun activateGame(gameToBeActivated: Game?) {
        currentlyActiveGame = gameToBeActivated
        val editor = callingContext.getSharedPreferences(SharedPrefKeys.gameManagerTitle, Context.MODE_PRIVATE).edit()
        if (gameToBeActivated != null)
            editor.putInt(SharedPrefKeys.currentlyActiveGameSharedPrefsKey, gameToBeActivated.id)
        else
            editor.remove(SharedPrefKeys.currentlyActiveGameSharedPrefsKey)
        editor.apply()
    }

    /**
     * Creates a game with the specified name. The game is saved automatically.
     *
     * @param gameName The name of the game to create. Does not need to be unique.
     * @return The game that was created.
     */
    fun createGame(gameName: String?): Game {
        val game = Game(this, nextGameId, gameName, GameMode.HIGH_SCORE, listOf(), null, null)
        saveGame(game)
        _games.add(game)
        return game
    }

    fun createGameIfEmptyAndActivateTheLastActivatedGame(): Game {
        val lastActivatedId = callingContext.getSharedPreferences(SharedPrefKeys.gameManagerTitle, Context.MODE_PRIVATE).getInt(SharedPrefKeys.currentlyActiveGameSharedPrefsKey, 0)
        val gameToActivate = if (games.isEmpty()) createGame(null) else games[lastActivatedId]
        activateGame(gameToActivate)
        return gameToActivate
    }

    /**
     * Deletes the specified game if it exists.
     *
     * @param game The game to delete.
     * @return `true` if the game was successfully deleted, `false` if the game did not exist.
     */
    fun deleteGame(game: Game): Boolean {
        if (game.isActive)
            activateGame(null)
        if (game.isShared)
            game.networkEventHandler.onGameDeleted(game)
        return _games.remove(game)
    }

    private fun getGameKey(id: Int): String = "game$id"

    internal fun saveGame(game: Game) {
        XmlUtils.saveFile(callingContext, getGameKey(game.id), game.toXml())
    }

    private fun saveGameList() {
        val rootElement = Element(XmlConstants.GameManager.XML_GAME_IDS_TAG_NAME)
        gameIDs.forEach {
            val idElement = Element(XmlConstants.GameManager.XML_GAME_ID_TAG_NAME)
            idElement.setContent(Text(it.toString()))
            rootElement.children.add(idElement)
        }
        XmlUtils.saveFile(callingContext, gameListPrefKey, Document(rootElement))
    }

    private fun restoreData(): List<Game> {
        val restoredGames = mutableListOf<Game>()
        val gameIdsDocument = XmlUtils.readFile(callingContext, gameListPrefKey)
                ?: return listOf()
        gameIdsDocument.rootElement.children.forEach {
            val id = it.content[0].value.toInt()
            val restoredGame = Game.fromXml(this, XmlUtils.readFile(callingContext, getGameKey(id))
                    ?: return@forEach)
            restoredGames.add(restoredGame)
        }
        return restoredGames
    }
}

enum class GameMode(@StringRes val nameResource: Int) {
    HIGH_SCORE(R.string.switch_mode_highscore), LOW_SCORE(R.string.switch_mode_lowscore);

    fun getNameString(context: Context): String = context.getString(nameResource)
}

class Game internal constructor(internal var gameManager: GameManager?, val id: Int, name: String?, gameMode: GameMode, players: List<Player>, sharedGameId: String?, isHostOfSharedGame: Boolean?) {
    /**
     * Used by GSON and fromXml. GSON will overwrite all default values.
     */
    private constructor() : this(null, -1, null, GameMode.HIGH_SCORE, listOf(), null, null)

    var name by Delegates.observable(name ?: "") { _, _, newValue ->
        gameManager?.saveGame(this)
        if (isShared)
            this.networkEventHandler.onGameNameChanged(newValue)
    }
    val nameOrDummyName: String
        get() {
            if (name.replace(" ", "").isNotEmpty()) return name
            val gameManager = this.gameManager ?: return ""
            return gameManager.callingContext.getString(R.string.game_no_name_template, gameManager.games.indexOf(this) + 1)
        }
    var mode by Delegates.observable(gameMode) { _, _, newValue ->
        gameManager?.saveGame(this)
        if (isShared)
            this.networkEventHandler.onGameModeChanged(newValue)
    }
    val isActive: Boolean
        get() = gameManager?.currentlyActiveGame == this
    val players = ObservableMutableList(players,
            { newPlayer, _ ->
                gameManager?.saveGame(this)
                if (isShared)
                    networkEventHandler.onPlayerAdded(newPlayer)
            },
            { oldPlayer, newPlayer, _ ->
                gameManager?.saveGame(this)
                if (isShared) {
                    with(networkEventHandler) {
                        onPlayerRemoved(oldPlayer)
                        onPlayerAdded(newPlayer)
                    }
                }
            },
            { removedPlayer, _ ->
                gameManager?.saveGame(this)
                if (isShared)
                    networkEventHandler.onPlayerRemoved(removedPlayer)
            },
            {
                gameManager?.saveGame(this)
                if (isShared)
                    networkEventHandler.onPlayersCleared()
            })
    val isShared: Boolean
        get() = sharedGameId != null
    var sharedGameId by Delegates.observable(sharedGameId) { _, _, _ -> gameManager?.saveGame(this) }
    var isHostOfSharedGame by Delegates.observable(isHostOfSharedGame) { _, _, _ -> gameManager?.saveGame(this) }

    private val nextPlayerId: Int
        get() {
            val ids = playerIDs
            return if (ids.isEmpty()) 0 else Collections.max(ids) + 1
        }

    private val playerIDs
        get() = players.map { it.id }

    /**
     * The number of lines that are currently on the scoreboard
     *
     * @return The number of lines that are currently on the scoreboard
     */
    val scoreCount: Int
        get() = if (players.isEmpty()) 0 else players[0].scores.size

    val winners: List<Int>
        get() {
            val bestIndices = mutableListOf<Int>()

            var bestScore = when (mode) {
                GameMode.HIGH_SCORE -> java.lang.Long.MIN_VALUE
                GameMode.LOW_SCORE -> java.lang.Long.MAX_VALUE
            }

            players.forEachIndexed { index, player ->
                val score = player.totalScore
                if (score == bestScore) {
                    bestIndices.add(index)
                    return@forEachIndexed
                }

                when (mode) {
                    GameMode.HIGH_SCORE -> if (score > bestScore) {
                        bestIndices.clear()
                        bestScore = score
                        bestIndices.add(index)
                    }
                    GameMode.LOW_SCORE -> if (score < bestScore) {
                        bestIndices.clear()
                        bestScore = score
                        bestIndices.add(index)
                    }
                }
            }

            return bestIndices
        }

    val loosers: List<Int>
        get() {
            val worstIs = mutableListOf<Int>()

            var worstScore = when (mode) {
                GameMode.HIGH_SCORE -> java.lang.Long.MAX_VALUE
                GameMode.LOW_SCORE -> java.lang.Long.MIN_VALUE
            }

            players.forEachIndexed { index, player ->
                val score = player.totalScore
                if (score == worstScore) {
                    worstIs.add(index)
                    return@forEachIndexed
                }

                when (mode) {
                    GameMode.HIGH_SCORE -> if (score < worstScore) {
                        worstIs.clear()
                        worstScore = score
                        worstIs.add(index)
                    }
                    GameMode.LOW_SCORE -> if (score > worstScore) {
                        worstIs.clear()
                        worstScore = score
                        worstIs.add(index)
                    }
                }
            }

            return worstIs
        }

    val ranking: Map<Player, Long>
        get() {
            val sortedScores = when (mode) {
                GameMode.HIGH_SCORE -> ValueSortedMap<Player, Long>(false)
                GameMode.LOW_SCORE -> ValueSortedMap(true)
            }

            players.forEach { sortedScores[it] = it.totalScore }
            return sortedScores
        }

    override fun toString() = nameOrDummyName

    /**
     * Creates a new player in this game
     *
     * @param playerName The name of the player to create
     * @return The created player
     */

    fun createPlayer(playerName: String?) = createPlayer(playerName, nextPlayerId)

    fun createPlayer(playerName: String?, playerId: Int): Player {
        val player = Player(this, playerId, playerName, List(scoreCount) { 0L })
        players.add(player)
        return player
    }

    /**
     * Adds a new line to the score board.
     *
     * @param scores The list of scores to add. This list must contain exactly one score per player and an IllegalStateException is thrown if this is not the case.
     */
    fun addScoreLine(scores: List<Long>) {
        assertScoreListLength(scores)
        players.forEachIndexed { index, player -> player.scores.add(scores[index]) }
        if (isShared)
            networkEventHandler.onScoreLineAdded(scores)
    }

    fun addEmptyScoreLine() =
            addScoreLine(List(players.size) { 0L })

    /**
     * Modifies the specified score line
     *
     * @param index  The index of the score line to modify
     * @param scores The list of scores to be set. This list must contain exactly one score per player and an IllegalStateException is thrown if this is not the case.
     */
    fun modifyScoreLineAt(index: Int, scores: List<Long>) {
        assertScoreListLength(scores)
        players.forEachIndexed { playerIndex, player -> player.scores[index] = scores[playerIndex] }
        if (isShared)
            networkEventHandler.onScoreLineModified(index, scores)
    }

    private fun assertScoreListLength(scores: List<Long>) {
        if (scores.size != players.size)
            throw IllegalArgumentException("The size of the submitted score list must match the size of the player list! (Score list size was: " + scores.size + ", player list size was: " + playerIDs.size + ")")
    }

    /**
     * Removes the specified line from the scoreboard
     *
     * @param index The index of the row to remove
     */
    fun removeScoreLineAt(index: Int) {
        players.forEach { it.scores.removeAt(index) }
        if (isShared)
            networkEventHandler.onScoreLineRemoved(index)
    }

    /**
     * Returns the specified score line
     *
     * @param index The row index of the line to return
     * @return The score line at the specified index
     */
    fun getScoreLineAt(index: Int) = players.map { it.scores[index] }

    internal fun savePlayer() = gameManager?.saveGame(this)

    fun toXml(): Document {
        val rootElement = Element(XmlConstants.Game.XML_GAME_TAG_NAME)
        rootElement.attributes.add(Attribute(XmlConstants.Game.XML_GAME_ID_ATTRIBUTE, id.toString()))
        rootElement.attributes.add(Attribute(XmlConstants.Game.XML_GAME_NAME_ATTRIBUTE, name))
        rootElement.attributes.add(Attribute(XmlConstants.Game.XML_GAME_GAME_MODE_ATTRIBUTE, mode.toString()))
        if (sharedGameId != null)
            rootElement.attributes.add(Attribute(XmlConstants.Game.XML_GAME_SHARED_ID_ATTRIBUTE, sharedGameId))
        if (isHostOfSharedGame != null)
            rootElement.attributes.add(Attribute(XmlConstants.Game.XML_GAME_IS_HOST_OF_SHARED_GAME_ATTRIBUTE, isHostOfSharedGame.toString()))

        val playersElement = Element(XmlConstants.Game.XML_GAME_PLAYERS_TAG_NAME)
        rootElement.children.add(playersElement)

        players.forEach { playersElement.children.add(it.toXml()) }

        return Document(rootElement)
    }

    fun updateFromXml(document: Document) {
        val root = document.rootElement
        this.sharedGameId = root.getAttribute(XmlConstants.Game.XML_GAME_SHARED_ID_ATTRIBUTE)?.value
        this.isHostOfSharedGame = root.getAttribute(XmlConstants.Game.XML_GAME_IS_HOST_OF_SHARED_GAME_ATTRIBUTE)?.booleanValue
        this.name = root.getAttribute(XmlConstants.Game.XML_GAME_NAME_ATTRIBUTE).value
        this.mode = GameMode.valueOf(root.getAttribute(XmlConstants.Game.XML_GAME_GAME_MODE_ATTRIBUTE).value)

        val playersElement = root.getChild(XmlConstants.Game.XML_GAME_PLAYERS_TAG_NAME)
        players.clear()
        players.addAll(playersElement.children.map {
            Player.fromXml(it)
                    .also { player -> player.parentGame = this }
        })
    }

    companion object {
        fun fromXml(gameManager: GameManager?, document: Document): Game {
            with(document.rootElement) {
                val id = getAttribute(XmlConstants.Game.XML_GAME_ID_ATTRIBUTE).intValue
                val name = getAttribute(XmlConstants.Game.XML_GAME_NAME_ATTRIBUTE).value
                val gameMode = GameMode.valueOf(getAttribute(XmlConstants.Game.XML_GAME_GAME_MODE_ATTRIBUTE).value)

                val playersElement = getChild(XmlConstants.Game.XML_GAME_PLAYERS_TAG_NAME)
                val players = List(playersElement.children.size) { Player.fromXml(playersElement.children[it]) }

                val game = Game(gameManager, id, name, gameMode, players, null, null)
                players.forEach { it.parentGame = game }
                return game
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Game

        if (id != other.id) return false

        return true
    }

    override fun hashCode() = id
}

class Player(var parentGame: Game?, val id: Int, name: String?, scores: List<Long>) {
    /**
     * For GSON only, default values are overwritten by gson
     */
    @Suppress("unused")
    private constructor() : this(null, -1, null, listOf())

    var name: String? by Delegates.observable(name) { _, _, newValue ->
        parentGame?.savePlayer()
        if (parentGame?.isShared == true)
            parentGame?.networkEventHandler?.onPlayerNameChanged(id, newValue)
    }
    val scores = ObservableMutableList(scores,
            { _, _ -> parentGame?.savePlayer() },
            { _, _, _ -> parentGame?.savePlayer() },
            { _, _ -> parentGame?.savePlayer() },
            { parentGame?.savePlayer() })

    val totalScore: Long
        get() = getSubTotalAt(scores.size - 1)

    fun getSubTotalAt(index: Int) = scores.take(index + 1).sum()

    fun toXml(): Element {
        val result = Element(XmlConstants.Player.XML_PLAYER_TAG_NAME)
        result.attributes.add(Attribute(XmlConstants.Player.XML_PLAYER_ID_ATTRIBUTE, id.toString()))
        result.attributes.add(Attribute(XmlConstants.Player.XML_PLAYER_NAME_ATTRIBUTE, name))

        val scoresElement = Element(XmlConstants.Player.XML_PLAYER_SCORES_TAG_NAME)
        result.children.add(scoresElement)

        scores.forEach {
            val scoreElement = Element(XmlConstants.Player.XML_PLAYER_SCORE_TAG_NAME)
            scoreElement.setContent(Text(it.toString()))
            scoresElement.children.add(scoreElement)
        }

        return result
    }

    companion object {
        fun fromXml(element: Element): Player {
            val id = element.getAttribute(XmlConstants.Player.XML_PLAYER_ID_ATTRIBUTE).intValue
            val name = element.getAttribute(XmlConstants.Player.XML_PLAYER_NAME_ATTRIBUTE).value

            val scoresElement = element.getChild(XmlConstants.Player.XML_PLAYER_SCORES_TAG_NAME)
            val scores = List(scoresElement.children.size) { scoresElement.children[it].content[0].value.toLong() }

            return Player(null, id, name, scores)
        }
    }

    override fun toString() = name ?: super.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Player

        if (id != other.id) return false

        return true
    }

    override fun hashCode() = id
}