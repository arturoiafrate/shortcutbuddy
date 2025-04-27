package it.arturoiafrate.shortcutbuddy.model.manager.settings;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.arturoiafrate.shortcutbuddy.model.bean.Setting;
import it.arturoiafrate.shortcutbuddy.model.manager.AbstractManager;
import it.arturoiafrate.shortcutbuddy.model.manager.IFileSystemManager;
import it.arturoiafrate.shortcutbuddy.utility.AppInfo;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class SettingsManager extends AbstractManager implements IFileSystemManager {
    private List<Setting> settings;
    private final String filename = "settings.json";
    private final String currentAppVersion;
    @Getter
    private boolean appVersionUpdated = false;

    @Inject
    public SettingsManager() {
        settings = new ArrayList<>();
        currentAppVersion = AppInfo.getVersion();
    }


    @Override
    public void load() {
        settings = loadFromFile(filename, new TypeToken<List<Setting>>() {}.getType(), false);
        if (settings == null) {
            settings = new ArrayList<>();
        }
        Optional<Setting> appVersionSetting = settings.stream()
                .filter(setting -> setting.getKey().equals("appVersion"))
                .findFirst();

        appVersionUpdated = appVersionSetting.isEmpty() || !appVersionSetting.get().getValue().equals(currentAppVersion);
        if(appVersionUpdated){
            log.info("App version updated from {} to {}", appVersionSetting.map(Setting::getValue).orElse("null"), currentAppVersion);
            List<Setting> settingsFromResource = loadFromFile("/default/" + filename, new TypeToken<List<Setting>>() {}.getType(), true);
            log.info("Settings loaded from resource");
            for(Setting settingFromResource : settingsFromResource) {
                Optional<Setting> cSetting = settings.stream()
                        .filter(s -> s.getKey().equals(settingFromResource.getKey()))
                        .findFirst();
                if(cSetting.isPresent()){
                    log.info("Setting {} already present", settingFromResource.getKey());
                    if(cSetting.get().getOptions() != settingFromResource.getOptions()) {
                        log.info("Setting {} options changed from {} to {}", settingFromResource.getKey(), cSetting.get().getOptions(), settingFromResource.getOptions());
                        settings.remove(cSetting.get());
                        settings.add(new Setting(settingFromResource.getKey(), cSetting.get().getValue(), settingFromResource.isReadonly(), settingFromResource.getOptions(), settingFromResource.isHide(), settingFromResource.getOrder()));
                    }
                }else{
                    log.info("Setting {} not present, adding it", settingFromResource.getKey());
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
                .filter(setting -> setting.getKey().equals(key))
                .findFirst().orElse(null);
    }

    public boolean isEnabled(String key) {
        Setting setting = getSetting(key);
        return setting != null && "y".equals(setting.getValue());
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
