package com.github.vatbub.scoreboard.data

import android.content.Context
import com.github.vatbub.scoreboard.ValueSortedMap
import java.util.*
import kotlin.properties.Delegates

class GameManager(private val callingContext: Context) {
    companion object {
        // private val gson = Gson()
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
                var res = -1

                val currentGame = getInstance(context).currentlyActiveGame
                if (currentGame != null)
                    res = currentGame.id

                instances.remove(context)
                return res
            }
        }
    }

    private val gameManagerPrefs = callingContext.getSharedPreferences("gameManager", Context.MODE_PRIVATE)!!
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

    fun getGameById(id: Int): Game? {
        _games.forEach {
            if (it.id == id)
                return it
        }
        return null
    }

    /**
     * Activates the game with the specified id.
     *
     * @param id The id of the game to activate
     */
    fun activateGame(id: Int) {
        activateGame(getGameById(id))
    }

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
    fun createGame(gameName: String): Game {
        val game = Game(this, nextGameId, gameName, GameMode.HIGH_SCORE, listOf())
        _games.add(game)
        return game
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
        // val json = gson.toJson(game.serializableCopy())
        // gameManagerPrefs.edit().putString(getGameKey(game.id), json).apply()
        KryoUtils.saveObject(callingContext, getGameKey(game.id), game.serializableCopy())
    }

    private fun saveGameList() {
        // val json = gson.toJson(gameIDs)
        // gameManagerPrefs.edit().putString(gameListPrefKey, json).apply()
        KryoUtils.saveObject(callingContext, gameListPrefKey, gameIDs)
    }

    private fun restoreData(): ArrayList<Game> {
        // val json = gameManagerPrefs.getString(gameListPrefKey, "")
        // val restoredGameIds = gson.fromJson<List<Int>>(json, List::class.java) ?: return listOf()
        val restoredGameIds = KryoUtils.readObject(callingContext, gameListPrefKey, List::class.java)
                ?: return arrayListOf()
        val restoredGames = arrayListOf<Game>()

        restoredGameIds.forEach {
            it as Int
            // val gameJson = gameManagerPrefs.getString(getGameKey(it), "")
            // val restoredGame = gson.fromJson<Game>(gameJson, Game::class.java) ?: return@forEach
            val restoredGame = KryoUtils.readObject(callingContext, getGameKey(it), Game::class.java)
                    ?: return@forEach
            restoredGame.gameManager = this

            restoredGame.players.forEach { player ->
                player.parentGame = restoredGame
            }

            restoredGames.add(restoredGame)
        }

        return restoredGames
    }
}

enum class GameMode {
    HIGH_SCORE, LOW_SCORE
}

class Game(var gameManager: GameManager?, val id: Int, name: String?, gameMode: GameMode, players: List<Player>) {
    /**
     * For GSON only. GSON will overwrite all default values.
     */
    @Suppress("unused")
    private constructor() : this(null, -1, null, GameMode.HIGH_SCORE, listOf())

    var name by Delegates.observable(name) { _, _, _ -> gameManager?.saveGame(this) }
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

    internal fun serializableCopy(): Game {
        return Game(null, id, name, mode, List(players.size) { players[it].serializableCopy() })
    }

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
            val gameMode = mode
            val sortedScores = when (gameMode) {
                GameMode.HIGH_SCORE -> ValueSortedMap<Player, Long>(false)
                GameMode.LOW_SCORE -> ValueSortedMap(true)
            }

            players.forEach { sortedScores[it] = it.totalScore }

            return sortedScores
        }

    override fun toString(): String {
        return name ?: super.toString()
    }

    /**
     * Creates a new player in this game
     *
     * @param playerName The name of the player to create
     * @return The created player
     */
    fun createPlayer(playerName: String): Player {
        val player = Player(this, nextPlayerId, playerName, listOf())
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
        players.forEachIndexed { index, player ->
            player.scores.add(scores[index])
        }
    }

    fun addEmptyScoreLine() {
        val players = players
        val scores = ArrayList<Long>(players.size)
        repeat(players.size) { scores.add(0L) }
        addScoreLine(scores)
    }

    /**
     * Modifies the specified score line
     *
     * @param index  The index of the score line to modify
     * @param scores The list of scores to be set. This list must contain exactly one score per player and an IllegalStateException is thrown if this is not the case.
     */
    fun modifyScoreLineAt(index: Int, scores: List<Long>) {
        assertScoreListLength(scores)
        players.forEachIndexed { playerIndex, player ->
            player.scores[index] = scores[playerIndex]
        }
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
    }

    /**
     * Returns the specified score line
     *
     * @param index The row index of the line to return
     * @return The score line at the specified index
     */
    fun getScoreLineAt(index: Int): List<Long> {
        val playersCopy = players
        return List(playersCopy.size) { playerIndex ->
            val playerScores = playersCopy[playerIndex].scores
            playerScores[index]
        }
    }

    internal fun savePlayer() = gameManager?.saveGame(this)
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

    internal fun serializableCopy(): Player {
        return Player(null, id, name, scores)
    }

    val totalScore: Long
        get() = getSubTotalAt(scores.size - 1)

    fun getSubTotalAt(index: Int): Long {
        var sum: Long = 0
        val scores = scores
        for (i in 0..index)
            sum += scores[i]

        return sum
    }

    override fun toString(): String {
        return name ?: super.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Player

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}