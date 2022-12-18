package com.github.vatbub.scoreboard.view.viewModels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.vatbub.scoreboard.R
import com.github.vatbub.scoreboard.util.FragmentCompanion


class JoinFragment : Fragment() {

    companion object : FragmentCompanion<JoinFragment> {
        override val titleId = R.string.tab_join_title
        override fun newInstance() = JoinFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_join, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

}
