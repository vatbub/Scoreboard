package com.github.vatbub.scoreboard;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

public class GameTableViewHolderWithPlaceholders extends RecyclerView.ViewHolder {
    private final LinearLayout scoreHolderLayout;
    private final TextView lineNumberTextView;
    private View view;
    private Space deleteRowButton;

    public GameTableViewHolderWithPlaceholders(View itemView) {
        super(itemView);
        view = itemView;
        scoreHolderLayout = getView().findViewById(R.id.main_table_text_view_holder);
        lineNumberTextView = getView().findViewById(R.id.main_table_line_number_placeholder);
        deleteRowButton = getView().findViewById(R.id.main_table_delete_row_button_placeholder);
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

    public Space getDeleteRowButton() {
        return deleteRowButton;
    }

    public void setLineNumber(int lineNumber) {
        getLineNumberTextView().setText(String.format(getView().getContext().getString(R.string.main_table_row_number_template), lineNumber));
    }

}
