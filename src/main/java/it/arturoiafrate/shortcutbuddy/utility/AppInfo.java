package it.arturoiafrate.shortcutbuddy.utility;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Properties;
import java.io.IOException;

@Slf4j
public class AppInfo {

    private static final String PROPERTIES_PATH = "/application.properties";
    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        if (loaded) return;
        try (InputStream input = AppInfo.class.getResourceAsStream(PROPERTIES_PATH)) {
            if (input == null) {
                loaded = true;
                return;
            }
            properties.load(input);
            loaded = true;
        } catch (IOException ex) {
            log.error("Error loading properties file: {}", PROPERTIES_PATH, ex);
            loaded = true;
        }
    }

    private static String getProperty(String key, String defaultValue) {
        if (!loaded) {
            loadProperties(); // Dovrebbe essere già stato chiamato dal blocco static, ma per sicurezza
        }
        return properties.getProperty(key, defaultValue);
    }

    public static String getVersion() {
        return getProperty("app.version", "N/A");
    }

    public static String getName() {
        return getProperty("app.name", "ShortcutBuddy");
    }

    public static String getLicense() {
        return getProperty("app.license", "MIT License");
    }

    public static String getDeveloper() {
        return getProperty("app.developer", "Arturo Iafrate");
    }

    public static String getGithubUrl() {
        return getProperty("app.githubUrl", "");
    }
}
