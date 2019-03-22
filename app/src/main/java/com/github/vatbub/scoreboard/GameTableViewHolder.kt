package com.github.vatbub.scoreboard

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.scoreboard_row.view.*

class GameTableViewHolder(val view: View, var shouldLineColorBeSet: Boolean = false) : RecyclerView.ViewHolder(view) {
    val scoreHolderLayout = view.main_table_text_view_holder!!
    val lineNumberTextView = view.main_table_line_number!!
    val deleteRowButton = view.main_table_delete_row_button!!

    fun setLineNumber(lineNumber: Int) {
        lineNumberTextView.text = String.format(view.context.getString(R.string.main_table_row_number_template), lineNumber)
        deleteRowButton.contentDescription = String.format(view.context.getString(R.string.main_table_delete_button_content_description_template), lineNumber)

        if (!shouldLineColorBeSet) return

        @Suppress("DEPRECATION")
        if (isOdd(lineNumber))
            view.setBackgroundColor(view.resources.getColor(R.color.oddLineColor))
        else
            view.setBackgroundColor(view.resources.getColor(R.color.evenLineColor))

    }

    private fun isOdd(i: Int) = i % 2 == 1
}
