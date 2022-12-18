package com.github.vatbub.scoreboard.util

import androidx.fragment.app.Fragment

interface FragmentCompanion<T : Fragment> {
    val titleId: Int
    fun newInstance(): T
}