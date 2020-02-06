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
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class GameManagerTest : TestClassWithContext() {
    @Before
    fun beforeEachTest() {
        context.filesDir?.listFiles()?.forEach {
            println("Deleting file: ${it.absolutePath}")
            it.delete()
        }
    }

    @Test
    fun sameInstanceForSameContextTest() {
        val instance1 = GameManager[context]
        val instance2 = GameManager[context]
        assertThat(instance2).isSameInstanceAs(instance1)
    }

    @Test
    fun differentInstanceAfterReset() {
        val instance1 = GameManager[context]
        GameManager.resetInstance(context)
        val instance2 = GameManager[context]
        assertThat(instance2).isNotSameInstanceAs(instance1)
    }

    @Test
    fun emptyGamesListOnInitialization() {
        val gameManager = GameManager[context]
        assertThat(gameManager.games).isEmpty()
    }

    @Test
    fun createGameTest() {
        val gameManager = GameManager[context]
        val newGame = gameManager.createGame("game1")
        assertGameList(gameManager, context, newGame)
    }

    @Test
    fun getGameTest() {
        val gameManager = GameManager[context]
        val newGame = gameManager.createGame("game1")
        val findResult = gameManager[newGame.id]
        assertThat(findResult).isSameInstanceAs(newGame)
    }

    @Test
    fun activateGameById() {
        val gameManager = GameManager[context]
        val newGame = gameManager.createGame("game1")
        assertThat(gameManager.currentlyActiveGame).isNull()
        gameManager.activateGame(newGame.id)
        assertThat(gameManager.currentlyActiveGame).isSameInstanceAs(newGame)
    }

    @Test
    fun activateGameByGameObject() {
        val gameManager = GameManager[context]
        val newGame = gameManager.createGame("game1")
        assertThat(gameManager.currentlyActiveGame).isNull()
        gameManager.activateGame(newGame)
        assertThat(gameManager.currentlyActiveGame).isSameInstanceAs(newGame)
    }

    @Test
    fun createGameIfEmptyAndActivateTheFirstGameNoGameTest() {
        val gameManager = GameManager[context]
        assertThat(gameManager.games).isEmpty()
        val newGame = gameManager.createGameIfEmptyAndActivateTheFirstGame()
        assertGameList(gameManager, context, newGame)
        assertThat(gameManager.currentlyActiveGame).isSameInstanceAs(newGame)
    }

    @Test
    fun createGameIfEmptyAndActivateTheFirstGameWithGameTest() {
        val gameManager = GameManager[context]
        assertThat(gameManager.games).isEmpty()
        val newGame = gameManager.createGame("game1")
        gameManager.createGameIfEmptyAndActivateTheFirstGame()
        assertGameList(gameManager, context, newGame)
        assertThat(gameManager.currentlyActiveGame).isSameInstanceAs(newGame)
    }

    @Test
    fun deleteNonExistentGame() {
        val gameManager = GameManager[context]
        assertThat(gameManager.games).isEmpty()
        val game = Game(gameManager, 1, "game1", GameMode.HIGH_SCORE, listOf())
        assertThat(gameManager.deleteGame(game)).isFalse()
    }

    @Test
    fun deleteInactiveGame() {
        val gameManager = GameManager[context]
        assertThat(gameManager.games).isEmpty()
        val newGame = gameManager.createGame("game1")
        assertThat(gameManager.deleteGame(newGame)).isTrue()
        assertThat(gameManager.games).isEmpty()
    }

    @Test
    fun deleteActiveGame() {
        val gameManager = GameManager[context]
        assertThat(gameManager.games).isEmpty()
        val newGame = gameManager.createGame("game1")
        gameManager.activateGame(newGame)
        assertThat(gameManager.deleteGame(newGame)).isTrue()
        assertThat(gameManager.games).isEmpty()
        assertThat(gameManager.currentlyActiveGame).isNull()
    }

    private fun assertGameList(gameManager: GameManager, context: Context, vararg games: Game) {
        assertThat(gameManager.games).containsExactly(*games)
        // Test data retention
        GameManager.resetInstance(context)
        assertThat(GameManager[context].games).containsExactly(*games)
    }
}