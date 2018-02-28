package com.github.vatbub.scoreboard;

import android.content.Context;
import android.content.SharedPreferences;
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
    private static final Map<Context, GameManager> instances = new HashMap<>();
    private Context callingContext;
    private Game currentlyActiveGame;

    private GameManager(Context callingContext) {
        setCallingContext(callingContext);
    }

    public static GameManager getInstance(Context callingContext) {
        synchronized (instances) {
            if (!instances.containsKey(callingContext))
                instances.put(callingContext, new GameManager(callingContext));

            return instances.get(callingContext);
        }
    }

    public static int resetInstance(Context callingActivity) {
        synchronized (instances) {
            int res = -1;

            Game currentGame = getInstance(callingActivity).getCurrentlyActiveGame();
            if (currentGame != null)
                res = currentGame.getId();

            instances.remove(callingActivity);
            return res;
        }
    }

    @Nullable
    public Game getCurrentlyActiveGame() {
        return currentlyActiveGame;
    }

    private void setCurrentlyActiveGame(Game currentlyActiveGame) {
        this.currentlyActiveGame = currentlyActiveGame;
    }

    public List<Game> listGames() {
        List<Game> res = new ArrayList<>();
        for (int id : getIDs()) {
            res.add(new Game(id));
        }
        return res;
    }

    public void activateGame(int id) {
        activateGame(new Game(id));
    }

    public void activateGame(@Nullable Game gameToBeActivated) {
        Game previousGame = getCurrentlyActiveGame();

        if (previousGame != null && previousGame.equals(gameToBeActivated))
            return;

        setCurrentlyActiveGame(gameToBeActivated);
    }

    public Game createGame(String gameName) {
        return new Game(gameName);
    }

    public void deleteGame(Game game) {
        if (game.isActive())
            activateGame(null);
        game.delete();
        List<Integer> ids = getIDs();
        ids.remove((Integer) game.getId());
        setIDs(ids);
    }

    public Context getCallingContext() {
        return callingContext;
    }

    private void setCallingContext(Context callingContext) {
        this.callingContext = callingContext;
    }

    private void saveGame(Game game) {
        List<Integer> gameIDs = getIDs();
        if (!gameIDs.contains(game.getId())) {
            gameIDs.add(game.getId());
        }

        setIDs(gameIDs);
    }

    private List<Integer> getIDs() {
        String ids = getPrefs().getString(Keys.GameManagerKeys.IDS_PREF_KEY, "");
        if (ids.isEmpty())
            return new ArrayList<>();
        List<Integer> res = new ArrayList<>();
        for (String id : ids.split(Keys.GameManagerKeys.IDS_DELIMITER))
            res.add(Integer.parseInt(id));

        return res;
    }

    private void setIDs(List<Integer> ids) {
        StringBuilder idsStringBuilder = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            idsStringBuilder.append(ids.get(i));
            if (i != ids.size() - 1)
                idsStringBuilder.append(Keys.GameManagerKeys.IDS_DELIMITER);
        }

        getPrefs().edit().putString(Keys.GameManagerKeys.IDS_PREF_KEY, idsStringBuilder.toString()).apply();
    }

    private SharedPreferences getPrefs() {
        return getCallingContext().getSharedPreferences(Keys.GameManagerKeys.SETTINGS_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

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

    public interface OnRedrawListener {
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

    public class Game {
        private int id;
        private List<OnRedrawListener> onRedrawListeners;

        private Game(String name) {
            this(getNextGameId());
            setName(name);
            saveGame(this);
        }

        public Game(int id) {
            setId(id);
            setOnRedrawListeners(new ArrayList<OnRedrawListener>());
        }

        private String generateGamePrefKey(String prefKey) {
            return getId() + "." + prefKey;
        }

        private void triggerOnRedrawListeners() {
            if (getOnRedrawListeners() == null) return;

            for (OnRedrawListener onRedrawListener : getOnRedrawListeners())
                onRedrawListener.onChangeApplied(this);
        }

        public List<Player> getPlayers() {
            List<Player> res = new ArrayList<>();
            for (int id : getPlayerIDs()) {
                res.add(new Player(id));
            }
            return res;
        }

        public String getName() {
            return getPrefs().getString(generateGamePrefKey(Keys.GameKeys.GAME_NAME_PREF_KEY), null);
        }

        public void setName(String name) {
            getPrefs().edit().putString(generateGamePrefKey(Keys.GameKeys.GAME_NAME_PREF_KEY), name).apply();
            triggerOnRedrawListeners();
        }

        private void deleteGameNameSetting() {
            getPrefs().edit().remove(generateGamePrefKey(Keys.GameKeys.GAME_NAME_PREF_KEY)).apply();
        }

        public int getId() {
            return id;
        }

        private void setId(int id) {
            this.id = id;
        }

        /**
         * Deletes this game from the shared preferences
         */
        private void delete() {
            deleteGameNameSetting();
            for (Player player : getPlayers())
                deletePlayer(player);
            deletePlayerList();
        }

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

        public Player createPlayer(String playerName) {
            return new Player(playerName);
        }

        private void savePlayer(Player player) {
            List<Integer> playerIDs = getPlayerIDs();
            if (!playerIDs.contains(player.getId())) {
                playerIDs.add(player.getId());
                setPlayerIDs(playerIDs);

                triggerOnRedrawListeners();
            }
        }

        public void deletePlayer(Player playerToDelete) {
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

        public void addScoreLine(List<Integer> scores) {
            assertScoreListLength(scores);
            List<Player> players = getPlayers();
            for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
                List<Integer> playerScores = players.get(playerIndex).getScores();
                playerScores.add(scores.get(playerIndex));
                players.get(playerIndex).setScores(playerScores);
            }

            triggerOnRedrawListeners();
        }

        public void modifyScoreLineAt(int index, List<Integer> scores) {
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

        public void removeScoreLineAt(int index) {
            List<Player> players = getPlayers();
            for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
                List<Integer> playerScores = players.get(playerIndex).getScores();
                playerScores.remove(index);
                players.get(playerIndex).setScores(playerScores);
            }

            triggerOnRedrawListeners();
        }

        public List<Integer> getScoreLineAt(int index) {
            List<Player> players = getPlayers();
            List<Integer> res = new ArrayList<>(players.size());
            for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
                List<Integer> playerScores = players.get(playerIndex).getScores();
                res.add(playerScores.get(index));
            }

            return res;
        }

        public int getScoreCount() {
            if (getPlayers().isEmpty()) return 0;
            return getPlayers().get(0).getScores().size();
        }

        public List<OnRedrawListener> getOnRedrawListeners() {
            return onRedrawListeners;
        }

        public void setOnRedrawListeners(List<OnRedrawListener> onRedrawListeners) {
            this.onRedrawListeners = onRedrawListeners;
        }

        public class Player {
            private int id;

            private Player(String name) {
                this(getNextPlayerId());
                setName(name);
                savePlayer(this);
            }

            public Player(int id) {
                setId(id);
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

            public List<Integer> getScores() {
                String scores = getPrefs().getString(generatePlayerPrefKey(Keys.PlayerKeys.SCORES_PREF_KEY), "");
                if (scores.isEmpty())
                    return new ArrayList<>();
                List<Integer> res = new ArrayList<>();
                for (String score : scores.split(Keys.PlayerKeys.SCORES_DELIMITER))
                    res.add(Integer.parseInt(score));

                return res;
            }

            private void setScores(List<Integer> scores) {
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
