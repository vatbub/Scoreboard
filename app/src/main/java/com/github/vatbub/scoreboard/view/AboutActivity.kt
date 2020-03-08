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
import com.mikepenz.aboutlibraries.LibsBuilder
import kotlinx.android.synthetic.main.activity_about.*
import ru.noties.markwon.Markwon


class AboutActivity : AppCompatActivity() {
    private val textColor by lazy {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            resources.getColor(R.color.textColorPrimary, null)
        else
            resources.getColor(R.color.textColorPrimary)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setTitle(R.string.activity_about_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        displayMarkdown(R.raw.about)
        setButtonListenersUp()
        setVersionLabelUp()
    }

    private fun setVersionLabelUp() {
        activity_about_version_label.text = getString(R.string.version_label_template, BuildConfig.VERSION_NAME, BuildConfig.GitHash)
        activity_about_version_label.setTextColor(textColor)
    }

    private fun setButtonListenersUp() {
        imprint_button_open_source_licenses.setOnClickListener { showOpenSourceLicenses() }
    }

    private fun displayMarkdown(@RawRes markdownFile: Int) {
        activity_about_markdown_view.movementMethod = LinkMovementMethod.getInstance()
        Markwon.unscheduleDrawables(activity_about_markdown_view)
        Markwon.unscheduleTableRows(activity_about_markdown_view)

        activity_about_markdown_view.text = MarkdownRenderer[this].getCachedRenderResult(markdownFile)

        activity_about_markdown_view.setTextColor(textColor)

        Markwon.scheduleDrawables(activity_about_markdown_view)
        Markwon.scheduleTableRows(activity_about_markdown_view)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showOpenSourceLicenses() {
        LibsBuilder().start(this)
    }
}
