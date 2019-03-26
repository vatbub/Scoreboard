package com.github.vatbub.scoreboard

import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.github.vatbub.scoreboard.data.Game
import java.util.*

class GameTableRecyclerViewAdapter(val parent: RecyclerView, val game: Game, val mainActivity: MainActivity) : RecyclerView.Adapter<GameTableViewHolder>() {
    private val mBoundViewHolders = HashSet<GameTableViewHolder>()
    private var lastLineColumnWidth: Int = 0

    val allBoundViewHolders: Set<GameTableViewHolder>
        get() = Collections.unmodifiableSet(mBoundViewHolders)

    init {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = updateLineNumbers()
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) = updateLineNumbers()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = updateLineNumbers()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = updateLineNumbers()
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = updateLineNumbers()
            override fun onChanged() = updateLineNumbers()
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameTableViewHolder {
        val rowView = LayoutInflater.from(parent.context).inflate(R.layout.scoreboard_row, parent, false)
        return GameTableViewHolder(rowView, true)
    }

    override fun onBindViewHolder(holder: GameTableViewHolder, position: Int) {
        mBoundViewHolders.add(holder)

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
    }

    override fun onViewRecycled(holder: GameTableViewHolder?) {
        mBoundViewHolders.remove(holder)
    }

    fun updateLineNumbers() {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val holder = parent.getChildViewHolder(parent.getChildAt(i)) as GameTableViewHolder
            if (holder.adapterPosition != RecyclerView.NO_POSITION)
                holder.setLineNumber(holder.adapterPosition + 1)
        }
    }

    fun updateColumnWidths(columnWidth: Int) {
        lastLineColumnWidth = columnWidth
        allBoundViewHolders.forEach { it.lineNumberTextView.width = columnWidth }
    }

    override fun getItemCount(): Int {
        return game.scoreCount
    }
}
