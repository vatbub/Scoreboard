package com.github.vatbub.scoreboard.view.viewModels

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager)
    : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private fun getFragmentCompanion(position: Int) = when (position) {
        0 -> HostFragment
        1 -> JoinFragment
        else -> throw IllegalArgumentException("Unknown fragment index $position")
    }

    override fun getItem(position: Int): Fragment = getFragmentCompanion(position).newInstance()

    override fun getPageTitle(position: Int): CharSequence? =
            context.resources.getString(getFragmentCompanion(position).titleId)

    override fun getCount() = 2
}