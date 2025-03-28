package it.arturoiafrate.shortcutbuddy.model.manager.settings;

import com.google.gson.reflect.TypeToken;
import it.arturoiafrate.shortcutbuddy.model.bean.Setting;
import it.arturoiafrate.shortcutbuddy.model.manager.AbstractManager;
import it.arturoiafrate.shortcutbuddy.model.manager.IFileSystemManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsManager extends AbstractManager implements IFileSystemManager {
    private static SettingsManager instance;
    private List<Setting> settings;
    private final String filename = "settings.json";

    private SettingsManager() {
        settings = new ArrayList<>();
    }

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    @Override
    public void load() {
        settings = loadFromFile(filename, new TypeToken<List<Setting>>() {}.getType());
        if (settings == null) {
            settings = new ArrayList<>();
        }
    }

    public Setting getSetting(String key) {
        return settings.stream()
                .filter(setting -> setting.key().equals(key))
                .findFirst().orElse(null);
    }
}
