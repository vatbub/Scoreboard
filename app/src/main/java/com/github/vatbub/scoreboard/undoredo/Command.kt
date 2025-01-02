package com.github.vatbub.scoreboard.undoredo

import android.os.Parcelable

interface Command : Parcelable
interface UndoableCommand : Command