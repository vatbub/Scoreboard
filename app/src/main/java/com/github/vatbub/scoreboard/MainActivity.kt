package com.github.vatbub.scoreboard

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
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
import com.github.vatbub.scoreboard.data.Game
import com.github.vatbub.scoreboard.data.GameManager
import com.github.vatbub.scoreboard.data.GameMode
import com.github.vatbub.scoreboard.data.Player
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import net.steamcrafted.materialiconlib.MaterialMenuInflater

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var backingMainTableAdapter: GameTableRecyclerViewAdapter? = null
    private val mainTableAdapter: GameTableRecyclerViewAdapter
        get() {
            if (backingMainTableAdapter == null)
                backingMainTableAdapter = GameTableRecyclerViewAdapter(main_table_recycler_view, GameManager.getInstance(this).currentlyActiveGame!!, this)
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

    private var headerRowEditTextViews: List<EditText>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCommonLibUp()
        AppConfig.initialize(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val gameManager = GameManager.getInstance(this)
        if (gameManager.games.isEmpty())
            gameManager.createGame("dummyGame")

        gameManager.activateGame(gameManager.games[0])
        renderHeaderRow()
        setSumBottomSheetUp()
        renderSumRow()
        renderLeaderboard()
        headerRowViewHolder.lineNumberTextView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> mainTableAdapter.updateColumnWidths(headerRowViewHolder.lineNumberTextView.width) }

        setFabListenerUp()
        setNavigationDrawerUp()
        nav_view.setNavigationItemSelectedListener(this)

        setRecyclerViewUp()
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
        val mBottomSheetBehavior = BottomSheetBehavior.from(sum_bottom_sheet)
        // mBottomSheetBehavior.peekHeight = sum_row.height
        ViewUtil.runJustBeforeBeingDrawn(sum_row) { mBottomSheetBehavior.peekHeight = it.height }
        // 31.toPx(this)
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START))
            drawer_layout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        MaterialMenuInflater.with(this)
                .setDefaultColor(Color.WHITE)
                .inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_add_player -> {
                addPlayerHandler()
                return true
            }
            R.id.action_remove_player -> {
                removePlayerHandler()
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
            R.id.action_load_game -> {
                loadGameHandler()
                return true
            }
            R.id.action_manage_saved_games -> {
                manageSavedGamesHandler()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun manageSavedGamesHandler() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun loadGameHandler() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun saveGameHandler() {
        val game = GameManager.getInstance(this@MainActivity).currentlyActiveGame
        val currentName = if (game != null && game.name != "")
            game.name
        else
            null

        createStringPrompt(this, R.string.save_game_dialog_title, R.string.dialog_ok, R.string.dialog_cancel, defaultText = currentName, defaultHint = getString(R.string.save_game_dialog_hint), resultHandler = object : StringPromptResultHandler {
            override fun onOk(result: String) {
                if (game == null) {
                    Toast.makeText(this@MainActivity, R.string.no_game_active_toast, Toast.LENGTH_LONG).show()
                    return
                }

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

    private fun getPlayerNameOrDummy(game: Game, player: Player, players: List<Player> = game.players): String {
        if (!players.contains(player))
            throw IllegalArgumentException("Player must be part of the specified game.")
        val index = players.indexOf(player) + 1
        return player.name ?: getString(R.string.player_no_name_template, index)
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
            renderHeaderRow()
            renderLeaderboard()
            renderSumRow()
            mainTableAdapter.notifyDataSetChanged()
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

        renderHeaderRow()
        renderSumRow()
        renderLeaderboard()
        mainTableAdapter.notifyDataSetChanged()

        val viewToFocus = headerRowEditTextViews?.last() ?: return
        viewToFocus.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(viewToFocus, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.nav_imprint -> {
                // TODO: Imprint
            }
            R.id.nav_website -> startURLIntent(AppConfig.websiteURL)
            R.id.nav_instagram -> startURLIntent(AppConfig.instagramURL)
            R.id.nav_share -> {
                // TODO: Share
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

        if (game == null) {
            val emptyRow = TableRow(this)
            val emptyLabel = TextView(this)
            emptyLabel.setText(R.string.leaderboard_empty)
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

            val textColor = Color.WHITE
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
