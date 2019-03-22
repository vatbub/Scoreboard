package com.github.vatbub.scoreboard

import android.content.Context
import android.content.SharedPreferences
import java.util.*

/**
 * Manages game states
 */

class GameManager private constructor(callingContext: Context) {
    /**
     * Returns the game that is currently active or `null` if no game is active.
     *
     * @return The game that is currently active or `null` if no game is active.
     */
    var currentlyActiveGame: Game? = null
        private set

    /**
     * Returns a list of all game ids that are currently in use.
     *
     * @return a list of all game ids that are currently in use.
     */
    private var iDs: MutableList<Int>
        get() {
            val ids = prefs.getString(Keys.GameManagerKeys.IDS_PREF_KEY, "")
            if (ids!!.isEmpty())
                return mutableListOf()
            val res = mutableListOf<Int>()
            for (id in ids.split(Keys.GameManagerKeys.IDS_DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                res.add(Integer.parseInt(id))

            return res
        }
        private set(ids) {
            val idsStringBuilder = StringBuilder()
            for (i in ids.indices) {
                idsStringBuilder.append(ids[i])
                if (i != ids.size - 1)
                    idsStringBuilder.append(Keys.GameManagerKeys.IDS_DELIMITER)
            }

            prefs.edit().putString(Keys.GameManagerKeys.IDS_PREF_KEY, idsStringBuilder.toString()).apply()
        }

    /**
     * Returns the [SharedPreferences] to be used in the GameManager.
     *
     * @return the [SharedPreferences] to be used in the GameManager.
     */
    private val prefs = callingContext.getSharedPreferences(Keys.GameManagerKeys.SETTINGS_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)!!

    private val nextGameId: Int
        get() {
            val ids = iDs
            return if (ids.isEmpty()) 0 else Collections.max(ids) + 1
        }

    /**
     * Returns a list of all currently running games.
     *
     * @return A list of all currently running games.
     */
    fun listGames() = List(iDs.size) { index -> Game(iDs[index]) }

    /**
     * Activates the game with the specified id.
     *
     * @param id The id of the game to activate
     */
    fun activateGame(id: Int) {
        activateGame(Game(id))
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
        return Game(gameName)
    }

    /**
     * Deletes the specified game.
     *
     * @param game The game to delete.
     */
    fun deleteGame(game: Game) {
        if (game.isActive)
            activateGame(null)
        game.delete()
        val ids = iDs
        ids.remove(game.id)
        iDs = ids
    }

    private fun saveGame(game: Game) {
        val gameIDs = iDs
        if (!gameIDs.contains(game.id)) {
            gameIDs.add(game.id)
        }

        iDs = gameIDs
    }

    /**
     * Returns the position of the specified game in the list returned by [.listGames]
     *
     * @param gameToCheck The game to get the position of
     * @return The position of the specified game or -1 if the game was not found.
     */
    fun getPosition(gameToCheck: Game): Int {
        val games = listGames()
        games.forEachIndexed { index, game ->
            if (game == gameToCheck)
                return index
        }
        return -1
    }

    enum class GameMode {
        HIGH_SCORE, LOW_SCORE
    }

    /**
     * Implement this interface to listen for changes in a game.
     */
    interface OnRedrawListener {
        /**
         * Called when the game that this listener is attached to was changed.
         *
         * @param changedGame The game that was changed.
         */
        fun onChangeApplied(changedGame: Game)
    }

    private object Keys {
        object GameManagerKeys {
            internal const val SETTINGS_SHARED_PREFERENCES_NAME = "scoreboardSettings"
            internal const val IDS_PREF_KEY = "gameIDs"
            internal const val IDS_DELIMITER = ";"
        }

        object GameKeys {
            internal const val GAME_NAME_PREF_KEY = "gameName"
            internal const val GAME_MODE_PREF_KEY = "gameMode"
            internal const val PLAYER_IDS_PREF_KEY = "playerIDs"
            internal const val IDS_DELIMITER = ";"
        }

        object PlayerKeys {
            internal const val PLAYER_NAME_PREF_KEY = "playerName"
            internal const val SCORES_PREF_KEY = "scores"
            internal const val SCORES_DELIMITER = ";"
        }
    }

    /**
     * Represents the scores of a particular game.
     */
    inner class Game
    /**
     * Retrieves the game with the specified id. Do not use this to create new games, use [.createGame] for that.
     *
     * @param id The id of the game to retrieve. No checks are made if that id actually exists.
     */
    constructor(val id: Int) {
        val onRedrawListeners = mutableListOf<OnRedrawListener>()

        /**
         * The list of [Player]s that participate in this game
         *
         * @return The list of [Player]s that participate in this game
         */
        val players
            get() = List(playerIDs.size) { Player(it) }

        var name: String?
            get() = prefs.getString(generateGamePrefKey(Keys.GameKeys.GAME_NAME_PREF_KEY), null)
            set(name) {
                prefs.edit().putString(generateGamePrefKey(Keys.GameKeys.GAME_NAME_PREF_KEY), name).apply()
                triggerOnRedrawListeners()
            }

        var mode: GameMode
            get() = GameMode.valueOf(prefs.getString(generateGamePrefKey(Keys.GameKeys.GAME_MODE_PREF_KEY), GameMode.HIGH_SCORE.toString()))
            set(mode) {
                prefs.edit().putString(generateGamePrefKey(Keys.GameKeys.GAME_MODE_PREF_KEY), mode.toString()).apply()
                triggerOnRedrawListeners()
            }

        /**
         * Checks if this game is currently active
         *
         * @return `true` if [.getCurrentlyActiveGame]`.equals(this) == true`
         */
        val isActive: Boolean
            get() = currentlyActiveGame == this

        private var playerIDs: List<Int>
            get() {
                val ids = prefs.getString(generateGamePrefKey(Keys.GameKeys.PLAYER_IDS_PREF_KEY), "")!!
                if (ids.isEmpty())
                    return listOf()
                val idsAsStrings = ids.split(Keys.GameKeys.IDS_DELIMITER).dropLastWhile { it.isEmpty() }
                return List(idsAsStrings.size) { Integer.parseInt(idsAsStrings[it]) }
            }
            set(ids) {
                val idsAsString = ids.joinToString(Keys.GameKeys.IDS_DELIMITER)
                prefs.edit().putString(generateGamePrefKey(Keys.GameKeys.PLAYER_IDS_PREF_KEY), idsAsString).apply()
            }

        private val nextPlayerId: Int
            get() {
                val ids = playerIDs
                return if (ids.isEmpty()) 0 else Collections.max(ids) + 1
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
                val players = players
                val bestIndices = mutableListOf<Int>()
                val gameMode = mode

                var bestScore = when (gameMode) {
                    GameManager.GameMode.HIGH_SCORE -> java.lang.Long.MIN_VALUE
                    GameManager.GameMode.LOW_SCORE -> java.lang.Long.MAX_VALUE
                }

                players.forEachIndexed { index, player ->
                    val score = player.totalScore
                    if (score == bestScore) {
                        bestIndices.add(index)
                        return@forEachIndexed
                    }

                    when (gameMode) {
                        GameManager.GameMode.HIGH_SCORE -> if (score > bestScore) {
                            bestIndices.clear()
                            bestScore = score
                            bestIndices.add(index)
                        }
                        GameManager.GameMode.LOW_SCORE -> if (score < bestScore) {
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
                val players = players
                val worstIs = mutableListOf<Int>()
                val gameMode = mode

                var worstScore = when (gameMode) {
                    GameManager.GameMode.HIGH_SCORE -> java.lang.Long.MAX_VALUE
                    GameManager.GameMode.LOW_SCORE -> java.lang.Long.MIN_VALUE
                }

                players.forEachIndexed { index, player ->
                    val score = player.totalScore
                    if (score == worstScore) {
                        worstIs.add(index)
                        return@forEachIndexed
                    }

                    when (gameMode) {
                        GameManager.GameMode.HIGH_SCORE -> if (score < worstScore) {
                            worstIs.clear()
                            worstScore = score
                            worstIs.add(index)
                        }
                        GameManager.GameMode.LOW_SCORE -> if (score > worstScore) {
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
                val players = players

                val gameMode = mode
                val sortedScores = when (gameMode) {
                    GameManager.GameMode.HIGH_SCORE -> ValueSortedMap<Player, Long>(false)
                    GameManager.GameMode.LOW_SCORE -> ValueSortedMap(true)
                }

                players.forEach { sortedScores[it] = it.totalScore }

                return sortedScores
            }

        /**
         * Creates a new game with the specified name. For internal use only, use [.createGame] instead.
         *
         * @param name The name of the game to create
         */
        internal constructor(name: String) : this(nextGameId) {
            this.name = name
            saveGame(this)
        }

        private fun generateGamePrefKey(prefKey: String) = "$id.$prefKey"

        /**
         * Triggers [OnRedrawListener.onChangeApplied] on all attached listeners
         */
        private fun triggerOnRedrawListeners() {
            for (onRedrawListener in onRedrawListeners)
                onRedrawListener.onChangeApplied(this)
        }

        private fun deleteGameNameSetting() {
            prefs.edit().remove(generateGamePrefKey(Keys.GameKeys.GAME_NAME_PREF_KEY)).apply()
        }

        private fun deleteGameModeSetting() {
            prefs.edit().remove(generateGamePrefKey(Keys.GameKeys.GAME_MODE_PREF_KEY)).apply()
        }

        /**
         * Deletes this game from the shared preferences. For internal use only.
         *
         * @see .deleteGame
         */
        internal fun delete() {
            deleteGameNameSetting()
            deleteGameModeSetting()
            for (player in players)
                deletePlayer(player)
            deletePlayerList()
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
            return Player(playerName)
        }

        private fun savePlayer(player: Player) {
            val playerIDsCopy = playerIDs.toMutableList()
            if (!playerIDsCopy.contains(player.id)) {
                playerIDsCopy.add(player.id)
                playerIDs = playerIDsCopy

                triggerOnRedrawListeners()
            }
        }

        /**
         * Deletes the specified player from this game
         *
         * @param playerToDelete The player to delete
         */
        fun deletePlayer(playerToDelete: Player) {
            playerToDelete.delete()
            val playerIDsCopy = playerIDs.toMutableList()
            val playerIDsToRemove = ArrayList<Int>(1)
            playerIDsToRemove.add(playerToDelete.id)
            playerIDsCopy.removeAll(playerIDsToRemove)

            playerIDs = playerIDsCopy
            triggerOnRedrawListeners()
        }

        private fun deletePlayerList() {
            prefs.edit().remove(generateGamePrefKey(Keys.GameKeys.PLAYER_IDS_PREF_KEY)).apply()
        }

        /**
         * Adds a new line to the score board.
         *
         * @param scores The list of scores to add. This list must contain exactly one score per player and an IllegalStateException is thrown if this is not the case.
         */
        fun addScoreLine(scores: List<Long>) {
            assertScoreListLength(scores)
            players.forEachIndexed { index, player ->
                val playerScores = player.scores.toMutableList()
                playerScores.add(scores[index])
                player.scores = playerScores
            }

            triggerOnRedrawListeners()
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
                val playerScores = player.scores.toMutableList()
                playerScores[index] = scores[playerIndex]
                player.scores = playerScores
            }

            triggerOnRedrawListeners()
        }

        private fun assertScoreListLength(scores: List<Long>) {
            if (scores.size != playerIDs.size)
                throw IllegalArgumentException("The size of the submitted score list must match the size of the player list! (Score list size was: " + scores.size + ", player list size was: " + playerIDs.size + ")")
        }

        /**
         * Removes the specified line from the scoreboard
         *
         * @param index The index of the row to remove
         */
        fun removeScoreLineAt(index: Int) {
            players.forEach {
                val playerScores = it.scores.toMutableList()
                playerScores.removeAt(index)
                it.scores = playerScores
            }

            triggerOnRedrawListeners()
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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Game

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id
        }

        inner class Player
        /**
         * Retrieves the player with the specified id. Do not use this to create new players, use [.createPlayer] for that.
         *
         * @param id The id of the player to retrieve. No checks are made if that id actually exists.
         */
        (val id: Int) {
            var name: String?
                get() = prefs.getString(generatePlayerPrefKey(Keys.PlayerKeys.PLAYER_NAME_PREF_KEY), null)
                set(name) {
                    prefs.edit().putString(generatePlayerPrefKey(Keys.PlayerKeys.PLAYER_NAME_PREF_KEY), name).apply()
                    triggerOnRedrawListeners()
                }

            var scores: List<Long>
                get() {
                    val scores = prefs.getString(generatePlayerPrefKey(Keys.PlayerKeys.SCORES_PREF_KEY), "")
                    if (scores!!.isEmpty())
                        return ArrayList()
                    val scoresAsString = scores.split(Keys.PlayerKeys.SCORES_DELIMITER).dropLastWhile { it.isEmpty() }
                    return List(scoresAsString.size) { index -> scoresAsString[index].toLong() }
                }
                internal set(scores) {
                    val scoresString = scores.joinToString(Keys.PlayerKeys.SCORES_DELIMITER)
                    prefs.edit().putString(generatePlayerPrefKey(Keys.PlayerKeys.SCORES_PREF_KEY), scoresString).apply()
                }

            val totalScore: Long
                get() = getSubTotalAt(scoreCount - 1)

            /**
             * Creates a new Player in this game. For internal use only
             *
             * @param name The name of the player to create
             * @see .createPlayer
             */
            internal constructor(name: String) : this(nextPlayerId) {
                this.name = name
                initScores()
                savePlayer(this)
            }

            private fun initScores() {
                val scoresCopy = scores.toMutableList()
                repeat(scoreCount) { scoresCopy.add(0L) }
                scores = scoresCopy
            }

            private fun generatePlayerPrefKey(prefKey: String) = generateGamePrefKey("$id.$prefKey")

            private fun deletePlayerNameSetting() {
                prefs.edit().remove(generatePlayerPrefKey(Keys.PlayerKeys.PLAYER_NAME_PREF_KEY)).apply()
            }

            private fun deleteScores() {
                prefs.edit().remove(generatePlayerPrefKey(Keys.PlayerKeys.SCORES_PREF_KEY)).apply()
            }

            internal fun delete() {
                deleteScores()
                deletePlayerNameSetting()
            }

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
    }

    companion object {
        private val instances = HashMap<Context, GameManager>()

        /**
         * Instantiates a new GameManager for the specified context or returns the existing instance if one has already been initialized.
         *
         * @param callingContext The context to get the GameManager for
         * @return The GameManager for the specified context
         */
        fun getInstance(callingContext: Context): GameManager {
            synchronized(instances) {
                if (!instances.containsKey(callingContext))
                    instances[callingContext] = GameManager(callingContext)

                return instances[callingContext]!!
            }
        }

        /**
         * Resets the GameManager instance for the specified context. Calling [.getInstance] will cause a new instance to be crated.
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
}
