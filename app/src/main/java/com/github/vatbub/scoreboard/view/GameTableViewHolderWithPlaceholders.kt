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

import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.vatbub.scoreboard.R

class GameTableViewHolderWithPlaceholders(val view: View) : RecyclerView.ViewHolder(view) {
    val scoreHolderLayout = view.findViewById<LinearLayout>(R.id.main_table_text_view_holder)!!
    val lineNumberTextView = view.findViewById<TextView>(R.id.main_table_line_number_placeholder)!!
    val deleteRowButton = view.findViewById<Space>(R.id.main_table_delete_row_button_placeholder)!!

    fun setLineNumber(lineNumber: Int) {
        lineNumberTextView.text =
            view.context.getString(R.string.main_table_row_number_template, lineNumber)
    }

}
