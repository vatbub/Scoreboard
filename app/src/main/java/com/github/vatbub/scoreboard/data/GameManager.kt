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
import android.support.annotation.StringRes
import com.github.vatbub.scoreboard.R
import com.github.vatbub.scoreboard.util.transform
import org.jdom2.Attribute
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Text
import java.util.*
import kotlin.properties.Delegates

class GameManager(private val callingContext: Context) {
    companion object {
        private const val gameListPrefKey = "games"
        private val instances = HashMap<Context, GameManager>()

        /**
         * Instantiates a new GameManagerOld for the specified context or returns the existing instance if one has already been initialized.
         *
         * @param callingContext The context to get the GameManagerOld for
         * @return The GameManagerOld for the specified context
         */
        fun getInstance(callingContext: Context): GameManager {
            synchronized(instances) {
                if (!instances.containsKey(callingContext))
                    instances[callingContext] = GameManager(callingContext)

                return instances[callingContext]!!
            }
        }

        /**
         * Resets the GameManagerOld instance for the specified context. Calling [.getInstance] will cause a new instance to be crated.
         *
         * @param context The context of the instance to reset
         * @return The id of the currently active game or -1 if [.getCurrentlyActiveGame]`== null`
         */
        fun resetInstance(context: Context): Int {
            synchronized(instances) {
                val res = getInstance(context).currentlyActiveGame?.id ?: -1
                instances.remove(context)
                return res
            }
        }
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
        val previousGame = currentlyActiveGame
        if (previousGame != null && previousGame == gameToBeActivated)
            return
        currentlyActiveGame = gameToBeActivated
    }

    /**
     * Creates a game with the specified name. The game is saved automatically.
     *
     * @param gameName The name of the game to create. Does not need to be unique.
     * @return The game that was created.
     */
    fun createGame(gameName: String?): Game {
        val game = Game(this, nextGameId, gameName, GameMode.HIGH_SCORE, listOf())
        saveGame(game)
        _games.add(game)
        return game
    }

    fun createGameIfEmptyAndActivateTheFirstGame() {
        val gameToActivate = if (games.isEmpty()) createGame(null) else games[0]
        activateGame(gameToActivate)
    }

    /**
     * Deletes the specified game.
     *
     * @param game The game to delete.
     */
    fun deleteGame(game: Game) {
        if (game.isActive)
            activateGame(null)
        _games.remove(game)
    }

    private fun getGameKey(id: Int): String = "game$id"

    internal fun saveGame(game: Game) {
        XmlFileUtils.saveFile(callingContext, getGameKey(game.id), game.toXml())
    }

    private fun saveGameList() {
        val rootElement = Element(XmlConstants.GameManager.XML_GAME_IDS_TAG_NAME)
        gameIDs.forEach {
            val idElement = Element(XmlConstants.GameManager.XML_GAME_ID_TAG_NAME)
            idElement.setContent(Text(it.toString()))
            rootElement.children.add(idElement)
        }
        XmlFileUtils.saveFile(callingContext, gameListPrefKey, Document(rootElement))
    }

    private fun restoreData(): List<Game> {
        val restoredGames = mutableListOf<Game>()
        val gameIdsDocument = XmlFileUtils.readFile(callingContext, gameListPrefKey)
                ?: return listOf()
        gameIdsDocument.rootElement.children.forEach {
            val id = it.content[0].value.toInt()
            val restoredGame = Game.fromXml(this, XmlFileUtils.readFile(callingContext, getGameKey(id))
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

class Game internal constructor(private var gameManager: GameManager?, val id: Int, name: String?, gameMode: GameMode, players: List<Player>) {
    /**
     * For GSON only. GSON will overwrite all default values.
     */
    @Suppress("unused")
    private constructor() : this(null, -1, null, GameMode.HIGH_SCORE, listOf())

    var name by Delegates.observable(name ?: "") { _, _, _ -> gameManager?.saveGame(this) }
    var mode by Delegates.observable(gameMode) { _, _, _ -> gameManager?.saveGame(this) }
    val isActive: Boolean
        get() = gameManager?.currentlyActiveGame == this
    val players = ObservableMutableList(players,
            { _, _ -> gameManager?.saveGame(this) },
            { _, _, _ -> gameManager?.saveGame(this) },
            { _, _ -> gameManager?.saveGame(this) },
            { gameManager?.saveGame(this) })

    private val nextPlayerId: Int
        get() {
            val ids = playerIDs
            return if (ids.isEmpty()) 0 else Collections.max(ids) + 1
        }

    private val playerIDs
        get() = List(players.size) { players[it].id }

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

    override fun toString() = name

    /**
     * Creates a new player in this game
     *
     * @param playerName The name of the player to create
     * @return The created player
     */
    fun createPlayer(playerName: String): Player {
        val player = Player(this, nextPlayerId, playerName, List(scoreCount) { 0L })
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
    fun removeScoreLineAt(index: Int) =
            players.forEach { it.scores.removeAt(index) }

    /**
     * Returns the specified score line
     *
     * @param index The row index of the line to return
     * @return The score line at the specified index
     */
    fun getScoreLineAt(index: Int) = players.transform { it.scores[index] }

    internal fun savePlayer() = gameManager?.saveGame(this)

    fun toXml(): Document {
        val rootElement = Element(XmlConstants.Game.XML_GAME_TAG_NAME)
        rootElement.attributes.add(Attribute(XmlConstants.Game.XML_GAME_ID_ATTRIBUTE, id.toString()))
        rootElement.attributes.add(Attribute(XmlConstants.Game.XML_GAME_NAME_ATTRIBUTE, name))
        rootElement.attributes.add(Attribute(XmlConstants.Game.XML_GAME_GAME_MODE_ATTRIBUTE, mode.toString()))

        val playersElement = Element(XmlConstants.Game.XML_GAME_PLAYERS_TAG_NAME)
        rootElement.children.add(playersElement)

        players.forEach { playersElement.children.add(it.toXml()) }

        return Document(rootElement)
    }

    companion object {
        fun fromXml(gameManager: GameManager?, document: Document): Game {
            with(document.rootElement) {
                val id = getAttribute(XmlConstants.Game.XML_GAME_ID_ATTRIBUTE).intValue
                val name = getAttribute(XmlConstants.Game.XML_GAME_NAME_ATTRIBUTE).value
                val gameMode = GameMode.valueOf(getAttribute(XmlConstants.Game.XML_GAME_GAME_MODE_ATTRIBUTE).value)

                val playersElement = getChild(XmlConstants.Game.XML_GAME_PLAYERS_TAG_NAME)
                val players = List(playersElement.children.size) { Player.fromXml(playersElement.children[it]) }

                val game = Game(gameManager, id, name, gameMode, players)
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

    var name: String? by Delegates.observable(name) { _, _, _ -> parentGame?.savePlayer() }
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