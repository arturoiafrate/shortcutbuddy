package it.arturoiafrate.shortcutbuddy.model.manager.shortcut;

import com.google.gson.reflect.TypeToken;
import it.arturoiafrate.shortcutbuddy.model.bean.AppShortcuts;
import it.arturoiafrate.shortcutbuddy.model.bean.Shortcut;
import it.arturoiafrate.shortcutbuddy.model.manager.AbstractManager;
import it.arturoiafrate.shortcutbuddy.model.manager.IFileSystemManager;
import java.util.*;

public class ShortcutManager extends AbstractManager implements IFileSystemManager {
    private static ShortcutManager instance;
    private List<AppShortcuts> appShortcuts;
    private final String filename = "shortcuts.json";

    private ShortcutManager() {
        appShortcuts = new ArrayList<>();
    }

    public static ShortcutManager getInstance() {
        if (instance == null) {
            instance = new ShortcutManager();
        }
        return instance;
    }
    @Override
    public void load() {
        appShortcuts = loadFromFile(filename, new TypeToken<List<AppShortcuts>>() {}.getType());
        if (appShortcuts == null) {
            appShortcuts = new ArrayList<>();
        }
    }

    public List<Shortcut> getShortcutsForApp(String appName) {
        Optional<AppShortcuts> singleAppShortcut =  appShortcuts.stream()
                .filter(appShortcuts -> appShortcuts.appName().toLowerCase().contains(appName.toLowerCase()))
                .findFirst();
        return singleAppShortcut.isPresent() ? singleAppShortcut.get().shortcuts() : Collections.emptyList();
    }

}
