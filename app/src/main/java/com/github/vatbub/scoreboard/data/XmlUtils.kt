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

package com.github.vatbub.scoreboard.data

import android.content.Context
import org.jdom2.Document
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.io.File
import java.io.StringWriter

object XmlUtils {
    private fun getFileName(fileNameWithoutExtension: String) = "$fileNameWithoutExtension.xml"
    private fun getFile(context: Context, fileNameWithoutExtension: String) = File(context.filesDir, getFileName(fileNameWithoutExtension))

    fun Document.serializeToString(prettify: Boolean = false): String {
        val outputter = if (prettify) XMLOutputter(Format.getPrettyFormat()) else XMLOutputter(Format.getCompactFormat())
        StringWriter().use { stringWriter ->
            outputter.output(document, stringWriter)
            return stringWriter.toString()
        }
    }

    fun saveFile(context: Context, filenameWithoutExtension: String, document: Document) {
        val outputter = XMLOutputter(Format.getPrettyFormat())
        context.openFileOutput(getFileName(filenameWithoutExtension), Context.MODE_PRIVATE).use {
            outputter.output(document, it)
        }
    }

    fun readFile(context: Context, fileNameWithoutExtension: String): Document? {
        val file = getFile(context, fileNameWithoutExtension)
        if (!file.exists())
            return null

        return SAXBuilder().build(file)
    }
}


object XmlConstants {
    object GameManager {
        const val XML_GAME_IDS_TAG_NAME = "ids"
        const val XML_GAME_ID_TAG_NAME = "id"
    }

    object Game {
        const val XML_GAME_TAG_NAME = "game"
        const val XML_GAME_ID_ATTRIBUTE = "id"
        const val XML_GAME_NAME_ATTRIBUTE = "name"
        const val XML_GAME_SHARED_ID_ATTRIBUTE = "sharedId"
        const val XML_GAME_IS_HOST_OF_SHARED_GAME_ATTRIBUTE = "isHostOfSharedGame"
        const val XML_GAME_GAME_MODE_ATTRIBUTE = "mode"
        const val XML_GAME_PLAYERS_TAG_NAME = "players"
    }

    object Player {
        const val XML_PLAYER_TAG_NAME = "player"
        const val XML_PLAYER_ID_ATTRIBUTE = "id"
        const val XML_PLAYER_NAME_ATTRIBUTE = "name"
        const val XML_PLAYER_SCORES_TAG_NAME = "scores"
        const val XML_PLAYER_SCORE_TAG_NAME = "score"
    }
}
