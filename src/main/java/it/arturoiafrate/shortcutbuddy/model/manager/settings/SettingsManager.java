package it.arturoiafrate.shortcutbuddy.model.manager.settings;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.arturoiafrate.shortcutbuddy.model.bean.Setting;
import it.arturoiafrate.shortcutbuddy.model.manager.AbstractManager;
import it.arturoiafrate.shortcutbuddy.model.manager.IFileSystemManager;
import it.arturoiafrate.shortcutbuddy.utility.AppInfo;
import lombok.Getter;
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
    @Getter
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

        appVersionUpdated = appVersionSetting.isEmpty() || !appVersionSetting.get().value().equals(currentAppVersion);
        if(appVersionUpdated){
            log.info("App version updated from {} to {}", appVersionSetting.map(Setting::value).orElse("null"), currentAppVersion);
            List<Setting> settingsFromResource = loadFromFile("/default/" + filename, new TypeToken<List<Setting>>() {}.getType(), true);
            log.info("Settings loaded from resource");
            for(Setting settingFromResource : settingsFromResource) {
                Optional<Setting> cSetting = settings.stream()
                        .filter(s -> s.key().equals(settingFromResource.key()))
                        .findFirst();
                if(cSetting.isPresent()){
                    log.info("Setting {} already present", settingFromResource.key());
                    if(cSetting.get().options() != settingFromResource.options()) {
                        log.info("Setting {} options changed from {} to {}", settingFromResource.key(), cSetting.get().options(), settingFromResource.options());
                        settings.remove(cSetting.get());
                        settings.add(new Setting(settingFromResource.key(), cSetting.get().value(), settingFromResource.readonly(), settingFromResource.options(), settingFromResource.isHide(), settingFromResource.order()));
                    }
                }else{
                    log.info("Setting {} not present, adding it", settingFromResource.key());
                    settings.add(settingFromResource);
                }
            }
            appVersionSetting.ifPresent(setting -> settings.remove(setting));
            settings.add(new Setting("appVersion", currentAppVersion, true, null, true, 0));
            log.info("Saving settings with new app version {}", currentAppVersion);
            save(settings);
        }
    }

    public Setting getSetting(String key) {
        return settings.stream()
                .filter(setting -> setting.key().equals(key))
                .findFirst().orElse(null);
    }

    public boolean isEnabled(String key) {
        Setting setting = getSetting(key);
        return setting != null && "y".equals(setting.value());
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
}
