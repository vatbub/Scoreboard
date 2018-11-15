package com.github.vatbub.scoreboard;

import android.content.Context;
import android.content.SharedPreferences;

import com.github.vatbub.common.core.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class AppConfig {
    private static final String configAppVersionKey = "configSavedWithAppVersion";
    private static AppConfig instance;
    private Config config;

    private AppConfig(Context context) {
        try {
            copyDefaultConfigToUserMemoryIfNotPresent(context);
            config = new Config(new URL("https://raw.githubusercontent.com/vatbub/Scoreboard/master/app/src/main/assets/default_config.properties"), getCopyOfDefaultConfig(context).toURI().toURL(), true, "remote_config.properties", true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static AppConfig getInstance() {
        return instance;
    }

    public static void initialize(Context context) {
        if (instance == null)
            instance = new AppConfig(context);
    }

    private void copyDefaultConfigToUserMemoryIfNotPresent(Context context) throws IOException {
        File copyDestination = getCopyOfDefaultConfig(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID + ".RemoteConfigInfo", Context.MODE_PRIVATE);
        if (sharedPreferences.getInt(configAppVersionKey, BuildConfig.VERSION_CODE) <= BuildConfig.VERSION_CODE && copyDestination.exists())
            return;

        try (InputStream configInputStream = context.getAssets().open("default_config.properties"); FileOutputStream fileOutputStream = new FileOutputStream(copyDestination, false)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = configInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, length);
            }
        }
    }

    private File getCopyOfDefaultConfig(Context context) {
        return new File(context.getCacheDir(), "default_config.properties");
    }

    public Config getConfig() {
        if (instance == null)
            throw new NullPointerException("Please call AppConfig.initialize(Context) prior to calling AppConfig.getInstance()");
        return config;
    }

    public String getWebsiteURL() {
        return getConfig().getValue(Keys.WEBSITE_URL);
    }

    public String getGithubURL() {
        return getConfig().getValue(Keys.GITHUB_URL);
    }

    public String getInstagramURL() {
        return getConfig().getValue(Keys.INSTAGRAM_URL);
    }

    public int getMaxLinesForEnterText() {
        return Integer.parseInt(getConfig().getValue(Keys.MAX_LINES_FOR_ENTER_TEXT));
    }

    public static class Keys {
        public static final String WEBSITE_URL = "websiteURL";
        public static final String GITHUB_URL = "githubURL";
        public static final String INSTAGRAM_URL = "instagramURL";
        public static final String MAX_LINES_FOR_ENTER_TEXT = "maxLinesForEnterText";
    }
}
