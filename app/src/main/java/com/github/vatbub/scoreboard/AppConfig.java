package com.github.vatbub.scoreboard;

public class AppConfig {
    private static AppConfig instance;

    private final String websiteURL;
    private final String githubURL;
    private final String instagramURL;

    private AppConfig() {
    }

    public static AppConfig getInstance() {
        if (instance == null)
            instance = new AppConfig();

        return instance;
    }

    public String getWebsiteURL() {
        return websiteURL;
    }

    public String getGithubURL() {
        return githubURL;
    }

    public String getInstagramURL() {
        return instagramURL;
    }

    public int getMaxLinesForEnterText() {
        return 5;
    }
}
