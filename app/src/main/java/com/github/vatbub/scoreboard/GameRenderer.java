package com.github.vatbub.scoreboard;

import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
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
    private TableRow headerRow;
    private GameManager.Game gameToRender;
    private List<String> scoreInputTags;

    public GameRenderer(TableLayout renderingLayout, TableRow headerRow, GameManager.Game gameToRender) {
        setRenderingLayout(renderingLayout);
        setHeaderRow(headerRow);
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

    private void setTableCellProperties(View tableCell) {
        TableRow.LayoutParams layoutParams = (TableRow.LayoutParams) tableCell.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.weight = 1;

        if (tableCell instanceof EditableTextView)
            tableCell.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        tableCell.setLayoutParams(layoutParams);
    }

    private void renderHeaderRow(TableRow headerRow) {
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

            headerRow.addView(editableTextView);
            setTableCellProperties(editableTextView);
        }
    }

    public void redraw() {
        synchronized (this) {
            getRenderingLayout().removeAllViews();

            // render the actual header
            getHeaderRow().removeAllViews();
            renderHeaderRow(getHeaderRow());

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
                    setTableCellProperties(editableTextView);
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
                editText.setText("0");

                editText.setTag(scoreInputTag);
                scoreInputRow.addView(editText);
                setTableCellProperties(editText);
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

    public TableRow getHeaderRow() {
        return headerRow;
    }

    public void setHeaderRow(TableRow headerRow) {
        this.headerRow = headerRow;
    }
}
