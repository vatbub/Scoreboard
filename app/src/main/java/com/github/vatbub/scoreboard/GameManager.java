package com.github.vatbub.scoreboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages game states
 */

public class GameManager {
    @NonNull
    private static final Map<Context, GameManager> instances = new HashMap<>();
    private Context callingContext;
    @Nullable
    private Game currentlyActiveGame;

    private GameManager(@NonNull Context callingContext) {
        setCallingContext(callingContext);
    }

    /**
     * Instantiates a new GameManager for the specified context or returns the existing instance if one has already been initialized.
     *
     * @param callingContext The context to get the GameManager for
     * @return The GameManager for the specified context
     */
    @NonNull
    public static GameManager getInstance(@NonNull Context callingContext) {
        synchronized (instances) {
            if (!instances.containsKey(callingContext))
                instances.put(callingContext, new GameManager(callingContext));

            return instances.get(callingContext);
        }
    }

    /**
     * Resets the GameManager instance for the specified context. Calling {@link #getInstance(Context)} will cause a new instance to be crated.
     *
     * @param context The context of the instance to reset
     * @return The id of the currently active game or -1 if {@link #getCurrentlyActiveGame()}{@code == null}
     */
    public static int resetInstance(Context context) {
        synchronized (instances) {
            int res = -1;

            Game currentGame = getInstance(context).getCurrentlyActiveGame();
            if (currentGame != null)
                res = currentGame.getId();

            instances.remove(context);
            return res;
        }
    }

    /**
     * Returns the game that is currently active or {@code null} if no game is active.
     *
     * @return The game that is currently active or {@code null} if no game is active.
     */
    @Nullable
    public Game getCurrentlyActiveGame() {
        return currentlyActiveGame;
    }

    /**
     * Activates the specified game
     *
     * @param currentlyActiveGame The game to activate
     */
    private void setCurrentlyActiveGame(@Nullable Game currentlyActiveGame) {
        this.currentlyActiveGame = currentlyActiveGame;
    }

    /**
     * Returns a list of all currently running games.
     *
     * @return A list of all currently running games.
     */
    public List<Game> listGames() {
        List<Game> res = new ArrayList<>();
        for (int id : getIDs()) {
            res.add(new Game(id));
        }
        return res;
    }

    /**
     * Activates the game with the specified id.
     *
     * @param id The id of the game to activate
     */
    public void activateGame(int id) {
        activateGame(new Game(id));
    }

    /**
     * Activates the specified game
     *
     * @param gameToBeActivated The game to be activated or {@code null} to indicate that no game shall be active.
     */
    public void activateGame(@Nullable Game gameToBeActivated) {
        Game previousGame = getCurrentlyActiveGame();

        if (previousGame != null && previousGame.equals(gameToBeActivated))
            return;

        setCurrentlyActiveGame(gameToBeActivated);
    }

    /**
     * Creates a game with the specified name. The game is saved automatically.
     *
     * @param gameName The name of the game to create. Does not need to be unique.
     * @return The game that was created.
     */
    public Game createGame(@NonNull String gameName) {
        return new Game(gameName);
    }

    /**
     * Deletes the specified game.
     *
     * @param game The game to delete.
     */
    public void deleteGame(@NonNull Game game) {
        if (game.isActive())
            activateGame(null);
        game.delete();
        List<Integer> ids = getIDs();
        ids.remove((Integer) game.getId());
        setIDs(ids);
    }

    @NonNull
    public Context getCallingContext() {
        return callingContext;
    }

    private void setCallingContext(@NonNull Context callingContext) {
        this.callingContext = callingContext;
    }

    private void saveGame(@NonNull Game game) {
        List<Integer> gameIDs = getIDs();
        if (!gameIDs.contains(game.getId())) {
            gameIDs.add(game.getId());
        }

        setIDs(gameIDs);
    }

    /**
     * Returns a list of all game ids that are currently in use.
     *
     * @return a list of all game ids that are currently in use.
     */
    private List<Integer> getIDs() {
        String ids = getPrefs().getString(Keys.GameManagerKeys.IDS_PREF_KEY, "");
        if (ids.isEmpty())
            return new ArrayList<>();
        List<Integer> res = new ArrayList<>();
        for (String id : ids.split(Keys.GameManagerKeys.IDS_DELIMITER))
            res.add(Integer.parseInt(id));

        return res;
    }

