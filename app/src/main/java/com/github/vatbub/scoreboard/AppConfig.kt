package com.github.vatbub.scoreboard

import android.content.Context
import com.github.vatbub.common.core.Config
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

class AppConfig private constructor(context: Context) {
    private val config: Config

    init {
        copyDefaultConfigToUserMemoryIfNotPresent(context)
        config =
                Config(URL("https://raw.githubusercontent.com/vatbub/Scoreboard/master/app/src/main/assets/default_config.properties"), getCopyOfDefaultConfig(context).toURI().toURL(), true, "remote_config.properties", true)
    }

    @Throws(IOException::class)
    private fun copyDefaultConfigToUserMemoryIfNotPresent(context: Context) {
        val copyDestination = getCopyOfDefaultConfig(context)
        val sharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID + ".RemoteConfigInfo", Context.MODE_PRIVATE)!!
        if (sharedPreferences.getInt(configAppVersionKey, BuildConfig.VERSION_CODE) <= BuildConfig.VERSION_CODE && copyDestination.exists())
            return

        IOUtils.copy(context.assets.open("default_config.properties"), FileOutputStream(copyDestination, false))
    }

    private fun getCopyOfDefaultConfig(context: Context): File {
        return File(context.cacheDir, "default_config.properties")
    }

    fun getConfig(): Config? {
        if (instance == null)
            throw NullPointerException("Please call AppConfig.initialize(Context) prior to calling AppConfig.getInstance()")
        return config
    }

    object Keys {
        val WEBSITE_URL = "websiteURL"
        val GITHUB_URL = "githubURL"
        val INSTAGRAM_URL = "instagramURL"
        val MAX_LINES_FOR_ENTER_TEXT = "maxLinesForEnterText"
    }

    companion object {
        private val configAppVersionKey = "configSavedWithAppVersion"
        private var instance: AppConfig? = null

        private val config: Config
            get() {
                val instanceCopy = instance
                        ?: throw IllegalStateException("Please call AppConfig.initialize(Context) prior to calling AppConfig.getInstance()")
                return instanceCopy.config
            }

        val websiteURL: String
            get() = config.getValue(Keys.WEBSITE_URL)!!

        val githubURL: String
            get() = config.getValue(Keys.GITHUB_URL)!!

        val instagramURL: String
            get() = config.getValue(Keys.INSTAGRAM_URL)!!

        val maxLinesForEnterText
            get() = Integer.parseInt(config.getValue(Keys.MAX_LINES_FOR_ENTER_TEXT))

        fun initialize(context: Context) {
            if (instance == null)
                instance = AppConfig(context)
        }
    }
}
