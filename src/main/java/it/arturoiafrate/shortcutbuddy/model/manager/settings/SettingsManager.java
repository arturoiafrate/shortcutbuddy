package it.arturoiafrate.shortcutbuddy.model.manager.settings;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.arturoiafrate.shortcutbuddy.model.bean.Setting;
import it.arturoiafrate.shortcutbuddy.model.manager.AbstractManager;
import it.arturoiafrate.shortcutbuddy.model.manager.IFileSystemManager;
import it.arturoiafrate.shortcutbuddy.utility.AppInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class SettingsManager extends AbstractManager implements IFileSystemManager {
    private static SettingsManager instance;
    private List<Setting> settings;
    private final String filename = "settings.json";
    private final String currentAppVersion;
    private boolean appVersionUpdated = false;

    private SettingsManager() {
        settings = new ArrayList<>();
        currentAppVersion = AppInfo.getVersion();
    }

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    @Override
    public void load() {
        settings = loadFromFile(filename, new TypeToken<List<Setting>>() {}.getType(), false);
        if (settings == null) {
            settings = new ArrayList<>();
        }
        Optional<Setting> appVersionSetting = settings.stream()
                .filter(setting -> setting.key().equals("appVersion"))
                .findFirst();
        boolean checkForSettingsUpdate = false;
        if(appVersionSetting.isPresent()) {
            Setting appVersion = appVersionSetting.get();
            if (!appVersion.value().equals(currentAppVersion)) {
                settings.remove(appVersion);
                Setting newAppVersion = new Setting("appVersion", currentAppVersion, true, null, true);
                settings.add(newAppVersion);
                appVersionUpdated = true;
                checkForSettingsUpdate = true;
            }
        } else {
            Setting appVersion = new Setting("appVersion", currentAppVersion, true, null, true);
            settings.add(appVersion);
            appVersionUpdated = true;
        }
        if (appVersionUpdated) {
            if(checkForSettingsUpdate){
                List<Setting> settingsFromResource = loadFromFile("/default/" + filename, new TypeToken<List<Setting>>() {}.getType(), true);
                for(Setting setting : settingsFromResource) {
                    Optional<Setting> cSetting = settings.stream()
                            .filter(s -> s.key().equals(setting.key()))
                            .findFirst();
                    if(cSetting.isPresent()){
                        if(cSetting.get().options() != setting.options()) {
                            settings.remove(cSetting.get());
                            settings.add(new Setting(setting.key(), cSetting.get().value(), setting.readonly(), setting.options(), setting.isHide()));
                        }
                    }else{
                        settings.add(setting);
                    }
                }
            }
            save(settings);
        }
    }

    public Setting getSetting(String key) {
        return settings.stream()
                .filter(setting -> setting.key().equals(key))
                .findFirst().orElse(null);
    }

    public List<Setting> getSettingsAll(){
        return settings;
    }

    public boolean save(List<Setting> settings) {
        try{
            File file = new File(getFilePath(filename));
            String jsonString = new Gson().toJson(settings);
            FileUtils.writeStringToFile(file, jsonString, "UTF-8");
            return true;
        } catch (Exception e){
            log.error("Error while saving settings", e);
        }
        return false;
    }

    public boolean isAppVersionUpdated() {
        return appVersionUpdated;
    }
}
