package com.github.vatbub.scoreboard;

import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameTableRecyclerViewAdapter extends RecyclerView.Adapter<GameTableViewHolder> {
    private GameManager.Game game;
    private RecyclerView parent;
    private Set<GameTableViewHolder> mBoundViewHolders = new HashSet<>();
    private int lastLineColumnWidth;
    private MainActivity mainActivity;

    public GameTableRecyclerViewAdapter(RecyclerView parent, GameManager.Game game, MainActivity mainActivity) {
        this.game = game;
        this.parent = parent;
        setMainActivity(mainActivity);
        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                updateLineNumbers();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                updateLineNumbers();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateLineNumbers();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateLineNumbers();
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                updateLineNumbers();
            }

            @Override
            public void onChanged() {
                updateLineNumbers();
            }
        });
    }

    public Set<GameTableViewHolder> getAllBoundViewHolders() {
        return Collections.unmodifiableSet(mBoundViewHolders);
    }

    @Override
    public GameTableViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.scoreboard_row, parent, false);
        return new GameTableViewHolder(rowView);
    }

    @Override
    public void onBindViewHolder(final GameTableViewHolder holder, int position) {
        mBoundViewHolders.add(holder);

        holder.setLineNumber(holder.getAdapterPosition() + 1);
        if (lastLineColumnWidth != 0)
            holder.getLineNumberTextView().setWidth(lastLineColumnWidth);

        holder.getDeleteRowButton().setOnClickListener(v -> {
            if (holder.getAdapterPosition() == RecyclerView.NO_POSITION) return;
            getGame().removeScoreLineAt(holder.getAdapterPosition());
            getMainActivity().updateLineNumberWidth();
            notifyItemRemoved(holder.getAdapterPosition());
        });

        final List<Long> scoreLine = getGame().getScoreLineAt(holder.getAdapterPosition());
        holder.getScoreHolderLayout().removeAllViews();

        for (int i = 0; i < scoreLine.size(); i++) {
            final long score = scoreLine.get(i);
            final EditText editText = new EditText(holder.getView().getContext());
            editText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
            editText.setText(String.format(editText.getContext().getString(R.string.main_table_score_template), score));
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
                        long newScore = 0;
                        if (s.length() > 0)
                            newScore = Long.valueOf(s.toString());

                        scoreLine.set(finalI, newScore);
                        getGame().modifyScoreLineAt(holder.getAdapterPosition(), scoreLine);
                    } catch (NumberFormatException e) {
                        Toast.makeText(holder.getView().getContext(), R.string.max_input_length_reached_toast, Toast.LENGTH_LONG).show();
                        s.delete(s.length() - 1, s.length());
                    }
                }
            });

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            editText.setLayoutParams(layoutParams);

            holder.getScoreHolderLayout().addView(editText);
        }
    }

    @Override
    public void onViewRecycled(GameTableViewHolder holder) {
        mBoundViewHolders.remove(holder);
    }

    public void updateLineNumbers() {
        for (int childCount = getParent().getChildCount(), i = 0; i < childCount; ++i) {
            GameTableViewHolder holder = (GameTableViewHolder) getParent().getChildViewHolder(getParent().getChildAt(i));
            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION)
                holder.setLineNumber(holder.getAdapterPosition() + 1);
        }
    }

    public void updateColumnWidths(int columnWidth) {
        lastLineColumnWidth = columnWidth;
        for (GameTableViewHolder holder : getAllBoundViewHolders()) {
            holder.getLineNumberTextView().setWidth(columnWidth);
            // holder.getLineNumberTextView().requestLayout();
        }
    }

    @Override
    public int getItemCount() {
        return getGame().getScoreCount();
    }

    public GameManager.Game getGame() {
        return game;
    }


    public RecyclerView getParent() {
        return parent;
    }

    public MainActivity getMainActivity() {
        return mainActivity;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }
}
