package com.github.vatbub.scoreboard;

import android.text.InputType;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link GameManager.Game} in a {@link android.widget.TableLayout}
 */

public class GameRenderer implements GameManager.OnRedrawListener {
    private TableLayout renderingLayout;
    private GameManager.Game gameToRender;
    private List<String> scoreInputTags;

    public GameRenderer(TableLayout renderingLayout, GameManager.Game gameToRender) {
        setRenderingLayout(renderingLayout);
        setGameToRender(gameToRender);
    }

    public TableLayout getRenderingLayout() {
        return renderingLayout;
    }

    public void setRenderingLayout(TableLayout renderingLayout) {
        this.renderingLayout = renderingLayout;
    }

    public GameManager.Game getGameToRender() {
        return gameToRender;
    }

    public void setGameToRender(GameManager.Game gameToRender) {
        // remove listener from old game
        if (getGameToRender() != null)
            getGameToRender().getOnRedrawListeners().remove(this);

        this.gameToRender = gameToRender;
        getGameToRender().getOnRedrawListeners().add(this);
        redraw();
    }

    public void redraw() {
        synchronized (this) {
            getRenderingLayout().removeAllViews();

            // Add the player names
            TableRow namesRow = new TableRow(getRenderingLayout().getContext());

            for (final GameManager.Game.Player player : getGameToRender().getPlayers()) {
                EditableTextView editableTextView = new EditableTextView(getRenderingLayout().getContext());
                editableTextView.setText(player.getName());

                editableTextView.setEditAlertTitle(getRenderingLayout().getContext().getString(R.string.edit_player_name));
                editableTextView.setOkButtonCaption(getRenderingLayout().getContext().getString(R.string.dialog_ok));
                editableTextView.setCancelButtonCaption(getRenderingLayout().getContext().getString(R.string.dialog_cancel));

                editableTextView.setOnChangedCallback(new EditableTextView.OnChangedCallback() {
                    @Override
                    public void changed(String oldText, String newText) {
                        player.setName(newText);
                    }
                });

                namesRow.addView(editableTextView);
            }

            getRenderingLayout().addView(namesRow);

            // render scores
            for (int scoreLineIndex = 0; scoreLineIndex < getGameToRender().getScoreCount(); scoreLineIndex++) {
                TableRow scoreRow = new TableRow(getRenderingLayout().getContext());
                final List<Integer> scoreLine = getGameToRender().getScoreLineAt(scoreLineIndex);

                for (int playerIndex = 0; playerIndex < scoreLine.size(); playerIndex++) {
                    EditableTextView editableTextView = new EditableTextView(getRenderingLayout().getContext());
                    editableTextView.setText(Integer.toString(scoreLine.get(playerIndex)));

                    editableTextView.setEditAlertTitle(getRenderingLayout().getContext().getString(R.string.edit_score));
                    editableTextView.setOkButtonCaption(getRenderingLayout().getContext().getString(R.string.dialog_ok));
                    editableTextView.setCancelButtonCaption(getRenderingLayout().getContext().getString(R.string.dialog_cancel));

                    final int finalPlayerIndex = playerIndex;
                    final int finalScoreLineIndex = scoreLineIndex;
                    editableTextView.setOnChangedCallback(new EditableTextView.OnChangedCallback() {
                        @Override
                        public void changed(String oldText, String newText) {
                            scoreLine.set(finalPlayerIndex, Integer.parseInt(newText));
                            getGameToRender().modifyScoreLineAt(finalScoreLineIndex, scoreLine);
                        }
                    });

                    scoreRow.addView(editableTextView);
                }

                getRenderingLayout().addView(scoreRow);
            }

            // render row to add scores
            TableRow scoreInputRow = new TableRow(getRenderingLayout().getContext());
            scoreInputTags = new ArrayList<>(getGameToRender().getScoreCount());
            for (int playerIndex = 0; playerIndex < getGameToRender().getPlayers().size(); playerIndex++) {
                String scoreInputTag = "scoreInput_" + playerIndex;
                scoreInputTags.add(scoreInputTag);
                EditText editText = new EditText(getRenderingLayout().getContext());
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);

                editText.setTag(scoreInputTag);
                scoreInputRow.addView(editText);
            }

            getRenderingLayout().addView(scoreInputRow);
        }
    }

    public void applyNewScore() {
        List<Integer> scores = new ArrayList<>(scoreInputTags.size());
        for (String inputTag : scoreInputTags) {
            EditText editText = getRenderingLayout().findViewWithTag(inputTag);
            scores.add(Integer.parseInt(editText.getText().toString()));
        }
        getGameToRender().addScoreLine(scores);
    }

    @Override
    public void onChangeApplied(GameManager.Game changedGame) {
        redraw();
    }
}
