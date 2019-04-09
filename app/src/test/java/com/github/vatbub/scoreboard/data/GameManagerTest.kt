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

import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
// @RunWith(AndroidJUnit4::class)
class GameManagerTest {
    @Suppress("DEPRECATION")
    private val context = RuntimeEnvironment.application.applicationContext// ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun beforeEachTest() {
        context.filesDir.listFiles().forEach {
            println("Deleting file: ${it.absolutePath}")
            it.delete()
        }
    }

    @Test
    fun sameInstanceForSameContextTest() {
        val instance1 = GameManager.getInstance(context)
        val instance2 = GameManager.getInstance(context)
        Assert.assertSame(instance1, instance2)
    }

    @Test
    fun differentInstanceAfterReset() {
        val instance1 = GameManager.getInstance(context)
        GameManager.resetInstance(context)
        val instance2 = GameManager.getInstance(context)
        Assert.assertNotSame(instance1, instance2)
    }

    @Test
    fun emptyGamesListOnInitialization() {
        val gameManager = GameManager.getInstance(context)
        assertThat(gameManager.games).isEmpty()
    }

    @Test
    fun createGameTest() {
        val gameManager = GameManager.getInstance(context)
        val newGame = gameManager.createGame("game1")
        assertThat(gameManager.games).containsExactly(newGame)

        GameManager.resetInstance(context)
        val gameManager2 = GameManager.getInstance(context)
        assertThat(gameManager2.games).containsExactly(newGame)
    }
}