    /**
     * Overrides the list of game ids in use. Use with care as deleting values from this list without calling {@link Game#delete()} will cause a memory leak.
     *
     * @param ids The list of ids to save.
     */
    private void setIDs(List<Integer> ids) {
        StringBuilder idsStringBuilder = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            idsStringBuilder.append(ids.get(i));
            if (i != ids.size() - 1)
                idsStringBuilder.append(Keys.GameManagerKeys.IDS_DELIMITER);
        }

        getPrefs().edit().putString(Keys.GameManagerKeys.IDS_PREF_KEY, idsStringBuilder.toString()).apply();
    }

    /**
     * Returns the {@link SharedPreferences} to be used in the GameManager.
     *
     * @return the {@link SharedPreferences} to be used in the GameManager.
     */
    private SharedPreferences getPrefs() {
        return getCallingContext().getSharedPreferences(Keys.GameManagerKeys.SETTINGS_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns the position of the specified game in the list returned by {@link #listGames()}
     *
     * @param game The game to get the position of
     * @return The position of the specified game or -1 if the game was not found.
     */
    public int getPosition(Game game) {
        List<Game> games = listGames();
        for (int i = 0; i < games.size(); i++) {
            if (games.get(i).equals(game))
                return i;
        }
        return -1;
    }

    private int getNextGameId() {
        List<Integer> ids = getIDs();
        if (ids.isEmpty())
            return 1;

        return Collections.max(ids) + 1;
    }

    /**
     * Implement this interface to listen for changes in a game.
     */
    public interface OnRedrawListener {
        /**
         * Called when the game that this listener is attached to was changed.
         *
         * @param changedGame The game that was changed.
         */
        void onChangeApplied(Game changedGame);
    }

    private static class Keys {
        private static class GameManagerKeys {
            static final String SETTINGS_SHARED_PREFERENCES_NAME = "scoreboardSettings";
            static final String IDS_PREF_KEY = "gameIDs";
            static final String IDS_DELIMITER = ";";
        }

        private static class GameKeys {
            static final String GAME_NAME_PREF_KEY = "gameName";
            static final String PLAYER_IDS_PREF_KEY = "playerIDs";
            static final String IDS_DELIMITER = ";";
        }

        private static class PlayerKeys {
            static final String PLAYER_NAME_PREF_KEY = "playerName";
            static final String SCORES_PREF_KEY = "scores";
            static final String SCORES_DELIMITER = ";";
        }
    }

    /**
     * Represents the scores of a particular game.
     */
    public class Game {
        private int id;
        private List<OnRedrawListener> onRedrawListeners;

        /**
         * Creates a new game with the specified name. For internal use only, use {@link #createGame(String)} instead.
         *
         * @param name The name of the game to create
         */
        private Game(String name) {
            this(getNextGameId());
            setName(name);
            saveGame(this);
        }

        /**
         * Retrieves the game with the specified id. Do not use this to create new games, use {@link #createGame(String)} for that.
         *
         * @param id The id of the game to retrieve. No checks are made if that id actually exists.
         */
        public Game(int id) {
            setId(id);
            setOnRedrawListeners(new ArrayList<OnRedrawListener>());
        }

        private String generateGamePrefKey(String prefKey) {
            return getId() + "." + prefKey;
        }

        /**
         * Triggers {@link OnRedrawListener#onChangeApplied(Game)} on all attached listeners
         */
        private void triggerOnRedrawListeners() {
            if (getOnRedrawListeners() == null) return;

            for (OnRedrawListener onRedrawListener : getOnRedrawListeners())
                onRedrawListener.onChangeApplied(this);
        }

        /**
         * The list of {@link Player}s that participate in this game
         *
         * @return The list of {@link Player}s that participate in this game
         */
        public List<Player> getPlayers() {
            List<Player> res = new ArrayList<>();
            for (int id : getPlayerIDs()) {
                res.add(new Player(id));
            }
            return res;
        }

        @NonNull
        public String getName() {
            String res = getPrefs().getString(generateGamePrefKey(Keys.GameKeys.GAME_NAME_PREF_KEY), null);
            assert res != null;
            return res;
        }

        public void setName(@NonNull String name) {
            getPrefs().edit().putString(generateGamePrefKey(Keys.GameKeys.GAME_NAME_PREF_KEY), name).apply();
            triggerOnRedrawListeners();
        }

        private void deleteGameNameSetting() {
            getPrefs().edit().remove(generateGamePrefKey(Keys.GameKeys.GAME_NAME_PREF_KEY)).apply();
        }

        public int getId() {
            return id;
        }

        /**
         * Sets the id of this game. Ids must be unique. For internal use only
         *
         * @param id The id to set
         */
        private void setId(int id) {
            this.id = id;
        }

        /**
         * Deletes this game from the shared preferences. For internal use only.
         *
         * @see #deleteGame(Game)
         */
        private void delete() {
            deleteGameNameSetting();
            for (Player player : getPlayers())
                deletePlayer(player);
            deletePlayerList();
        }

        /**
         * Checks if this game is currently active
         *
         * @return {@code true} if {@link #getCurrentlyActiveGame()}{@code .equals(this) == true}
         */
        public boolean isActive() {
            return getCurrentlyActiveGame() != null && getCurrentlyActiveGame().equals(this);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Game && ((Game) obj).getId() == this.getId();
        }

        @Override
        public String toString() {
            return getName();
        }

        private List<Integer> getPlayerIDs() {
            String ids = getPrefs().getString(generateGamePrefKey(Keys.GameKeys.PLAYER_IDS_PREF_KEY), "");
            if (ids.isEmpty())
                return new ArrayList<>();
            List<Integer> res = new ArrayList<>();
            for (String id : ids.split(Keys.GameKeys.IDS_DELIMITER))
                res.add(Integer.parseInt(id));

            return res;
        }

        private void setPlayerIDs(List<Integer> ids) {
            StringBuilder idsStringBuilder = new StringBuilder();
            for (int i = 0; i < ids.size(); i++) {
                idsStringBuilder.append(ids.get(i));
                if (i != ids.size() - 1)
                    idsStringBuilder.append(Keys.GameKeys.IDS_DELIMITER);
            }

            getPrefs().edit().putString(generateGamePrefKey(Keys.GameKeys.PLAYER_IDS_PREF_KEY), idsStringBuilder.toString()).apply();
        }

        /**
         * Creates a new player in this game
         *
         * @param playerName The name of the player to create
         * @return The created player
         */
        @NonNull
        public Player createPlayer(String playerName) {
            return new Player(playerName);
        }

        private void savePlayer(@NonNull Player player) {
            List<Integer> playerIDs = getPlayerIDs();
            if (!playerIDs.contains(player.getId())) {
                playerIDs.add(player.getId());
                setPlayerIDs(playerIDs);

                triggerOnRedrawListeners();
            }
        }

        /**
         * Deletes the specified player from this game
         *
         * @param playerToDelete The player to delete
         */
        public void deletePlayer(@NonNull Player playerToDelete) {
            playerToDelete.delete();
            List<Integer> playerIDs = getPlayerIDs();
            List<Integer> playerIDsToRemove = new ArrayList<>(1);
            playerIDsToRemove.add(playerToDelete.getId());
            playerIDs.removeAll(playerIDsToRemove);

            setPlayerIDs(playerIDs);
            triggerOnRedrawListeners();
        }

        private void deletePlayerList() {
            getPrefs().edit().remove(generateGamePrefKey(Keys.GameKeys.PLAYER_IDS_PREF_KEY)).apply();
        }

        private int getNextPlayerId() {
            List<Integer> ids = getPlayerIDs();
            if (ids.isEmpty())
                return 1;

            return Collections.max(ids) + 1;
        }

        /**
         * Adds a new line to the score board.
         *
         * @param scores The list of scores to add. This list must contain exactly one score per player and an IllegalStateException is thrown if this is not the case.
         */
        public void addScoreLine(@NonNull List<Integer> scores) {
            assertScoreListLength(scores);
            List<Player> players = getPlayers();
            for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
                List<Integer> playerScores = players.get(playerIndex).getScores();
                playerScores.add(scores.get(playerIndex));
                players.get(playerIndex).setScores(playerScores);
            }

            triggerOnRedrawListeners();
        }

        /**
         * Modifies the specified score line
         *
         * @param index  The index of the score line to modify
         * @param scores The list of scores to be set. This list must contain exactly one score per player and an IllegalStateException is thrown if this is not the case.
         */
        public void modifyScoreLineAt(int index, @NonNull List<Integer> scores) {
            assertScoreListLength(scores);
            List<Player> players = getPlayers();
            for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
                List<Integer> playerScores = players.get(playerIndex).getScores();
                playerScores.set(index, scores.get(playerIndex));
                players.get(playerIndex).setScores(playerScores);
            }

            triggerOnRedrawListeners();
        }

        private void assertScoreListLength(List<Integer> scores) {
            if (scores.size() != getPlayerIDs().size())
                throw new IllegalArgumentException("The size of the submitted score list must match the size of the player list! (Score list size was: " + scores.size() + ", player list size was: " + getPlayerIDs().size() + ")");
        }

        /**
         * Removes the specified line from the scoreboard
         *
         * @param index The index of the row to remove
         */
        public void removeScoreLineAt(int index) {
            List<Player> players = getPlayers();
            for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
                List<Integer> playerScores = players.get(playerIndex).getScores();
                playerScores.remove(index);
                players.get(playerIndex).setScores(playerScores);
            }

            triggerOnRedrawListeners();
        }

        /**
         * Returns the specified score line
         *
         * @param index The row index of the line to return
         * @return The score line at the specified index
         */
        @NonNull
        public List<Integer> getScoreLineAt(int index) {
            List<Player> players = getPlayers();
            List<Integer> res = new ArrayList<>(players.size());
            for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
                List<Integer> playerScores = players.get(playerIndex).getScores();
                res.add(playerScores.get(index));
            }

            return res;
        }

        /**
         * The number of lines that are currently on the scoreboard
         *
         * @return The number of lines that are currently on the scoreboard
         */
        public int getScoreCount() {
            if (getPlayers().isEmpty()) return 0;
            return getPlayers().get(0).getScores().size();
        }

        @Nullable
        public List<OnRedrawListener> getOnRedrawListeners() {
            return onRedrawListeners;
        }

        public void setOnRedrawListeners(@Nullable List<OnRedrawListener> onRedrawListeners) {
            this.onRedrawListeners = onRedrawListeners;
        }

        public class Player {
            private int id;

            /**
             * Creates a new PLayer in this game. For internal use only
             *
             * @param name The name of the player to create
             * @see #createPlayer(String)
             */
            private Player(String name) {
                this(getNextPlayerId());
                setName(name);
                initScores();
                savePlayer(this);
            }

            /**
             * Retrieves the player with the specified id. Do not use this to create new players, use {@link #createPlayer(String)} for that.
             *
             * @param id The id of the player to retrieve. No checks are made if that id actually exists.
             */
            public Player(int id) {
                setId(id);
            }

            private void initScores() {
                List<Integer> scores = getScores();
                while (scores.size() < getScoreCount())
                    scores.add(0);
                setScores(scores);
            }

            private String generatePlayerPrefKey(String prefKey) {
                return generateGamePrefKey(getId() + "." + prefKey);
            }

            public String getName() {
                return getPrefs().getString(generatePlayerPrefKey(Keys.PlayerKeys.PLAYER_NAME_PREF_KEY), null);
            }

            public void setName(String name) {
                getPrefs().edit().putString(generatePlayerPrefKey(Keys.PlayerKeys.PLAYER_NAME_PREF_KEY), name).apply();
                triggerOnRedrawListeners();
            }

            private void deletePlayerNameSetting() {
                getPrefs().edit().remove(generatePlayerPrefKey(Keys.PlayerKeys.PLAYER_NAME_PREF_KEY)).apply();
            }

            @NonNull
            public List<Integer> getScores() {
                String scores = getPrefs().getString(generatePlayerPrefKey(Keys.PlayerKeys.SCORES_PREF_KEY), "");
                if (scores.isEmpty())
                    return new ArrayList<>();
                List<Integer> res = new ArrayList<>();
                for (String score : scores.split(Keys.PlayerKeys.SCORES_DELIMITER))
                    res.add(Integer.parseInt(score));

                return res;
            }

            private void setScores(@NonNull List<Integer> scores) {
                StringBuilder scoresStringBuilder = new StringBuilder();
                for (int i = 0; i < scores.size(); i++) {
                    scoresStringBuilder.append(scores.get(i));
                    if (i != scores.size() - 1)
                        scoresStringBuilder.append(Keys.PlayerKeys.SCORES_DELIMITER);
                }

                getPrefs().edit().putString(generatePlayerPrefKey(Keys.PlayerKeys.SCORES_PREF_KEY), scoresStringBuilder.toString()).apply();
            }

            private void deleteScores() {
                getPrefs().edit().remove(generatePlayerPrefKey(Keys.PlayerKeys.SCORES_PREF_KEY)).apply();
            }

            public int getId() {
                return id;
            }

            private void setId(int id) {
                this.id = id;
            }

            private void delete() {
                deleteScores();
                deletePlayerNameSetting();
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Player && ((Player) obj).getId() == this.getId();
            }

            @Override
            public String toString() {
                return getName();
            }
        }
    }
}
