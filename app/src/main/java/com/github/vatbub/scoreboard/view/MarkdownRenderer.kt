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
import android.util.SparseArray
import androidx.annotation.RawRes
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.commonmark.parser.Parser
import ru.noties.markwon.Markwon
import ru.noties.markwon.SpannableConfiguration
import ru.noties.markwon.renderer.SpannableRenderer
import ru.noties.markwon.spans.SpannableTheme
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.util.*


class MarkdownRenderer private constructor(private val context: Context) {
    private val results = SparseArray<CharSequence>()
    private val resultStatusArray = SparseArray<ResultStatus>()
    private val parser: Parser = Markwon.createParser()
    private val spannableConfiguration = SpannableConfiguration.builder(context)
            .theme(SpannableTheme.builderWithDefaults(context)
                    .headingBreakHeight(0)
                    .build())
            .build()


    companion object {
        private val instances = HashMap<Context, MarkdownRenderer>()
        operator fun get(context: Context): MarkdownRenderer {
            if (!instances.containsKey(context))
                instances[context] = MarkdownRenderer(context)

            return instances[context]!!
        }

        fun resetInstance(context: Context) =
            instances.remove(context)
    }

    fun getCachedRenderResult(@RawRes markdownFile: Int): CharSequence {
        if (resultStatusArray.get(markdownFile, ResultStatus.NOT_STARTED) == ResultStatus.NOT_STARTED)
            return renderSynchronously(markdownFile)


        while (resultStatusArray.get(markdownFile, ResultStatus.NOT_STARTED) != ResultStatus.READY)
            println("Waiting for rendering to finish...")

        return results.get(markdownFile)
    }

    fun prerender(@RawRes markdownFile: Int) = GlobalScope.launch {
        renderSynchronously(markdownFile)
    }

    fun renderSynchronously(@RawRes markdownFile: Int): CharSequence {
        resultStatusArray.put(markdownFile, ResultStatus.RENDERING)
        val lines = readLines(context.resources.openRawResource(markdownFile))

        val spannableRenderer = SpannableRenderer()
        val node = parser.parse(lines)
        val res = spannableRenderer.render(spannableConfiguration, node)
        results.put(markdownFile, res)
        resultStatusArray.put(markdownFile, ResultStatus.READY)
        return res
    }

    private fun readLines(input: InputStream) = readLines(InputStreamReader(input))

    /**
     * Get the contents of a `Reader` as a list of Strings,
     * one entry per line.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedReader`.
     *
     * @param input the `Reader` to read from, not null
     * @return the list of Strings, never null
     * @throws NullPointerException if the input is null
     */
    private fun readLines(input: Reader): String {
        val reader = BufferedReader(input)
        val res = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
            res.append(line).append("\n")
            line = reader.readLine()
        }
        return res.toString()
    }

    private enum class ResultStatus {
        READY, RENDERING, NOT_STARTED
    }
}