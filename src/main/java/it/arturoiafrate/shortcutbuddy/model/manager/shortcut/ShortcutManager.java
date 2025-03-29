package it.arturoiafrate.shortcutbuddy.model.manager.shortcut;

import com.google.gson.reflect.TypeToken;
import it.arturoiafrate.shortcutbuddy.model.bean.AppShortcuts;
import it.arturoiafrate.shortcutbuddy.model.bean.Shortcut;
import it.arturoiafrate.shortcutbuddy.model.manager.AbstractManager;
import it.arturoiafrate.shortcutbuddy.model.manager.IFileSystemManager;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class ShortcutManager extends AbstractManager implements IFileSystemManager {
    private static ShortcutManager instance;
    private List<AppShortcuts> appShortcuts;
    private List<AppShortcuts> userDefinedShortcuts;

    private ShortcutManager() {
        appShortcuts = new ArrayList<>();
        userDefinedShortcuts = new ArrayList<>();
    }

    public static ShortcutManager getInstance() {
        if (instance == null) {
            instance = new ShortcutManager();
        }
        return instance;
    }
    @Override
    public void load() {
        final String filename = "shortcuts.json";
        appShortcuts = loadFromFile(filename, new TypeToken<List<AppShortcuts>>() {}.getType(), false);
        if (appShortcuts == null) {
            appShortcuts = new ArrayList<>();
        }
        if(SettingsManager.getInstance().isAppVersionUpdated()){
            List<AppShortcuts> resourceAppShortcuts = loadFromFile("/default/" + filename, new TypeToken<List<AppShortcuts>>() {}.getType(), true);
            for(AppShortcuts resourceAppShortcut : resourceAppShortcuts) {
                Optional<AppShortcuts> appShortcut = appShortcuts.stream()
                        .filter(appShortcuts -> appShortcuts.appName().equals(resourceAppShortcut.appName()))
                        .findFirst();
                if (appShortcut.isPresent()) {
                    if(resourceAppShortcut.shortcuts() != appShortcut.get().shortcuts()) {
                        appShortcuts.remove(appShortcut.get());
                        appShortcuts.add(resourceAppShortcut);
                    }
                } else {
                    appShortcuts.add(resourceAppShortcut);
                }
            }
        }
        loadUserDefinedShortcuts();
    }

    public void loadUserDefinedShortcuts() {
        final String filename = "usershortcuts.json";
        userDefinedShortcuts = loadIfFileExists(filename, new TypeToken<List<AppShortcuts>>() {}.getType());
        if (userDefinedShortcuts == null) {
            userDefinedShortcuts = new ArrayList<>();
        }
    }

    public List<Shortcut> getShortcutsForApp(String appName) {
        Optional<AppShortcuts> singleAppShortcut =  appShortcuts.stream()
                .filter(appShortcuts -> appShortcuts.appName().toLowerCase().contains(appName.toLowerCase()))
                .findFirst();
        if(singleAppShortcut.isPresent()){
            return singleAppShortcut.get().shortcuts();
        }
        singleAppShortcut = userDefinedShortcuts.stream()
                .filter(appShortcuts -> appShortcuts.appName().toLowerCase().contains(appName.toLowerCase()))
                .findFirst();
        return singleAppShortcut.isPresent() ? singleAppShortcut.get().shortcuts() : Collections.emptyList();
    }

    public String getAppDescription(String appName) {
        Optional<String> appDescription = appShortcuts.stream()
                .filter(appShortcuts -> appShortcuts.appName().toLowerCase().contains(appName.toLowerCase()))
                .map(AppShortcuts::appDescription)
                .findFirst();
        if(appDescription.isPresent() && StringUtils.isEmpty(appDescription.get())){
            appDescription = userDefinedShortcuts.stream()
                    .filter(appShortcuts -> appShortcuts.appName().toLowerCase().contains(appName.toLowerCase()))
                    .map(AppShortcuts::appDescription)
                    .findFirst();
        }
        return appDescription.orElse("");
    }

}
