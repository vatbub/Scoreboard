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

package com.github.vatbub.scoreboard

import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.EditText

/**
 * Created by frede on 28.02.2018.
 */

class EditableTextView constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.textViewStyle) : android.support.v7.widget.AppCompatTextView(context, attrs, defStyleAttr) {
    var onChangedCallback: OnChangedCallback? = null
    var editAlertTitle: String? = null
    var okButtonCaption: String? = null
    var cancelButtonCaption: String? = null

    init {
        addOnClickListener()
    }

    private fun addOnClickListener() {
        internalSetOnLongClickListener(OnLongClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(editAlertTitle)

            // Set up the input
            val input = EditText(context)
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            // Set up the buttons
            builder.setPositiveButton(okButtonCaption) { _, _ ->
                val oldText = this@EditableTextView.text as String
                val newText = input.text.toString()
                this@EditableTextView.text = newText

                if (onChangedCallback != null)
                    onChangedCallback!!.changed(oldText, newText)
            }
            builder.setNegativeButton(cancelButtonCaption) { dialog, _ -> dialog.cancel() }

            builder.show()

            true
        })
    }

    override fun setOnTouchListener(onTouchListener: View.OnTouchListener?) {
        throw IllegalStateException("Custom on click listeners are not allowed")
    }


    private fun internalSetOnLongClickListener(l: View.OnLongClickListener?) {
        super.setOnLongClickListener(l)
    }

    interface OnChangedCallback {
        fun changed(oldText: String, newText: String)
    }
}
