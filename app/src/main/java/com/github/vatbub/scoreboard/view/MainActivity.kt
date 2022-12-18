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
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.vatbub.scoreboard.R
import com.github.vatbub.scoreboard.data.Game
import com.github.vatbub.scoreboard.data.GameManager
import com.github.vatbub.scoreboard.data.GameMode
import com.github.vatbub.scoreboard.data.Player
import com.github.vatbub.scoreboard.util.ResettableLazyProperty
import com.github.vatbub.scoreboard.util.ViewUtil
import com.github.vatbub.scoreboard.util.toPx
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import net.steamcrafted.materialiconlib.MaterialDrawableBuilder
import net.steamcrafted.materialiconlib.MaterialIconView
import net.steamcrafted.materialiconlib.MaterialMenuInflater
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val showSubTotalsDefaultValue = false
    private val showSubTotalsKey = "showSubTotals"
    private val sharedPreferencesName = "MainActivityPrefs"
    private val sharedPreferences by lazy { getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)!! }
    private var showSubtotals: Boolean
        get() = sharedPreferences.getBoolean(showSubTotalsKey, showSubTotalsDefaultValue)
        set(value) = sharedPreferences.edit().putBoolean(showSubTotalsKey, value).apply()
    private val gameManager by lazy { GameManager[this] }

    private val mainTableAdapter = ResettableLazyProperty {
        GameTableRecyclerViewAdapter(main_table_recycler_view, gameManager.currentlyActiveGame!!, this, showSubtotals) {
            showSubtotals = it
            updateShowSubTotalsMenuItem(it)
        }
    }

    private val headerRowViewHolder by lazy { GameTableViewHolder(header_row, false) }
    private val sumRowViewHolder by lazy { GameTableViewHolderWithPlaceholders(sum_row) }
    private val mBottomSheetBehavior by lazy { BottomSheetBehavior.from(sum_bottom_sheet) }

    private var optionsMenu: Menu? = null
    private var lastAddPlayerActionBarCenterX = -1f
    private var lastAddPlayerArrowWidth = -1
    private var lastAddPlayerHintWidth = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_NoActionBar)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        gameManager.createGameIfEmptyAndActivateTheLastActivatedGame()

        setSumBottomSheetUp()
        setRecyclerViewUp()

        redraw()
        headerRowViewHolder.lineNumberTextView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> mainTableAdapter.value.updateColumnWidths(headerRowViewHolder.lineNumberTextView.width) }

        setAddPlayerHintLayoutListenerUp()
        setFabListenerUp()
        setNavigationDrawerUp()
        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun setAddPlayerHintLayoutListenerUp() {
        // add_player_hint_arrow.layoutParams.width
        add_player_hint_arrow.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            val oldWidth = abs(oldRight - oldLeft)
            lastAddPlayerArrowWidth = abs(right - left)
            if (oldWidth != lastAddPlayerArrowWidth)
                informAddPlayerButtonLocation()
        }
        add_player_hint.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            val oldWidth = abs(oldRight - oldLeft)
            lastAddPlayerHintWidth = abs(right - left)
            if (oldWidth != lastAddPlayerHintWidth)
                informAddPlayerButtonLocation()
        }
    }

    private fun setFabListenerUp() {
        fab.setOnClickListener {
            val game = gameManager.currentlyActiveGame
            if (game == null) {
                Toast.makeText(this, R.string.no_game_active_toast, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            game.addEmptyScoreLine()
            updateLineNumberWidth()
            mainTableAdapter.value.notifyItemInserted(game.scoreCount)
            focusOnLastRow(game)
        }
    }

    private fun focusOnLastRow(game: Game) {
        main_table_recycler_view.smoothScrollToPosition(game.scoreCount)
        focusOnLastRowSecondStep(game)
    }

    private fun focusOnLastRowSecondStep(game: Game) {
        ViewUtil.runOnGlobalLayoutChange<RecyclerView>(this, R.id.main_table_recycler_view, ViewUtil.RemoveListenerPolicy.Always) {
            val viewHolder = main_table_recycler_view.findViewHolderForAdapterPosition(game.scoreCount - 1)
            if (viewHolder == null) {
                // Layout not yet complete, reschedule the action
                focusOnLastRowSecondStep(game)
                return@runOnGlobalLayoutChange
            }

            viewHolder as GameTableViewHolder
            viewHolder.scoreHolderLayout.getChildAt(0).requestFocus()
        }
    }

    private fun setNavigationDrawerUp() {
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setRecyclerViewUp() {
        main_table_recycler_view.layoutManager = LinearLayoutManager(this)
        main_table_recycler_view.adapter = mainTableAdapter.value
        main_table_recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val scrollPosition = recyclerView.computeVerticalScrollOffset()
                val maxScrollPosition = 31
                header_row_shadow_view.alpha = min(1.0f, (1.0f / maxScrollPosition) * scrollPosition)
            }
        })
        mainTableAdapter.value.notifyDataSetChanged()
    }

    private fun calculateBottomSheetHeightFromOffset(bottomSheet: View, slideOffset: Float): Int {
        val peekHeight = mBottomSheetBehavior.peekHeight
        val fullHeight = bottomSheet.height
        if (slideOffset == 0f) return peekHeight
        if (slideOffset < 0f) return ((slideOffset + 1) * peekHeight).toInt()
        return ((slideOffset * (fullHeight - peekHeight)) + peekHeight).toInt()
    }

    private fun setSumBottomSheetUp() {
        ViewUtil.runJustBeforeBeingDrawn(sum_row) { mBottomSheetBehavior.peekHeight = it.height }
        mBottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val layoutParams = sum_bottom_sheet_scroll_space.layoutParams
                layoutParams.height = calculateBottomSheetHeightFromOffset(bottomSheet, slideOffset)
                sum_bottom_sheet_scroll_space.layoutParams = layoutParams
            }

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

        val menuItem = menu.findItem(R.id.action_add_player)
        val actionView = MaterialIconView(this)
        actionView.setOnClickListener { onOptionsItemSelected(menuItem) }
        actionView.setIcon(MaterialDrawableBuilder.IconValue.ACCOUNT_PLUS)
        actionView.setToActionbarSize()
        actionView.setColor(Color.WHITE)
        menuItem.actionView = actionView
        println("menuItem.isVisible: ${menuItem.isVisible}")
        actionView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(view: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                if (view == null) return
                if (left == 0 && top == 0 && right == 0 && bottom == 0) return

                val location = IntArray(2)
                view.getLocationOnScreen(location)
                lastAddPlayerActionBarCenterX = location[0] + (right - left) / 2f
                println("centerX: $lastAddPlayerActionBarCenterX")
                informAddPlayerButtonLocation()
            }
        })
        updateShowSubTotalsMenuItem(mainTableAdapter.value.showSubTotal)
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
                mainTableAdapter.value.showSubTotal = !mainTableAdapter.value.showSubTotal
                return true
            }
            R.id.action_sync -> {
                val syncIntent = Intent(this, SyncActivity::class.java)
                startActivity(syncIntent)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun newGameHandler() {
        val currentGame = gameManager.currentlyActiveGame

        if (currentGame == null) {
            newGameHandlerCreateNewGame()
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.alert_new_game_delete_current_game)

            builder.setPositiveButton(R.string.yes) { _, _ ->
                gameManager.deleteGame(currentGame)
                newGameHandlerCreateNewGame()
            }
            builder.setNegativeButton(R.string.no) { _, _ ->
                Snackbar.make(root_node, R.string.snackbar_new_game_manage_games_hint, Snackbar.LENGTH_LONG).show()
                newGameHandlerCreateNewGame()
            }
            builder.create().show()
        }
    }

    private fun newGameHandlerCreateNewGame() {
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
        val gameNames = gameManager.games.map { it.nameOrDummyName }.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.manage_saved_games_title)

        val indicesToDelete = BooleanArray(gameManager.games.size)

        builder.setMultiChoiceItems(gameNames, indicesToDelete
        ) { _: DialogInterface, index: Int, checked: Boolean ->
            indicesToDelete[index] = checked
        }
        builder.setPositiveButton(R.string.dialog_ok) { _, _ ->
            val gamesToDelete = mutableListOf<Game>()
            indicesToDelete.forEachIndexed { index, checked ->
                if (checked)
                    gamesToDelete.add(gameManager.games[index])
            }
            gamesToDelete.forEach { gameManager.deleteGame(it) }
            gameManager.createGameIfEmptyAndActivateTheLastActivatedGame()
            redraw(true)
        }
        builder.setNegativeButton(R.string.dialog_cancel) { _, _ -> }
        builder.create().show()
    }

    private fun loadGameHandler() {
        val gameNames = gameManager.games.map { it.nameOrDummyName }
        val currentGame = gameManager.currentlyActiveGame
        val inputSelection = if (currentGame == null)
            0
        else
            gameManager.games.indexOf(currentGame)

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.load_game_title)

        builder.setSingleChoiceItems(gameNames.toTypedArray(), inputSelection
        ) { dialogInterface, selectedItem ->
            gameManager.activateGame(gameManager.games[selectedItem])
            dialogInterface.dismiss()
            redraw(true)
        }
        builder.create().show()
    }

    private fun saveGameHandler() {
        val game = gameManager.currentlyActiveGame ?: return
        createStringPrompt(this, R.string.save_game_dialog_title, R.string.dialog_ok, R.string.dialog_cancel, defaultText = game.nameOrDummyName, defaultHint = getString(R.string.save_game_dialog_hint), resultHandler = object : StringPromptResultHandler {
            override fun onOk(result: String) {
                game.name = result
            }

            override fun onCancel() {}
        })
    }

    private fun switchModeHandler() {
        val currentGame = gameManager.currentlyActiveGame
        val inputSelection = if (currentGame == null) {
            0
        } else when (currentGame.mode) {
            GameMode.HIGH_SCORE -> 0
            GameMode.LOW_SCORE -> 1
        }

        val items = GameMode.values().map { it.getNameString(this) }.toTypedArray()

        with(AlertDialog.Builder(this)) {
            setTitle(R.string.switch_mode_title)
            setSingleChoiceItems(items, inputSelection) { dialogInterface, selectedItem ->
                if (currentGame == null)
                    return@setSingleChoiceItems

                if (selectedItem == 0)
                    currentGame.mode = GameMode.HIGH_SCORE
                else if (selectedItem == 1)
                    currentGame.mode = GameMode.LOW_SCORE
                dialogInterface.dismiss()
                redraw(redrawHeaderRow = false, notifyDataSetChanged = false)
            }
            create().show()
        }
    }

    private fun verifyPlayer(player: Player, players: List<Player>) {
        if (!players.contains(player))
            throw IllegalArgumentException("Player must be part of the specified game.")
    }

    private fun getPlayerNameOrDummy(game: Game, player: Player): String {
        verifyPlayer(player, game.players)
        var finalName = player.name
        if (finalName == null || finalName.replace(" ", "") == "")
            finalName = getPlayerDummyName(game, player)
        return finalName
    }

    private fun getPlayerDummyName(game: Game, player: Player): String {
        verifyPlayer(player, game.players)
        val index = game.players.indexOf(player) + 1
        return getString(R.string.player_no_name_template, index)
    }

    private fun removePlayerHandler() {
        val currentGame = gameManager.currentlyActiveGame ?: return
        val playerNames = currentGame.players.map { getPlayerNameOrDummy(currentGame, it) }.toTypedArray()

        with(AlertDialog.Builder(this)) {
            setTitle(R.string.delete_player_title)
            val indicesToDelete = BooleanArray(playerNames.size)
            setMultiChoiceItems(playerNames, indicesToDelete) { _, index, checked -> indicesToDelete[index] = checked }
            setPositiveButton(R.string.dialog_ok) { _, _ ->
                val playersToDelete = mutableListOf<Player>()
                indicesToDelete.forEachIndexed { index, checked ->
                    if (checked)
                        playersToDelete.add(currentGame.players[index])
                }
                playersToDelete.forEach { currentGame.players.remove(it) }
                redraw()
            }
            setNegativeButton(R.string.dialog_cancel) { _, _ -> }
            create().show()
        }
    }

    private fun addPlayerHandler() {
        val game = gameManager.currentlyActiveGame
        if (game == null) {
            Toast.makeText(this@MainActivity, R.string.no_game_active_toast, Toast.LENGTH_LONG).show()
            return
        }

        game.createPlayer("")

        redraw()

        if (headerRowViewHolder.scoreHolderLayout.childCount == 0) return
        val viewToFocus = headerRowViewHolder.scoreHolderLayout.getChildAt(headerRowViewHolder.scoreHolderLayout.childCount - 1)
        viewToFocus.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(viewToFocus, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_imprint -> {
                val imprintIntent = Intent(this, AboutActivity::class.java)
                startActivity(imprintIntent)
            }
            R.id.nav_website -> startURLIntent(R.string.website_url)
            R.id.nav_instagram -> startURLIntent(R.string.instagram_url)
            R.id.nav_share -> {
                val sendIntent = Intent(Intent.ACTION_SEND)
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        getString(R.string.share_message, getString(R.string.play_store_url)))
                sendIntent.type = "text/plain"
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_screen_title)))
            }
            R.id.nav_github -> startURLIntent(R.string.github_url)
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun startURLIntent(@StringRes urlRes: Int) {
        with(Intent(Intent.ACTION_VIEW)) {
            data = Uri.parse(getString(urlRes))
            startActivity(this)
        }
    }

    private fun headerRowUpdateLineNumber() {
        val game = gameManager.currentlyActiveGame ?: return
        headerRowViewHolder.lineNumber = game.scoreCount
    }

    private fun sumRowUpdateLineNumber() {
        val game = gameManager.currentlyActiveGame ?: return
        sumRowViewHolder.setLineNumber(game.scoreCount)
    }

    fun updateLineNumberWidth() {
        headerRowUpdateLineNumber()
        sumRowUpdateLineNumber()
        headerRowViewHolder.lineNumberTextView.requestLayout()
        sumRowViewHolder.lineNumberTextView.requestLayout()
    }

    fun redraw(refreshGameData: Boolean = false, redrawHeaderRow: Boolean = true, notifyDataSetChanged: Boolean = true, redrawSumRow: Boolean = true, redrawLeaderBoard: Boolean = true, updateFabButtonHint: Boolean = true, updateAddPlayerHint: Boolean = true) {
        if (refreshGameData) {
            mainTableAdapter.resetValue()
            setRecyclerViewUp()
        }
        if (redrawHeaderRow)
            renderHeaderRow()
        if (notifyDataSetChanged)
            mainTableAdapter.value.notifyDataSetChanged()
        if (redrawSumRow)
            renderSumRow()
        if (redrawLeaderBoard)
            renderLeaderBoard()
        if (updateFabButtonHint)
            updateFabButtonHint()
        if (updateAddPlayerHint)
            updateAddPlayerHint()
    }

    private fun informAddPlayerButtonLocation() {
        if (lastAddPlayerActionBarCenterX < 0) return
        if (lastAddPlayerArrowWidth < 0) return
        if (lastAddPlayerHintWidth < 0) return
        add_player_hint_arrow.x = lastAddPlayerActionBarCenterX - lastAddPlayerArrowWidth / 2f

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val maxHintLeft = screenWidth - lastAddPlayerHintWidth
        val desiredHintLeft = lastAddPlayerActionBarCenterX - lastAddPlayerHintWidth / 2f
        add_player_hint.x = max(0f, min(maxHintLeft.toFloat(), desiredHintLeft))
    }

    private fun updateAddPlayerHint() {
        val currentGame = gameManager.currentlyActiveGame
        val targetArrowAlpha =
                if (currentGame != null && currentGame.players.isEmpty())
                    1f
                else 0f
        val targetHeaderRowAlpha = 1f - targetArrowAlpha

        val animationDuration = 150L
        add_player_hint_arrow.animate()
                .alpha(targetArrowAlpha)
                .setDuration(animationDuration)
                .start()
        add_player_hint.animate()
                .alpha(targetArrowAlpha)
                .setDuration(animationDuration)
                .start()
        header_row.animate()
                .alpha(targetHeaderRowAlpha)
                .setDuration(animationDuration)
                .start()
        header_row_shadow_view.animate()
                .alpha(targetHeaderRowAlpha)
                .setDuration(animationDuration)
                .start()
    }

    private fun updateFabButtonHint() {
        val currentGame = gameManager.currentlyActiveGame
        val targetAlpha = when {
            currentGame == null -> 0f
            currentGame.players.isEmpty() -> 0f
            currentGame.scoreCount > 0 -> 0f
            else -> 1f
        }
        fab_hint.animate()
                .alpha(targetAlpha)
                .setDuration(250)
                .start()
    }

    private fun renderHeaderRow() {
        headerRowUpdateLineNumber()
        headerRowViewHolder.lineNumberTextView.visibility = View.INVISIBLE
        headerRowViewHolder.deleteRowButton.visibility = View.INVISIBLE

        headerRowViewHolder.scoreHolderLayout.removeAllViews()

        val game = gameManager.currentlyActiveGame ?: return
        game.players.forEach {
            with(EditText(headerRowViewHolder.view.context)) {
                val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                this.layoutParams = layoutParams

                inputType = EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
                setHorizontallyScrolling(false)
                maxLines = resources.getInteger(R.integer.max_lines_for_enter_text)
                hint = getPlayerDummyName(game, it)
                setText(it.name)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable) {
                        it.name = s.toString()
                        redraw(refreshGameData = false, redrawHeaderRow = false, notifyDataSetChanged = false, redrawSumRow = false, redrawLeaderBoard = true, updateFabButtonHint = false)
                    }
                })

                headerRowViewHolder.scoreHolderLayout.addView(this)
            }
        }
    }

    private fun renderSumRow() {
        sumRowViewHolder.lineNumberTextView.visibility = View.INVISIBLE
        sumRowViewHolder.scoreHolderLayout.removeAllViews()

        val game = gameManager.currentlyActiveGame ?: return
        val winnerIndices = game.winners
        val looserIndices = game.loosers

        game.players.forEachIndexed { index, player ->
            with(TextView(sumRowViewHolder.view.context)) {
                @Suppress("DEPRECATION")
                setTextColor(resources.getColor(R.color.sumRowFontColor))
                text = context.getString(R.string.main_table_score_template, player.totalScore)
                gravity = Gravity.CENTER
                setPadding(0, 15, 0, 15)

                @Suppress("DEPRECATION")
                if (winnerIndices.contains(index))
                    setBackgroundColor(resources.getColor(R.color.winnerColor))
                else if (looserIndices.contains(index))
                    setBackgroundColor(resources.getColor(R.color.looserColor))

                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                sumRowViewHolder.scoreHolderLayout.addView(this)
            }
        }
    }

    private fun renderLeaderBoard() {
        leaderboard_table.removeAllViews()
        val textColor = Color.WHITE

        val game = gameManager.currentlyActiveGame
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

            with(TableRow(this)) {
                addView(rankTextView)
                addView(playerNameTextView)
                addView(scoreTextView)
                leaderboard_table.addView(this)
            }
            i++
        }
    }

    interface StringPromptResultHandler {
        fun onOk(result: String)
        fun onCancel()
    }

    companion object {
        private fun createStringPrompt(context: Context, @StringRes title: Int, @StringRes okText: Int, @StringRes cancelText: Int, resultHandler: StringPromptResultHandler, defaultText: String? = null, defaultHint: String? = null) =
                createStringPrompt(context, context.getText(title), context.getText(okText), context.getText(cancelText), resultHandler, defaultText, defaultHint)

        private fun createStringPrompt(context: Context, title: CharSequence, okText: CharSequence, cancelText: CharSequence, resultHandler: StringPromptResultHandler, defaultText: String? = null, defaultHint: String? = null) {
            with(AlertDialog.Builder(context)) {
                setTitle(title)

                val input = EditText(context)
                input.inputType = InputType.TYPE_CLASS_TEXT
                if (defaultText != null)
                    input.setText(defaultText)
                if (defaultHint != null)
                    input.hint = defaultHint
                setView(input)

                setPositiveButton(okText) { _, _ -> resultHandler.onOk(input.text.toString()) }
                setNegativeButton(cancelText) { dialog, _ ->
                    dialog.cancel()
                    resultHandler.onCancel()
                }

                show()
            }
        }
    }
}
