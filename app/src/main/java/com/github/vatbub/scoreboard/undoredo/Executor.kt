package com.github.vatbub.scoreboard.undoredo

interface Executor<T : Command> {
    fun execute(command: T)
}

interface UndoableExecutor<T : UndoableCommand> : Executor<T> {
    fun unexecute(command: T)
}