package it.arturoiafrate.shortcutbuddy.model.manager.shortcut;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.reflect.TypeToken;
import it.arturoiafrate.shortcutbuddy.model.bean.AppShortcuts;
import it.arturoiafrate.shortcutbuddy.model.bean.Shortcut;
import it.arturoiafrate.shortcutbuddy.model.manager.AbstractManager;
import it.arturoiafrate.shortcutbuddy.model.manager.IFileSystemManager;
import it.arturoiafrate.shortcutbuddy.model.manager.database.repository.ShortcutRepository;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ShortcutManager extends AbstractManager implements IFileSystemManager {
    private final SettingsManager settingsManager;
    private final ShortcutRepository shortcutRepository;
    private final Cache<String, AppShortcuts> appShortcutsCache;
    private final ConcurrentMap<String, AtomicInteger> appUsageIncrements;

    @Inject
    public ShortcutManager(SettingsManager settingsManager, ShortcutRepository shortcutRepository) {
        this.settingsManager = settingsManager;
        this.shortcutRepository = shortcutRepository;
        int cacheSize = Integer.parseInt(this.settingsManager.getSetting("cacheSize").value());
        this.appUsageIncrements = new ConcurrentHashMap<>();
        this.appShortcutsCache = Caffeine.newBuilder().maximumSize(cacheSize).recordStats().build();
        log.info("Cache size: {}", cacheSize);
    }

    @Override
    public void load() {
        String preloadAppsSettings = this.settingsManager.getSetting("preloadAppsNumber").value();
        if(!"disabled".equals(preloadAppsSettings)){
            var mostUsedApps = shortcutRepository.findMostUsedApps(Integer.parseInt(preloadAppsSettings));
            appShortcutsCache.putAll(mostUsedApps.stream()
                    .collect(Collectors.toMap(appShortcuts -> appShortcuts.getAppName().toLowerCase(), appShortcuts -> appShortcuts)));
        }
    }


    public List<Shortcut> getShortcutsForApp(String appName) {
        var singleAppShortcut = Optional.ofNullable(appShortcutsCache.get(appName.toLowerCase(), this::getAppShortcutsFromRepository));
        if(singleAppShortcut.isPresent()) {
            appUsageIncrements.computeIfAbsent(appName.toLowerCase(), k -> new AtomicInteger(0)).incrementAndGet();
        }
        return singleAppShortcut.isPresent() ? singleAppShortcut.get().getShortcuts() : Collections.emptyList();
    }

    private AppShortcuts getAppShortcutsFromRepository(String appName) {
        return shortcutRepository.findAppShortcutsByName(appName.toLowerCase());
    }

    public String getAppDescription(String appName) {
        var singleAppShortcut = Optional.ofNullable(appShortcutsCache.get(appName.toLowerCase(), this::getAppShortcutsFromRepository));
        return singleAppShortcut.isPresent() ? singleAppShortcut.get().getAppDescription() : "";
    }

    public void flushUsageCount(){
        Map<String, AtomicInteger> countsToFlush = new HashMap<>(appUsageIncrements);
        appUsageIncrements.clear();

        if (countsToFlush.isEmpty()) {
            return;
        }
        shortcutRepository.batchIncrementUsageCount(countsToFlush);
    }

}
