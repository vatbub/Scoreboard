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
import android.support.v7.widget.RecyclerView.NO_POSITION
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.github.vatbub.scoreboard.AppConfig
import com.github.vatbub.scoreboard.R
import com.github.vatbub.scoreboard.data.Game
import com.github.vatbub.scoreboard.util.toPx
import java.util.*
import kotlin.properties.Delegates

class GameTableRecyclerViewAdapter(private val parent: RecyclerView, val game: Game, val mainActivity: MainActivity, showSubTotal: Boolean, onShowSubTotalChange: ((Boolean) -> Unit)? = null) : RecyclerView.Adapter<GameTableViewHolder>() {
    private val mBoundViewHolders = HashSet<GameTableViewHolder>()
    private var lastLineColumnWidth: Int = 0
    var showSubTotal by Delegates.observable(showSubTotal) { _, _, newValue ->
        mBoundViewHolders.forEach {
            it.subTotalRow.visibility = targetSubTotalVisibility(newValue)
            onShowSubTotalChange?.invoke(newValue)
        }
    }

    init {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = onDataChanged()
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) = onDataChanged()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = onDataChanged()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = onDataChanged()
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = onDataChanged()
            override fun onChanged() = onDataChanged()
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameTableViewHolder {
        val rowView = LayoutInflater.from(parent.context).inflate(R.layout.scoreboard_row, parent, false)
        return GameTableViewHolder(rowView, true)
    }

    override fun onBindViewHolder(holder: GameTableViewHolder, position: Int) {
        mBoundViewHolders.add(holder)

        holder.subTotalRow.visibility = targetSubTotalVisibility(showSubTotal)

        holder.setLineNumber(holder.adapterPosition + 1)
        if (lastLineColumnWidth != 0)
            holder.lineNumberTextView.width = lastLineColumnWidth

        holder.deleteRowButton.setOnClickListener {
            if (holder.adapterPosition == RecyclerView.NO_POSITION)
                return@setOnClickListener
            game.removeScoreLineAt(holder.adapterPosition)
            mainActivity.updateLineNumberWidth()
            mainActivity.renderSumRow()
            mainActivity.renderLeaderboard()
            notifyItemRemoved(holder.adapterPosition)
        }

        val scoreLine = game.getScoreLineAt(holder.adapterPosition)
        val scoreLineCopy = scoreLine.toMutableList()
        holder.scoreHolderLayout.removeAllViews()

        scoreLine.forEachIndexed { index, score ->
            val editText = EditText(holder.view.context)
            editText.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            editText.inputType = EditorInfo.TYPE_CLASS_NUMBER
            editText.hint = editText.context.getString(R.string.main_table_score_template, 0)
            editText.setHorizontallyScrolling(false)
            editText.maxLines = AppConfig.maxLinesForEnterText
            val scoreString = editText.context.getString(R.string.main_table_score_template, score)
            if (score != 0L) editText.setText(scoreString)

            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    try {
                        var newScore: Long = 0
                        if (editable.isNotEmpty())
                            newScore = editable.toString().toLong()

                        scoreLineCopy[index] = newScore
                        game.modifyScoreLineAt(holder.adapterPosition, scoreLineCopy)
                        renderSubTotals()
                        mainActivity.renderSumRow()
                        mainActivity.renderLeaderboard()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(holder.view.context, R.string.max_input_length_reached_toast, Toast.LENGTH_LONG).show()
                        editable.delete(editable.length - 1, editable.length)
                    }

                }
            })

            holder.scoreHolderLayout.addView(editText)
        }

        renderSubTotals(holder)
    }

    override fun onViewRecycled(holder: GameTableViewHolder) {
        mBoundViewHolders.remove(holder)
    }

    private fun onDataChanged() {
        updateLineNumbers()
        renderSubTotals()
    }

    private fun updateLineNumbers() {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val holder = parent.getChildViewHolder(parent.getChildAt(i)) as GameTableViewHolder
            if (holder.adapterPosition != RecyclerView.NO_POSITION)
                holder.setLineNumber(holder.adapterPosition + 1)
        }
    }

    fun updateColumnWidths(columnWidth: Int) {
        lastLineColumnWidth = columnWidth
        mBoundViewHolders.forEach { it.lineNumberTextView.width = columnWidth }
    }

    private fun targetSubTotalVisibility(showSubTotal: Boolean) = if (showSubTotal) View.VISIBLE else View.GONE

    private fun renderSubTotals() = mBoundViewHolders.forEach { renderSubTotals(it) }

    private fun renderSubTotals(holder: GameTableViewHolder) {
        if (holder.adapterPosition == NO_POSITION) return
        val subTotals = List(game.players.size) { index -> game.players[index].getSubTotalAt(holder.adapterPosition) }
        holder.subTotalHolderLayout.removeAllViews()

        subTotals.forEach { subTotal ->
            val textView = TextView(holder.view.context)
            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams.marginStart = 2.toPx(holder.view.context)
            textView.layoutParams = layoutParams
            textView.setHorizontallyScrolling(false)
            textView.maxLines = AppConfig.maxLinesForEnterText
            val scoreString = textView.context.getString(R.string.main_table_score_template, subTotal)
            textView.text = scoreString

            holder.subTotalHolderLayout.addView(textView)
        }
    }

    override fun getItemCount(): Int {
        return game.scoreCount
    }
}
