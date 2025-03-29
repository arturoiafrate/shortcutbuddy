package it.arturoiafrate.shortcutbuddy.utility;

import java.io.InputStream;
import java.util.Properties;
import java.io.IOException;

public class AppVersion {

    private static final String APP_PROPERTIES = "/application.properties"; // Path nelle risorse
    private static String loadedVersion = null;
    private static String loadedAppName = null;

    private static void loadVersion() {
        if (loadedVersion != null) {
            return;
        }

        Properties props = new Properties();
        try (InputStream input = AppVersion.class.getResourceAsStream(APP_PROPERTIES)) {
            if (input == null) {
                loadedVersion = "UNKNOWN"; // Valore di fallback
                loadedAppName = "UNKNOWN";
                return;
            }
            props.load(input);
            loadedVersion = props.getProperty("app.version", "UNKNOWN"); // Default se manca la prop
            loadedAppName = props.getProperty("app.name", "UNKNOWN");
        } catch (IOException ex) {
            loadedVersion = "ERROR"; // Fallback in caso di errore I/O
            loadedAppName = "ERROR";
        }
    }

    public static String getVersion() {
        if (loadedVersion == null) {
            loadVersion();
        }
        return loadedVersion;
    }

    public static String getName() {
        if (loadedAppName == null) {
            loadVersion();
        }
        return loadedAppName;
    }
}
