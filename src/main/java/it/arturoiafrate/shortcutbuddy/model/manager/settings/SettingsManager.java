package it.arturoiafrate.shortcutbuddy.model.manager.settings;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.arturoiafrate.shortcutbuddy.model.bean.Setting;
import it.arturoiafrate.shortcutbuddy.model.manager.AbstractManager;
import it.arturoiafrate.shortcutbuddy.model.manager.IFileSystemManager;
import it.arturoiafrate.shortcutbuddy.model.manager.database.repository.SettingsRepository;
import it.arturoiafrate.shortcutbuddy.utility.AppInfo;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class SettingsManager extends AbstractManager implements IFileSystemManager {
    private final String currentAppVersion;
    private final SettingsRepository settingsRepository;
    private final Cache<String, Setting> settingsCache;

    @Getter private boolean appVersionUpdated = false;
    @Getter private boolean devMode = false;

    @Inject
    public SettingsManager(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
        this.currentAppVersion = AppInfo.getVersion();
        this.settingsCache = Caffeine.newBuilder().maximumSize(50).recordStats().build();
    }


    @Override
    public void load() {
        var settings = settingsRepository.getAllSettings();
        settingsCache.invalidateAll();
        settingsCache.putAll(settings.stream()
                .collect(Collectors.toMap(Setting::getKey, setting -> setting)));
        Setting appVersionSetting = settingsCache.getIfPresent("app.internal.lastVersion");
        assert appVersionSetting != null;
        appVersionUpdated = !appVersionSetting.getValue().equals(currentAppVersion);
        if(appVersionUpdated){
            log.info("App version updated from {} to {}", appVersionSetting.getValue(), currentAppVersion);
            appVersionSetting.setValue(currentAppVersion);
            settingsRepository.updateSetting(appVersionSetting);
            settingsCache.invalidate("app.internal.lastVersion");
            settingsCache.put("app.internal.lastVersion", appVersionSetting);
        }
        String dev = System.getenv("DEV");
        if(!StringUtils.isEmpty(dev) && dev.equals("true")) {
            devMode = true;
            log.info("Running in DEV mode");
        }
    }

    private Setting getSettingFromRepository(String key) {
        return settingsRepository.getSettingByKey(key);
    }

    public Setting getSetting(String key) {
        return settingsCache.get(key, this::getSettingFromRepository);
    }

    public boolean isEnabled(String key) {
        Setting setting = getSetting(key);
        return setting != null && "y".equals(setting.getValue());
    }

    public List<Setting> getSettingsAll(){
        return new ArrayList<>(settingsCache.getAllPresent(settingsCache.asMap().keySet()).values());
    }

    public boolean save(List<Setting> settings) {
        settings.forEach(setting -> {
            settingsCache.invalidate(setting.getKey());
            settingsRepository.updateSetting(setting);
            settingsCache.put(setting.getKey(), setting);
        });
        return true;
    }
}
