package com.github.vatbub.scoreboard.util

inline fun <TIn, reified TOut> Array<out TIn>.transform(action: (TIn) -> TOut): Array<out TOut> =
    Array(this.size) { action(this[it]) }

inline fun <TIn, reified TOut> List<TIn>.transform(action: (TIn) -> TOut): List<TOut> =
    List(this.size) { action(this[it]) }
