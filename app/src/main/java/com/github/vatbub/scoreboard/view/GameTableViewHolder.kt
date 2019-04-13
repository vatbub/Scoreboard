/*
 * Copyright (c) 2019 Frederik Kammel <vatbub123@googlemail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.vatbub.scoreboard.view

import android.support.v7.widget.RecyclerView
import android.view.View
import com.github.vatbub.scoreboard.R
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
