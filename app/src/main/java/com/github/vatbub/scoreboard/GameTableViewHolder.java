package com.github.vatbub.scoreboard;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GameTableViewHolder extends RecyclerView.ViewHolder {
    private View view;

    public GameTableViewHolder(View itemView) {
        super(itemView);
        view = itemView;
    }

    public View getView() {
        return view;
    }

    public LinearLayout getScoreHolderLayout() {
        return getView().findViewById(R.id.main_table_text_view_holder);
    }

    public TextView getLineNumberTextView() {
        return getView().findViewById(R.id.main_table_line_number);
    }

    public ImageButton getDeleteRowButton() {
        return getView().findViewById(R.id.main_table_delete_row_button);
    }

    public void setLineNumber(int lineNumber) {
        getLineNumberTextView().setText(String.format(getView().getContext().getString(R.string.main_table_row_number_template), lineNumber));
        getDeleteRowButton().setContentDescription(String.format(getView().getContext().getString(R.string.main_table_delete_button_content_description_template), lineNumber));
    }
}
