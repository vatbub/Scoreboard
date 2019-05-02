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

    private inner class CustomTextWatcher(val index: Int, val holder: GameTableViewHolder, val scoreLine: MutableList<Long>) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(editable: Editable) {
            try {
                var newScore: Long = 0
                if (editable.isNotEmpty())
                    newScore = editable.toString().toLong()

                scoreLine[index] = newScore
                game.modifyScoreLineAt(holder.adapterPosition, scoreLine)
                renderSubTotals()
                mainActivity.redraw(refreshGameData = false, redrawHeaderRow = false, notifyDataSetChanged = false, redrawSumRow = true, redrawLeaderBoard = true, updateFabButtonHint = true)
            } catch (e: NumberFormatException) {
                Toast.makeText(holder.view.context, R.string.max_input_length_reached_toast, Toast.LENGTH_LONG).show()
                editable.delete(editable.length - 1, editable.length)
            }
        }
    }

    override fun onBindViewHolder(holder: GameTableViewHolder, position: Int) {
        mBoundViewHolders.add(holder)

        holder.subTotalRow.visibility = targetSubTotalVisibility(showSubTotal)

        holder.lineNumber = holder.adapterPosition + 1
        if (lastLineColumnWidth != 0)
            holder.lineNumberTextView.width = lastLineColumnWidth

        holder.deleteRowButton.setOnClickListener {
            if (holder.adapterPosition == NO_POSITION)
                return@setOnClickListener
            game.removeScoreLineAt(holder.adapterPosition)
            mainActivity.updateLineNumberWidth()
            mainActivity.redraw(refreshGameData = false, redrawHeaderRow = false, notifyDataSetChanged = false, redrawSumRow = true, redrawLeaderBoard = true, updateFabButtonHint = true)
            notifyItemRemoved(holder.adapterPosition)
        }

        val scoreLine = game.getScoreLineAt(holder.adapterPosition)
        val scoreLineCopy = scoreLine.toMutableList()
        holder.scoreHolderLayout.removeAllViews()

        scoreLine.forEachIndexed { index, score ->
            with(EditText(holder.view.context)) {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                inputType = EditorInfo.TYPE_CLASS_NUMBER
                hint = context.getString(R.string.main_table_score_template, 0)
                setHorizontallyScrolling(false)
                maxLines = resources.getInteger(R.integer.max_lines_for_enter_text)
                if (score != 0L) setText(context.getString(R.string.main_table_score_template, score))
                addTextChangedListener(CustomTextWatcher(index, holder, scoreLineCopy))
                holder.scoreHolderLayout.addView(this)
            }
        }
        renderSubTotals(holder)
    }

    override fun onViewRecycled(holder: GameTableViewHolder) {
        mBoundViewHolders.remove(holder)
    }

    private fun onDataChanged() {
        updateLineNumbers()
        renderSubTotals()
        mainActivity.redraw(refreshGameData = false, redrawHeaderRow = false, notifyDataSetChanged = false, redrawSumRow = true, redrawLeaderBoard = true, updateFabButtonHint = true)
    }

    private fun updateLineNumbers() {
        for (i in 0 until parent.childCount) {
            val holder = parent.getChildViewHolder(parent.getChildAt(i)) as GameTableViewHolder
            if (holder.adapterPosition != NO_POSITION)
                holder.lineNumber = holder.adapterPosition + 1
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
            with(TextView(holder.view.context)) {
                val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams.marginStart = 2.toPx(holder.view.context)
                this.layoutParams = layoutParams
                setHorizontallyScrolling(false)
                maxLines = context.resources.getInteger(R.integer.max_lines_for_enter_text)
                text = context.getString(R.string.main_table_score_template, subTotal)
                holder.subTotalHolderLayout.addView(this)
            }
        }
    }

    override fun getItemCount() = game.scoreCount
}
