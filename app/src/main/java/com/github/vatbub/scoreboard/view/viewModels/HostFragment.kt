package com.github.vatbub.scoreboard.view.viewModels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.vatbub.scoreboard.R
import com.github.vatbub.scoreboard.data.Game
import com.github.vatbub.scoreboard.data.GameManager
import com.github.vatbub.scoreboard.network.startSharing
import com.github.vatbub.scoreboard.util.FragmentCompanion
import kotlinx.android.synthetic.main.fragment_host.*

/**
 * A placeholder fragment containing a simple view.
 */
class HostFragment : Fragment() {
    companion object : FragmentCompanion<HostFragment> {
        override val titleId = R.string.tab_host_title
        override fun newInstance() = HostFragment()
    }

    private val loadingGames = mutableListOf<Game>()
    private val selectedGame: Game?
        get() = sync_host_spinner_game_selection.selectedItem as Game?

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_host, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setGameListUp()
        updateStartStopSharingButton()
        setButtonHandlersUp()
    }

    private fun setGameListUp() {
        sync_host_spinner_game_selection.adapter = ArrayAdapter(this.context!!, android.R.layout.simple_spinner_dropdown_item, GameManager[this.context!!].games)
        sync_host_spinner_game_selection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                updateView()
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateView()
            }
        }
    }

    private fun updateView() {
        updateStartStopSharingButton()
        updateGameIdView()
        updateLoadingIndicator()
    }

    private fun updateStartStopSharingButton() {
        val selectedGame = selectedGame
        val buttonText =
                when {
                    selectedGame == null -> this.getString(R.string.sync_host_button_start_sharing_select_game)
                    selectedGame.isShared -> this.getString(R.string.sync_host_button_stop_sharing)
                    else -> this.getString(R.string.sync_host_button_start_sharing)
                }
        sync_host_button_start_stop_sharing.text = buttonText
        sync_host_button_start_stop_sharing.isEnabled = selectedGame != null && !loadingGames.contains(selectedGame)
    }

    private fun setGameIdViewVisibility(visibility: Int) {
        sync_host_label_game_id.visibility = visibility
        sync_host_layout_game_id.visibility = visibility
    }

    private fun updateGameIdView() {
        val selectedGame = selectedGame
        if (selectedGame == null) {
            setGameIdViewVisibility(View.GONE)
            return
        }
        val sharedGameId = selectedGame.sharedGameId
        if (sharedGameId == null) {
            setGameIdViewVisibility(View.GONE)
            return
        }
        setGameIdViewVisibility(View.VISIBLE)
        edit_text_game_id.setText(sharedGameId)
    }

    private fun updateLoadingIndicator(){
        val selectedGame = selectedGame
        sync_host_progress_bar.visibility =
                if (selectedGame == null || !loadingGames.contains(selectedGame))
                    View.GONE
                else
                    View.VISIBLE
    }

    private fun setButtonHandlersUp() {
        sync_host_button_start_stop_sharing.setOnClickListener {
            val selectedGame = selectedGame
            if (selectedGame == null) {
                Toast.makeText(context!!, R.string.sync_host_toast_no_game_selected, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            loadingGames.add(selectedGame)
            updateView()
            selectedGame.startSharing(this.context!!) {
                loadingGames.remove(selectedGame)
                updateView()
            }
        }
        sync_host_button_share_game_id.setOnClickListener {
            // TODO
        }
    }
}