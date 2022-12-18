package com.github.vatbub.scoreboard.util

fun <T>List<T>?.notNullAndContains(element:T) =
        this!=null && this.contains(element)