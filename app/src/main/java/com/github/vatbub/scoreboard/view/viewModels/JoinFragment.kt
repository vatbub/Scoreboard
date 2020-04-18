package com.github.vatbub.scoreboard.view.viewModels

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.vatbub.scoreboard.R


class JoinFragment : Fragment() {

    companion object {
        fun newInstance() = JoinFragment()
    }

    private lateinit var viewModel: JoinViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_join, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(JoinViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
