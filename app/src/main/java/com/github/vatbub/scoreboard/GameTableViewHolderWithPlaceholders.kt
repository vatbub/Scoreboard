package com.github.vatbub.scoreboard

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.sum_row_layout.view.*

class GameTableViewHolderWithPlaceholders(val view: View) : RecyclerView.ViewHolder(view) {
    val scoreHolderLayout = view.main_table_text_view_holder!!
    val lineNumberTextView = view.main_table_line_number_placeholder!!
    val deleteRowButton = view.main_table_delete_row_button_placeholder!!

    fun setLineNumber(lineNumber: Int) {
        lineNumberTextView.text = view.context.getString(R.string.main_table_row_number_template, lineNumber)
    }

}
