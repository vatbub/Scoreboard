package com.github.vatbub.scoreboard.view.viewModels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.vatbub.scoreboard.R

/**
 * A placeholder fragment containing a simple view.
 */
class HostFragment : Fragment() {

    private lateinit var hostViewModel: HostViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hostViewModel = ViewModelProviders.of(this).get(HostViewModel::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_host, container, false)
        // hostViewModel.text.observe(this, Observer<String> {
        // })
        return root
    }
}