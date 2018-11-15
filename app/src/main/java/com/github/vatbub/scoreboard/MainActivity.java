package com.github.vatbub.scoreboard;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.vatbub.common.core.Common;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView mainTable;
    private GameTableRecyclerViewAdapter mainTableAdapter;

    public static void createStringPrompt(Context context, @StringRes int title, @StringRes int okText, @StringRes int cancelText, StringPromptResultHandler resultHandler) {
        createStringPrompt(context, context.getText(title), context.getText(okText), context.getText(cancelText), resultHandler);
    }

    public static void createStringPrompt(Context context, @StringRes int title, @StringRes int okText, @StringRes int cancelText, @Nullable String defaultText, StringPromptResultHandler resultHandler) {
        createStringPrompt(context, context.getText(title), context.getText(okText), context.getText(cancelText), defaultText, resultHandler);
    }

    public static void createStringPrompt(Context context, @StringRes int title, @StringRes int okText, @StringRes int cancelText, @Nullable String defaultText, @Nullable String defaultHint, StringPromptResultHandler resultHandler) {
        createStringPrompt(context, context.getText(title), context.getText(okText), context.getText(cancelText), defaultText, defaultHint, resultHandler);
    }

    public static void createStringPrompt(Context context, CharSequence title, CharSequence okText, CharSequence cancelText, StringPromptResultHandler resultHandler) {
        createStringPrompt(context, title, okText, cancelText, null, resultHandler);
    }

    public static void createStringPrompt(Context context, CharSequence title, CharSequence okText, CharSequence cancelText, @Nullable String defaultText, StringPromptResultHandler resultHandler) {
        createStringPrompt(context, title, okText, cancelText, defaultText, null, resultHandler);
    }

    public static void createStringPrompt(Context context, CharSequence title, CharSequence okText, CharSequence cancelText, @Nullable String defaultText, @Nullable String defaultHint, StringPromptResultHandler resultHandler) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
