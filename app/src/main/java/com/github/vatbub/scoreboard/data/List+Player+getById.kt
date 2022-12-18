package com.github.vatbub.scoreboard.data


fun List<Player>.getById(id: Int): Player = this.first { it.id == id }