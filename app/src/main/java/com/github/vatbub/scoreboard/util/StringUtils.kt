package com.github.vatbub.scoreboard.util

import kotlin.random.Random

fun getRandomHexString(allowNegativeNumbers: Boolean = false): String {
    var number = Random.nextInt()
    if (!allowNegativeNumbers && number < 0)
        number = -number
    return number.toString(16)
}