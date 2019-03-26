package com.github.vatbub.scoreboard.data

import android.content.Context
import org.jdom2.Document
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.io.File

object XmlFileUtils {
    private fun getFileName(fileNameWithoutExtension: String): String = "$fileNameWithoutExtension.xml"
    private fun getFile(context: Context, fileNameWithoutExtension: String): File = File(context.filesDir, getFileName(fileNameWithoutExtension))

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
