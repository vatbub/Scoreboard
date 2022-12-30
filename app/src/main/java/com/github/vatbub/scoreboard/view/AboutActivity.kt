/*
 * Copyright (c) 2019 Frederik Kammel <vatbub123@googlemail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.vatbub.scoreboard.view

import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import com.github.vatbub.scoreboard.BuildConfig
import com.github.vatbub.scoreboard.R
import com.github.vatbub.scoreboard.databinding.ActivityAboutBinding
import com.mikepenz.aboutlibraries.LibsBuilder
import ru.noties.markwon.Markwon


class AboutActivity : AppCompatActivity() {
    private lateinit var bindings: ActivityAboutBinding

    private val textColor by lazy {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            resources.getColor(R.color.textColorPrimary, null)
        else
            resources.getColor(R.color.textColorPrimary)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(bindings.root)
        setSupportActionBar(bindings.aboutToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        displayMarkdown(R.raw.about)
        setButtonListenersUp()
        setVersionLabelUp()
    }

    private fun setVersionLabelUp() {
        bindings.activityAboutVersionLabel.text = getString(
            R.string.version_label_template,
            BuildConfig.VERSION_NAME,
            BuildConfig.GitHash
        )
        bindings.activityAboutVersionLabel.setTextColor(textColor)
    }

    private fun setButtonListenersUp() {
        bindings.imprintButtonOpenSourceLicenses.setOnClickListener { showOpenSourceLicenses() }
    }

    private fun displayMarkdown(@RawRes markdownFile: Int) {
        bindings.activityAboutMarkdownView.movementMethod = LinkMovementMethod.getInstance()
        Markwon.unscheduleDrawables(bindings.activityAboutMarkdownView)
        Markwon.unscheduleTableRows(bindings.activityAboutMarkdownView)

        bindings.activityAboutMarkdownView.text =
            MarkdownRenderer[this].getCachedRenderResult(markdownFile)

        bindings.activityAboutMarkdownView.setTextColor(textColor)

        Markwon.scheduleDrawables(bindings.activityAboutMarkdownView)
        Markwon.scheduleTableRows(bindings.activityAboutMarkdownView)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showOpenSourceLicenses() {
        LibsBuilder().start(this)
    }
}
