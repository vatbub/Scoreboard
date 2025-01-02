package com.github.vatbub.scoreboard.undoredo

import android.content.Context
import com.github.vatbub.scoreboard.data.ObservableMutableList

class UndoRedoManager private constructor(private val callingContext: Context) {
    companion object {
        private val instances = mutableMapOf<Context, UndoRedoManager>()

        operator fun get(callingContext: Context): UndoRedoManager = synchronized(instances) {
            instances.getOrPut(callingContext) { UndoRedoManager(callingContext) }
        }

        fun resetInstance(context: Context) {
            synchronized(instances) {
                instances.remove(context)
            }
        }
    }

    private object SharedPrefKeys {
        private val undoRedoManagerTitle = "UndoRedoManager"
    }

    private val _undoStack = ObservableMutableList(restoreUndoStack(),
            { _, _ -> saveUndoStack() },
            { _, _, _ -> saveUndoStack() },
            { _, _ -> saveUndoStack() },
            { saveUndoStack() })

    private val _redoStack = ObservableMutableList(restoreRedoStack(),
            { _, _ -> saveRedoStack() },
            { _, _, _ -> saveRedoStack() },
            { _, _ -> saveRedoStack() },
            { saveRedoStack() })

    private fun saveUndoStack(){

    }
}