// R.string.edit_player_name

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (defaultText != null)
            input.setText(defaultText);
        if (defaultHint != null)
            input.setHint(defaultHint);
        builder.setView(input);

        // R.string.dialog_ok
        builder.setPositiveButton(okText, (dialog, which) -> resultHandler.onOk(input.getText().toString()));
        builder.setNegativeButton(cancelText, (dialog, which) -> {
            dialog.cancel();
            resultHandler.onCancel();
        });

        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Common.useAndroidImplementation(this);
        Common.getInstance().setAppName(BuildConfig.APPLICATION_ID);
        AppConfig.initialize(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (GameManager.getInstance(this).listGames().isEmpty())
            GameManager.getInstance(this).createGame("dummyGame");

        GameManager.getInstance(this).activateGame(GameManager.getInstance(this).listGames().get(0));
        renderHeaderRow();
        renderSumRow();
        getHeaderRowViewHolder().getLineNumberTextView().addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> mainTableAdapter.updateColumnWidths(getHeaderRowViewHolder().getLineNumberTextView().getWidth()));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            GameManager.Game game = GameManager.getInstance(MainActivity.this).getCurrentlyActiveGame();
            if (game == null) {
                Toast.makeText(this, R.string.no_game_active_toast, Toast.LENGTH_LONG).show();
                return;
            }

            game.addEmptyScoreLine();
            updateLineNumberWidth();
            mainTableAdapter.notifyItemInserted(game.getScoreCount());
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mainTable = findViewById(R.id.main_table_recycler_view);
        getMainTable().setLayoutManager(new LinearLayoutManager(this));
        mainTableAdapter = new GameTableRecyclerViewAdapter(getMainTable(), GameManager.getInstance(this).getCurrentlyActiveGame(), this);
        getMainTable().setAdapter(mainTableAdapter);
        mainTableAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add_player) {
            createStringPrompt(this, R.string.edit_player_name, R.string.dialog_ok, R.string.dialog_cancel, new StringPromptResultHandler() {
                @Override
                public void onOk(String result) {
                    GameManager.Game game = GameManager.getInstance(MainActivity.this).getCurrentlyActiveGame();
                    if (game == null) {
                        Toast.makeText(MainActivity.this, R.string.no_game_active_toast, Toast.LENGTH_LONG).show();
                        return;
                    }

                    game.createPlayer(result);

                    renderHeaderRow();
                    renderSumRow();
                    mainTableAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancel() {
                }
            });
            return true;
        } else if (id == R.id.action_remove_player) {

        } else if (id == R.id.action_switch_mode) {
            GameManager.Game currentGame = GameManager.getInstance(MainActivity.this).getCurrentlyActiveGame();
            int inputSelection = 0;
            if (currentGame != null) {
                switch (currentGame.getMode()) {
                    case HIGH_SCORE:
                        inputSelection = 0;
                        break;
                    case LOW_SCORE:
                        inputSelection = 1;
                        break;
                }
            }

            final CharSequence[] items = {getString(R.string.switch_mode_highscore), getString(R.string.switch_mode_lowscore)};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.switch_mode_title);

            builder.setSingleChoiceItems(items, inputSelection,
                    (dialogInterface, selectedItem) -> {
                        if (currentGame == null)
                            return;

                        if (selectedItem == 0)
                            currentGame.setMode(GameManager.GameMode.HIGH_SCORE);
                        else if (selectedItem == 1)
                            currentGame.setMode(GameManager.GameMode.LOW_SCORE);
                        dialogInterface.dismiss();
                        renderSumRow();
                    });
            Dialog levelDialog = builder.create();
            levelDialog.show();

            return true;
        } else if (id == R.id.action_save_game) {
            GameManager.Game game = GameManager.getInstance(MainActivity.this).getCurrentlyActiveGame();
            String currentName = null;
            if (game != null && !game.getName().equals(""))
                currentName = game.getName();

            createStringPrompt(this, R.string.save_game_dialog_title, R.string.dialog_ok, R.string.dialog_cancel, currentName, getString(R.string.save_game_dialog_hint), new StringPromptResultHandler() {
                @Override
                public void onOk(String result) {
                    if (game == null) {
                        Toast.makeText(MainActivity.this, R.string.no_game_active_toast, Toast.LENGTH_LONG).show();
                        return;
                    }

                    game.setName(result);
                }

                @Override
                public void onCancel() {
                }
            });
            return true;
        } else if (id == R.id.action_load_game) {

        } else if (id == R.id.action_manage_saved_games) {

        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_imprint) {
            // TODO: Imprint
        } else if (id == R.id.nav_website) {
            startURLIntent(AppConfig.getInstance().getWebsiteURL());
        } else if (id == R.id.nav_instagram) {
            startURLIntent(AppConfig.getInstance().getInstagramURL());
        } else if (id == R.id.nav_share) {
            // TODO: Share
        } else if (id == R.id.nav_github) {
            startURLIntent(AppConfig.getInstance().getGithubURL());
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void startURLIntent(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    public RecyclerView getMainTable() {
        return mainTable;
    }

    public LinearLayout getHeaderRow() {
        return findViewById(R.id.header_row);
    }

    public GameTableViewHolder getHeaderRowViewHolder() {
        return new GameTableViewHolder(getHeaderRow(), false);
    }

    public void headerRowUpdateLineNumber() {
        GameManager.Game game = GameManager.getInstance(this).getCurrentlyActiveGame();
        if (game == null) return;
        GameTableViewHolder viewHolder = getHeaderRowViewHolder();
        viewHolder.setLineNumber(game.getScoreCount());
    }

    public void sumRowUpdateLineNumber() {
        GameManager.Game game = GameManager.getInstance(this).getCurrentlyActiveGame();
        if (game == null) return;
        GameTableViewHolderWithPlaceholders viewHolder = getSumRowViewHolder();
        viewHolder.setLineNumber(game.getScoreCount());
    }

    public void updateLineNumberWidth() {
        headerRowUpdateLineNumber();
        sumRowUpdateLineNumber();
        getHeaderRowViewHolder().getLineNumberTextView().requestLayout();
        getSumRowViewHolder().getLineNumberTextView().requestLayout();
    }

    private void renderHeaderRow() {
        GameManager.Game game = GameManager.getInstance(this).getCurrentlyActiveGame();
        GameTableViewHolder viewHolder = getHeaderRowViewHolder();
        headerRowUpdateLineNumber();
        viewHolder.getLineNumberTextView().setVisibility(View.INVISIBLE);
        viewHolder.getDeleteRowButton().setVisibility(View.INVISIBLE);

        if (game == null) {
            viewHolder.getScoreHolderLayout().removeAllViews();
            return;
        }

        final List<GameManager.Game.Player> players = game.getPlayers();
        viewHolder.getScoreHolderLayout().removeAllViews();

        for (int i = 0; i < players.size(); i++) {
            final GameManager.Game.Player player = players.get(i);
            final EditText editText = new EditText(viewHolder.getView().getContext());

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            editText.setLayoutParams(layoutParams);

            editText.setInputType(EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE | EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES);
            editText.setHorizontallyScrolling(false);
            editText.setMaxLines(AppConfig.getInstance().getMaxLinesForEnterText());
            editText.setText(player.getName());
            final int finalI = i;
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        players.get(finalI).setName(s.toString());
                    } catch (NumberFormatException e) {
                        Toast.makeText(viewHolder.getView().getContext(), R.string.max_input_length_reached_toast, Toast.LENGTH_LONG).show();
                        s.delete(s.length() - 1, s.length());
                    }
                }
            });

            viewHolder.getScoreHolderLayout().addView(editText);
        }
    }

    public LinearLayout getSumRow() {
        return findViewById(R.id.sum_row);
    }

    public GameTableViewHolderWithPlaceholders getSumRowViewHolder() {
        return new GameTableViewHolderWithPlaceholders(getSumRow());
    }

    public void renderSumRow() {
        GameManager.Game game = GameManager.getInstance(this).getCurrentlyActiveGame();
        GameTableViewHolderWithPlaceholders viewHolder = getSumRowViewHolder();
        viewHolder.getLineNumberTextView().setVisibility(View.INVISIBLE);

        if (game == null) {
            viewHolder.getScoreHolderLayout().removeAllViews();
            return;
        }

        final List<GameManager.Game.Player> players = game.getPlayers();
        List<Integer> winnerIndices = game.getWinners();
        List<Integer> looserIndices = game.getLoosers();
        viewHolder.getScoreHolderLayout().removeAllViews();

        for (int i = 0; i < players.size(); i++) {
            GameManager.Game.Player player = players.get(i);
            final TextView textView = new TextView(viewHolder.getView().getContext());
            long sum = player.getTotalScore();
            textView.setText(String.format(textView.getContext().getString(R.string.main_table_score_template), sum));
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(0, 15, 0, 15);

            if (winnerIndices.contains(i))
                textView.setBackgroundColor(getResources().getColor(R.color.winnerColor));
            else if (looserIndices.contains(i))
                textView.setBackgroundColor(getResources().getColor(R.color.looserColor));

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
            textView.setLayoutParams(layoutParams);

            viewHolder.getScoreHolderLayout().addView(textView);
        }
    }

    public interface StringPromptResultHandler {
        void onOk(String result);

        void onCancel();
    }
}
