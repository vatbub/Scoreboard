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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.RawRes
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import com.github.vatbub.scoreboard.R
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import kotlinx.android.synthetic.main.activity_about.*
import ru.noties.markwon.Markwon


class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        displayMarkdown(R.raw.about)
        setButtonListenersUp()
    }

    private fun setButtonListenersUp() {
        imprint_button_open_source_licenses.setOnClickListener { showOpenSourceLicenses(this) }
    }

    private fun displayMarkdown(@RawRes markdownFile: Int) {
        activity_about_markdown_view.movementMethod = LinkMovementMethod.getInstance()
        Markwon.unscheduleDrawables(activity_about_markdown_view)
        Markwon.unscheduleTableRows(activity_about_markdown_view)

        activity_about_markdown_view.text = MarkdownRenderer.getInstance(this).getCachedRenderResult(markdownFile)

        Markwon.scheduleDrawables(activity_about_markdown_view)
        Markwon.scheduleTableRows(activity_about_markdown_view)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        fun showOpenSourceLicenses(context: Context) {
            OssLicensesMenuActivity.setActivityTitle(context.getString(R.string.activity_open_source_licenses_title))
            context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        }
    }
}
