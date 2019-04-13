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

package com.github.vatbub.scoreboard.view

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.github.vatbub.common.core.Common
import com.github.vatbub.scoreboard.AppConfig
import com.github.vatbub.scoreboard.BuildConfig
import com.github.vatbub.scoreboard.R
import com.github.vatbub.scoreboard.data.Game
import com.github.vatbub.scoreboard.data.GameManager
import com.github.vatbub.scoreboard.data.GameMode
import com.github.vatbub.scoreboard.data.Player
import com.github.vatbub.scoreboard.util.ViewUtil
import com.github.vatbub.scoreboard.util.toPx
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import net.steamcrafted.materialiconlib.MaterialMenuInflater

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val showSubTotalsDefaultValue = false
    private val showSubTotalsKey = "showSubTotals"
    private val sharedPreferencesName = "MainActivityPrefs"
    private fun getSharedPreferences() = getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)

    private fun saveShowSubTotalsSetting(showSubTotals: Boolean) {
        getSharedPreferences().edit().putBoolean("showSubTotalsKey", showSubTotals).apply()
    }

    private fun getShowSubTotalsSetting(): Boolean = getSharedPreferences().getBoolean(showSubTotalsKey, showSubTotalsDefaultValue)

    private var backingMainTableAdapter: GameTableRecyclerViewAdapter? = null
    private val mainTableAdapter: GameTableRecyclerViewAdapter
        get() {
            if (backingMainTableAdapter == null)
                backingMainTableAdapter = GameTableRecyclerViewAdapter(main_table_recycler_view, GameManager.getInstance(this).currentlyActiveGame!!, this, getShowSubTotalsSetting()) {
                    saveShowSubTotalsSetting(it)
                    updateShowSubTotalsMenuItem(it)
                }
            return backingMainTableAdapter!!
        }

    private var backingHeaderRowViewHolder: GameTableViewHolder? = null
    private val headerRowViewHolder: GameTableViewHolder
        get() {
            if (backingHeaderRowViewHolder == null)
                backingHeaderRowViewHolder = GameTableViewHolder(header_row, false)
            return backingHeaderRowViewHolder!!
        }

    private var backingSumRowViewHolder: GameTableViewHolderWithPlaceholders? = null
    private val sumRowViewHolder: GameTableViewHolderWithPlaceholders
        get() {
            if (backingSumRowViewHolder == null)
                backingSumRowViewHolder = GameTableViewHolderWithPlaceholders(sum_row)
            return backingSumRowViewHolder!!
        }

    private var backingBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>? = null
    private val mBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
        get() {
            if (backingBottomSheetBehavior == null)
                backingBottomSheetBehavior = BottomSheetBehavior.from(sum_bottom_sheet)
            return backingBottomSheetBehavior!!
        }

    private var headerRowEditTextViews: List<EditText>? = null

    private var optionsMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCommonLibUp()
        AppConfig.initialize(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val gameManager = GameManager.getInstance(this)
        if (gameManager.games.isEmpty())
            gameManager.createGame(null)

        gameManager.activateGame(gameManager.games[0])
        setSumBottomSheetUp()
        setRecyclerViewUp()

        redraw()
        headerRowViewHolder.lineNumberTextView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> mainTableAdapter.updateColumnWidths(headerRowViewHolder.lineNumberTextView.width) }

        setFabListenerUp()
        setNavigationDrawerUp()
        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun setCommonLibUp() {
        Common.useAndroidImplementation(this)
        Common.getInstance().appName = BuildConfig.APPLICATION_ID
    }

    private fun setFabListenerUp() {
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val game = GameManager.getInstance(this@MainActivity).currentlyActiveGame
            if (game == null) {
                Toast.makeText(this, R.string.no_game_active_toast, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            game.addEmptyScoreLine()
            updateLineNumberWidth()
            mainTableAdapter.notifyItemInserted(game.scoreCount)
        }

    }

    private fun setNavigationDrawerUp() {
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setRecyclerViewUp() {
        main_table_recycler_view.layoutManager = LinearLayoutManager(this)
        main_table_recycler_view.adapter = mainTableAdapter
        mainTableAdapter.notifyDataSetChanged()
    }

    private fun setSumBottomSheetUp() {
        ViewUtil.runJustBeforeBeingDrawn(sum_row) { mBottomSheetBehavior.peekHeight = it.height }
        mBottomSheetBehavior.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val optionsMenuCopy = optionsMenu ?: return
                val menuItem = optionsMenuCopy.findItem(R.id.action_toggle_ranking) ?: return
                setMenuItemTitleFromBoolean(menuItem,
                        newState == BottomSheetBehavior.STATE_COLLAPSED,
                        getString(R.string.action_show_ranking),
                        getString(R.string.action_hide_ranking))
            }
        })
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun updateShowSubTotalsMenuItem(subTotalsShown: Boolean) {
        val optionsMenuCopy = optionsMenu ?: return
        val menuItem = optionsMenuCopy.findItem(R.id.action_show_sub_totals) ?: return
        setMenuItemTitleFromBoolean(menuItem,
                subTotalsShown,
                getString(R.string.action_hide_sub_total),
                getString(R.string.action_show_sub_total))
    }

    private fun setMenuItemTitleFromBoolean(menuItem: MenuItem, value: Boolean, titleIfValueTrue: String, titleIfValueFalse: String) {
        menuItem.title = if (value) titleIfValueTrue else titleIfValueFalse
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            menuItem.contentDescription = menuItem.title
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START))
            drawer_layout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.optionsMenu = menu
        MaterialMenuInflater.with(this)
                .setDefaultColor(Color.WHITE)
                .inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_player -> {
                addPlayerHandler()
                return true
            }
            R.id.action_remove_player -> {
                removePlayerHandler()
                return true
            }
            R.id.action_toggle_ranking -> {
                toggleRankingHandler()
                return true
            }
            R.id.action_switch_mode -> {
                switchModeHandler()
                return true
            }
            R.id.action_save_game -> {
                saveGameHandler()
                return true
            }
            R.id.action_new_game -> {
                newGameHandler()
                return true
            }
            R.id.action_load_game -> {
                loadGameHandler()
                return true
            }
            R.id.action_manage_saved_games -> {
                manageSavedGamesHandler()
                return true
            }
            R.id.action_show_sub_totals -> {
                mainTableAdapter.showSubTotal = !mainTableAdapter.showSubTotal
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun newGameHandler() {
        val gameManager = GameManager.getInstance(this)
        val currentGame = gameManager.currentlyActiveGame

        if (currentGame == null) {
            newGameHandlerCreateNewGame(gameManager)
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.alert_new_game_delete_current_game)

            builder.setPositiveButton(R.string.yes) { _, _ ->
                gameManager.deleteGame(currentGame)
                newGameHandlerCreateNewGame(gameManager)
            }
            builder.setNegativeButton(R.string.no) { _, _ ->
                Snackbar.make(root_node, R.string.snackbar_new_game_manage_games_hint, Snackbar.LENGTH_LONG).show()
                newGameHandlerCreateNewGame(gameManager)
            }
            builder.create().show()
        }
    }

    private fun newGameHandlerCreateNewGame(gameManager: GameManager) {
        val newGame = gameManager.createGame(null)
        gameManager.activateGame(newGame)
        redraw(true)
    }

    private fun toggleRankingHandler() {
        if (mBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        else
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun manageSavedGamesHandler() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun loadGameHandler() {
        val gameManager = GameManager.getInstance(this)
        val games = gameManager.games
        val gameNames = List(games.size) { getGameNameOrDummy(games[it], games) }
        val currentGame = gameManager.currentlyActiveGame
        val inputSelection = if (currentGame == null)
            0
        else
            games.indexOf(currentGame)

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.switch_mode_title)

        builder.setSingleChoiceItems(gameNames.toTypedArray(), inputSelection
        ) { dialogInterface, selectedItem ->
            gameManager.activateGame(games[selectedItem])
            dialogInterface.dismiss()
            redraw(true)
        }
        builder.create().show()
    }

    private fun saveGameHandler() {
        val game = GameManager.getInstance(this@MainActivity).currentlyActiveGame ?: return

        createStringPrompt(this, R.string.save_game_dialog_title, R.string.dialog_ok, R.string.dialog_cancel, defaultText = getGameNameOrDummy(game), defaultHint = getString(R.string.save_game_dialog_hint), resultHandler = object : StringPromptResultHandler {
            override fun onOk(result: String) {
                game.name = result
            }

            override fun onCancel() {}
        })
    }

    private fun switchModeHandler() {
        val currentGame = GameManager.getInstance(this@MainActivity).currentlyActiveGame
        val inputSelection = if (currentGame == null) {
            0
        } else when (currentGame.mode) {
            GameMode.HIGH_SCORE -> 0
            GameMode.LOW_SCORE -> 1
        }

        val items = arrayOf<CharSequence>(getString(R.string.switch_mode_highscore), getString(R.string.switch_mode_lowscore))

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.switch_mode_title)

        builder.setSingleChoiceItems(items, inputSelection
        ) { dialogInterface, selectedItem ->
            if (currentGame == null)
                return@setSingleChoiceItems

            if (selectedItem == 0)
                currentGame.mode = GameMode.HIGH_SCORE
            else if (selectedItem == 1)
                currentGame.mode = GameMode.LOW_SCORE
            dialogInterface.dismiss()
            renderSumRow()
            renderLeaderboard()
        }
        builder.create().show()
    }

    private fun verifyPlayer(player: Player, players: List<Player>) {
        if (!players.contains(player))
            throw IllegalArgumentException("Player must be part of the specified game.")
    }

    private fun getPlayerNameOrDummy(game: Game, player: Player, players: List<Player> = game.players): String {
        verifyPlayer(player, players)
        var finalName = player.name
        if (finalName == null || finalName.replace(" ", "") == "")
            finalName = getPlayerDummyName(game, player, players)
        return finalName
    }

    private fun getPlayerDummyName(game: Game, player: Player, players: List<Player> = game.players): String {
        verifyPlayer(player, players)
        val index = players.indexOf(player) + 1
        return getString(R.string.player_no_name_template, index)
    }

    private fun getGameNameOrDummy(game: Game, games: List<Game> = GameManager.getInstance(this).games): String {
        var finalName = game.name
        if (finalName.replace(" ", "") == "")
            finalName = getGameDummyName(game, games)
        return finalName
    }

    private fun getGameDummyName(game: Game, games: List<Game> = GameManager.getInstance(this).games): String {
        val index = games.indexOf(game) + 1
        return getString(R.string.game_no_name_template, index)
    }

    private fun removePlayerHandler() {
        val currentGame = GameManager.getInstance(this@MainActivity).currentlyActiveGame ?: return
        val players = currentGame.players
        val playerNames = Array(players.size) { getPlayerNameOrDummy(currentGame, players[it], players) }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delete_player_title)

        val indicesToDelete = BooleanArray(playerNames.size)

        builder.setMultiChoiceItems(playerNames, indicesToDelete
        ) { _: DialogInterface, index: Int, checked: Boolean ->
            indicesToDelete[index] = checked
        }
        builder.setPositiveButton(R.string.delete_player_ok) { _, _ ->
            val playersToDelete = mutableListOf<Player>()
            indicesToDelete.forEachIndexed { index, checked ->
                if (checked)
                    playersToDelete.add(players[index])
            }
            playersToDelete.forEach { currentGame.players.remove(it) }
            redraw()
        }
        builder.setNegativeButton(R.string.delete_player_cancel) { _, _ -> }
        builder.create().show()
    }

    private fun addPlayerHandler() {
        val game = GameManager.getInstance(this@MainActivity).currentlyActiveGame
        if (game == null) {
            Toast.makeText(this@MainActivity, R.string.no_game_active_toast, Toast.LENGTH_LONG).show()
            return
        }

        game.createPlayer("")

        redraw()

        val viewToFocus = headerRowEditTextViews?.last() ?: return
        viewToFocus.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(viewToFocus, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.nav_imprint -> {
                val imprintIntent = Intent(this, AboutActivity::class.java)
                startActivity(imprintIntent)
            }
            R.id.nav_website -> startURLIntent(AppConfig.websiteURL)
            R.id.nav_instagram -> startURLIntent(AppConfig.instagramURL)
            R.id.nav_share -> {
                val sendIntent = Intent(Intent.ACTION_SEND)
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        getString(R.string.share_message, getString(R.string.play_store_url)))
                sendIntent.type = "text/plain"
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_screen_title)))
            }
            R.id.nav_github -> startURLIntent(AppConfig.githubURL)
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun startURLIntent(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    private fun headerRowUpdateLineNumber() {
        val game = GameManager.getInstance(this).currentlyActiveGame ?: return
        headerRowViewHolder.setLineNumber(game.scoreCount)
    }

    private fun sumRowUpdateLineNumber() {
        val game = GameManager.getInstance(this).currentlyActiveGame ?: return
        sumRowViewHolder.setLineNumber(game.scoreCount)
    }

    fun updateLineNumberWidth() {
        headerRowUpdateLineNumber()
        sumRowUpdateLineNumber()
        headerRowViewHolder.lineNumberTextView.requestLayout()
        sumRowViewHolder.lineNumberTextView.requestLayout()
    }

    private fun redraw(refreshGameData: Boolean = false, redrawHeaderRow: Boolean = true, notifyDataSetChanged: Boolean = true, redrawSumRow: Boolean = true, redrawLeaderBoard: Boolean = true) {
        if (refreshGameData) {
            backingMainTableAdapter = null
            setRecyclerViewUp()
        }
        if (redrawHeaderRow)
            renderHeaderRow()
        if (notifyDataSetChanged)
            mainTableAdapter.notifyDataSetChanged()
        if (redrawSumRow)
            renderSumRow()
        if (redrawLeaderBoard)
            renderLeaderboard()
    }

    private fun renderHeaderRow() {
        val game = GameManager.getInstance(this).currentlyActiveGame
        val editTextViews = mutableListOf<EditText>()
        headerRowUpdateLineNumber()
        headerRowViewHolder.lineNumberTextView.visibility = View.INVISIBLE
        headerRowViewHolder.deleteRowButton.visibility = View.INVISIBLE

        if (game == null) {
            headerRowViewHolder.scoreHolderLayout.removeAllViews()
            headerRowEditTextViews = editTextViews
            return
        }

        val players = game.players
        headerRowViewHolder.scoreHolderLayout.removeAllViews()

        players.forEach {
            val editText = EditText(headerRowViewHolder.view.context)
            editTextViews.add(editText)

            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            editText.layoutParams = layoutParams

            editText.inputType = EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
            editText.setHorizontallyScrolling(false)
            editText.maxLines = AppConfig.maxLinesForEnterText
            editText.hint = getPlayerDummyName(game, it, players)
            editText.setText(it.name)
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    it.name = s.toString()
                    renderLeaderboard()
                }
            })

            headerRowViewHolder.scoreHolderLayout.addView(editText)
        }

        headerRowEditTextViews = editTextViews
    }

    fun renderSumRow() {
        val game = GameManager.getInstance(this).currentlyActiveGame
        sumRowViewHolder.lineNumberTextView.visibility = View.INVISIBLE

        if (game == null) {
            sumRowViewHolder.scoreHolderLayout.removeAllViews()
            return
        }

        val players = game.players
        val winnerIndices = game.winners
        val looserIndices = game.loosers
        sumRowViewHolder.scoreHolderLayout.removeAllViews()

        players.forEachIndexed { index, player ->
            val textView = TextView(sumRowViewHolder.view.context)
            val sum = player.totalScore
            @Suppress("DEPRECATION")
            textView.setTextColor(resources.getColor(R.color.sumRowFontColor))
            textView.text = textView.context.getString(R.string.main_table_score_template, sum)
            textView.gravity = Gravity.CENTER
            textView.setPadding(0, 15, 0, 15)

            @Suppress("DEPRECATION")
            if (winnerIndices.contains(index))
                textView.setBackgroundColor(resources.getColor(R.color.winnerColor))
            else if (looserIndices.contains(index))
                textView.setBackgroundColor(resources.getColor(R.color.looserColor))

            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            textView.layoutParams = layoutParams

            sumRowViewHolder.scoreHolderLayout.addView(textView)
        }
    }

    fun renderLeaderboard() {
        val game = GameManager.getInstance(this).currentlyActiveGame
        leaderboard_table.removeAllViews()
        val textColor = Color.WHITE

        if (game == null || game.players.isEmpty()) {
            val emptyRow = TableRow(this)
            val emptyLabel = TextView(this)
            emptyLabel.setText(R.string.leaderboard_empty)
            emptyLabel.setTextColor(textColor)
            emptyRow.addView(emptyLabel)
            leaderboard_table.addView(emptyRow)
            return
        }

        val ranking = game.ranking
        var i = 0

        val textViewLayoutParams = TableRow.LayoutParams()
        textViewLayoutParams.rightMargin = 10.toPx(this)

        for ((key, value) in ranking) {
            val row = TableRow(this)

            val rankTextView = TextView(this)
            val playerNameTextView = TextView(this)
            val scoreTextView = TextView(this)

            rankTextView.text = getString(R.string.leaderboard_rank_template, i + 1)
            playerNameTextView.text = getString(R.string.leaderboard_player_name_template, getPlayerNameOrDummy(game, key))
            scoreTextView.text = getString(R.string.leaderboard_score_template, value)

            rankTextView.setTextColor(textColor)
            playerNameTextView.setTextColor(textColor)
            scoreTextView.setTextColor(textColor)

            rankTextView.layoutParams = textViewLayoutParams
            playerNameTextView.layoutParams = textViewLayoutParams
            scoreTextView.layoutParams = textViewLayoutParams

            row.addView(rankTextView)
            row.addView(playerNameTextView)
            row.addView(scoreTextView)

            leaderboard_table.addView(row)
            i++
        }
    }

    interface StringPromptResultHandler {
        fun onOk(result: String)

        fun onCancel()
    }

    companion object {
        private fun createStringPrompt(context: Context, @StringRes title: Int, @StringRes okText: Int, @StringRes cancelText: Int, resultHandler: StringPromptResultHandler, defaultText: String? = null, defaultHint: String? = null) {
            createStringPrompt(context, context.getText(title), context.getText(okText), context.getText(cancelText), resultHandler, defaultText, defaultHint)
        }

        private fun createStringPrompt(context: Context, title: CharSequence, okText: CharSequence, cancelText: CharSequence, resultHandler: StringPromptResultHandler, defaultText: String? = null, defaultHint: String? = null) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)

            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_TEXT
            if (defaultText != null)
                input.setText(defaultText)
            if (defaultHint != null)
                input.hint = defaultHint
            builder.setView(input)

            builder.setPositiveButton(okText) { _, _ -> resultHandler.onOk(input.text.toString()) }
            builder.setNegativeButton(cancelText) { dialog, _ ->
                dialog.cancel()
                resultHandler.onCancel()
            }

            builder.show()
        }
    }
}
