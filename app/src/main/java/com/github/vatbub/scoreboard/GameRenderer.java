package com.github.vatbub.scoreboard;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

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

    private void setTableCellProperties(View tableCell, boolean isDummyRow) {
        TableRow.LayoutParams layoutParams = (TableRow.LayoutParams) tableCell.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.weight = 1;
        if (isDummyRow)
            layoutParams.height = 0;

        if (tableCell instanceof EditableTextView)
            tableCell.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        tableCell.setLayoutParams(layoutParams);
    }

    private void renderHeaderRow(TableRow headerRow, boolean isDummyRow) {
        for (final GameManager.Game.Player player : getGameToRender().getPlayers()) {
            EditText editText = new EditText(getRenderingLayout().getContext());
            editText.setText(player.getName());


            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    player.setName(editable.toString());
                }
            });

            headerRow.addView(editText);
            setTableCellProperties(editText, isDummyRow);
        }
    }

    public void redraw() {
        synchronized (this) {
            getRenderingLayout().removeAllViews();

            // render the actual header
            getHeaderRow().removeAllViews();
            renderHeaderRow(getHeaderRow(), false);

            // render dummy row
            TableRow dummyHeaderRow = new TableRow(getRenderingLayout().getContext());
            renderHeaderRow(dummyHeaderRow, true);
            getRenderingLayout().addView(dummyHeaderRow);

            // render scores
            for (int scoreLineIndex = 0; scoreLineIndex < getGameToRender().getScoreCount(); scoreLineIndex++) {
                TableRow scoreRow = new TableRow(getRenderingLayout().getContext());
                final List<Integer> scoreLine = getGameToRender().getScoreLineAt(scoreLineIndex);

                for (int playerIndex = 0; playerIndex < scoreLine.size(); playerIndex++) {
                    EditText editText = new EditText(getRenderingLayout().getContext());
                    editText.setText(Integer.toString(scoreLine.get(playerIndex)));
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);

                    final int finalPlayerIndex = playerIndex;
                    final int finalScoreLineIndex = scoreLineIndex;
                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            try {
                                scoreLine.set(finalPlayerIndex, Integer.parseInt(editable.toString()));
                                getGameToRender().modifyScoreLineAt(finalScoreLineIndex, scoreLine);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    scoreRow.addView(editText);
                    setTableCellProperties(editText, false);
                }

                getRenderingLayout().addView(scoreRow);
            }
        }
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
