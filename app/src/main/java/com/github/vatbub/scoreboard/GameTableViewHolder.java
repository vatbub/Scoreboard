package com.github.vatbub.scoreboard;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GameTableViewHolder extends RecyclerView.ViewHolder {
    private final LinearLayout scoreHolderLayout;
    private final TextView lineNumberTextView;
    private View view;
    private ImageButton deleteRowButton;

    public GameTableViewHolder(View itemView) {
        super(itemView);
        view = itemView;
        scoreHolderLayout = getView().findViewById(R.id.main_table_text_view_holder);
        lineNumberTextView = getView().findViewById(R.id.main_table_line_number);
        deleteRowButton = getView().findViewById(R.id.main_table_delete_row_button);
    }

    public View getView() {
        return view;
    }

    public LinearLayout getScoreHolderLayout() {
        return scoreHolderLayout;
    }

    public TextView getLineNumberTextView() {
        return lineNumberTextView;
    }

    public ImageButton getDeleteRowButton() {
        return deleteRowButton;
    }

    public void setLineNumber(int lineNumber) {
        getLineNumberTextView().setText(String.format(getView().getContext().getString(R.string.main_table_row_number_template), lineNumber));
        getDeleteRowButton().setContentDescription(String.format(getView().getContext().getString(R.string.main_table_delete_button_content_description_template), lineNumber));
    }
}
