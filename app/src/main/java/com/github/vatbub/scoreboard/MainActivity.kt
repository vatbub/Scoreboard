package com.github.vatbub.scoreboard

import android.content.Context
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
import android.widget.*
import com.github.vatbub.common.core.Common
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import net.steamcrafted.materialiconlib.MaterialMenuInflater

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var backingMainTableAdapter: GameTableRecyclerViewAdapter? = null
    private val mainTableAdapter: GameTableRecyclerViewAdapter
        get() {
            if (backingMainTableAdapter == null)
                backingMainTableAdapter = GameTableRecyclerViewAdapter(main_table_recycler_view, GameManager.getInstance(this).currentlyActiveGame, this)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCommonLibUp()
        AppConfig.initialize(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val gameManager = GameManager.getInstance(this)
        if (gameManager.listGames().isEmpty())
            gameManager.createGame("dummyGame")

        gameManager.activateGame(gameManager.listGames()[0])
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
        val mBottomSheetBehavior = BottomSheetBehavior.from(findViewById<View>(R.id.sum_bottom_sheet))
        mBottomSheetBehavior.peekHeight = 111
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
            GameManager.GameMode.HIGH_SCORE -> 0
            GameManager.GameMode.LOW_SCORE -> 1
        }

        val items = arrayOf<CharSequence>(getString(R.string.switch_mode_highscore), getString(R.string.switch_mode_lowscore))

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.switch_mode_title)

        builder.setSingleChoiceItems(items, inputSelection
        ) { dialogInterface, selectedItem ->
            if (currentGame == null)
                return@setSingleChoiceItems

            if (selectedItem == 0)
                currentGame.mode = GameManager.GameMode.HIGH_SCORE
            else if (selectedItem == 1)
                currentGame.mode = GameManager.GameMode.LOW_SCORE
            dialogInterface.dismiss()
            renderSumRow()
            renderLeaderboard()
        }
        builder.create().show()
    }

    private fun removePlayerHandler() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun addPlayerHandler() =
            createStringPrompt(this, R.string.edit_player_name, R.string.dialog_ok, R.string.dialog_cancel, object : StringPromptResultHandler {
                override fun onOk(result: String) {
                    val game = GameManager.getInstance(this@MainActivity).currentlyActiveGame
                    if (game == null) {
                        Toast.makeText(this@MainActivity, R.string.no_game_active_toast, Toast.LENGTH_LONG).show()
                        return
                    }

                    game.createPlayer(result)

                    renderHeaderRow()
                    renderSumRow()
                    renderLeaderboard()
                    mainTableAdapter.notifyDataSetChanged()
                }

                override fun onCancel() {}
            })

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
        headerRowUpdateLineNumber()
        headerRowViewHolder.lineNumberTextView.visibility = View.INVISIBLE
        headerRowViewHolder.deleteRowButton.visibility = View.INVISIBLE

        if (game == null) {
            headerRowViewHolder.scoreHolderLayout.removeAllViews()
            return
        }

        val players = game.players
        headerRowViewHolder.scoreHolderLayout.removeAllViews()

        for (i in players.indices) {
            val player = players[i]
            val editText = EditText(headerRowViewHolder.view.context)

            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            editText.layoutParams = layoutParams

            editText.inputType = EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
            editText.setHorizontallyScrolling(false)
            editText.maxLines = AppConfig.maxLinesForEnterText
            editText.setText(player.name)
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    players[i].name = s.toString()
                    renderLeaderboard()
                }
            })

            headerRowViewHolder.scoreHolderLayout.addView(editText)
        }
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

        for (i in players.indices) {
            val player = players[i]
            val textView = TextView(sumRowViewHolder.view.context)
            val sum = player.totalScore
            textView.text = textView.context.getString(R.string.main_table_score_template, sum)
            textView.gravity = Gravity.CENTER
            textView.setPadding(0, 15, 0, 15)

            @Suppress("DEPRECATION")
            if (winnerIndices.contains(i))
                textView.setBackgroundColor(resources.getColor(R.color.winnerColor))
            else if (looserIndices.contains(i))
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

        for ((key, value) in ranking) {
            val row = TableRow(this)
            val textView = TextView(this)
            textView.text = getString(R.string.leaderboard_template, i + 1, key.name, value)
            row.addView(textView)
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
