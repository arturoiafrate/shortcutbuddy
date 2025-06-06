package it.arturoiafrate.shortcutbuddy.model.manager.shortcut;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.arturoiafrate.shortcutbuddy.model.bean.AppShortcuts;
import it.arturoiafrate.shortcutbuddy.model.bean.Shortcut;
import it.arturoiafrate.shortcutbuddy.model.bean.ShortcutEditLists;
import it.arturoiafrate.shortcutbuddy.model.manager.AbstractManager;
import it.arturoiafrate.shortcutbuddy.model.manager.IFileSystemManager;
import it.arturoiafrate.shortcutbuddy.model.manager.database.repository.ShortcutRepository;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    private final ConcurrentMap<Long, AtomicInteger> shortcutUsageIncrements;

    @Inject
    public ShortcutManager(SettingsManager settingsManager, ShortcutRepository shortcutRepository) {
        this.settingsManager = settingsManager;
        this.shortcutRepository = shortcutRepository;
        int cacheSize = Integer.parseInt(this.settingsManager.getSetting("cacheSize").getValue());
        this.appUsageIncrements = new ConcurrentHashMap<>();
        this.shortcutUsageIncrements = new ConcurrentHashMap<>();
        this.appShortcutsCache = Caffeine.newBuilder().maximumSize(cacheSize).recordStats().build();
        log.info("Cache size: {}", cacheSize);
        copyAppImages();
    }

    @Override
    public void load() {
        String preloadAppsSettings = this.settingsManager.getSetting("preloadAppsNumber").getValue();
        if(!"disabled".equals(preloadAppsSettings)){
            var mostUsedApps = shortcutRepository.findMostUsedApps(Integer.parseInt(preloadAppsSettings));
            appShortcutsCache.putAll(mostUsedApps.stream()
                    .collect(Collectors.toMap(appShortcuts -> appShortcuts.getAppName().toLowerCase(), appShortcuts -> appShortcuts)));
        }
    }

    public List<AppShortcuts> getAllApps() {
        return shortcutRepository.getAllAppList();
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
        Map<String, AtomicInteger> appCountsToFlush = new HashMap<>(appUsageIncrements);
        appUsageIncrements.clear();

        if (!appCountsToFlush.isEmpty()) {
            shortcutRepository.batchIncrementUsageCount(appCountsToFlush);
        }

        // Also flush shortcut usage counts
        flushShortcutUsageCount();
    }

    /**
     * Increments the usage count for a shortcut.
     * 
     * @param shortcutId The ID of the shortcut
     */
    public void incrementShortcutUsageCount(long shortcutId) {
        log.debug("Incrementing usage count for shortcut ID: {}", shortcutId);
        shortcutUsageIncrements.computeIfAbsent(shortcutId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Flushes the accumulated shortcut usage counts to the database.
     */
    public void flushShortcutUsageCount() {
        Map<Long, AtomicInteger> countsToFlush = new HashMap<>(shortcutUsageIncrements);
        shortcutUsageIncrements.clear();

        if (countsToFlush.isEmpty()) {
            return;
        }
        log.debug("Flushing usage counts for {} shortcuts", countsToFlush.size());
        shortcutRepository.batchIncrementShortcutUsageCount(countsToFlush);
    }

    public boolean addAppShortcuts(AppShortcuts appShortcuts) {
        if (StringUtils.isBlank(appShortcuts.getAppName())) {
            return false;
        }
        return shortcutRepository.insertApp(appShortcuts);
    }

    public void copyAppImage(String imageFilePath, String appName){
        if (StringUtils.isBlank(imageFilePath) || StringUtils.isBlank(appName)) {
            return;
        }
        try {
            Path sourcePath = Paths.get(imageFilePath);
            if (!Files.exists(sourcePath) || !Files.isReadable(sourcePath)) {
                log.error("Il file immagine sorgente non esiste o non è leggibile: {}", sourcePath);
                return;
            }
            String userHome = System.getProperty("user.home");
            Path destinationDir = Paths.get(userHome, ".shortcutbuddy", "appimages");
            Files.createDirectories(destinationDir);
            String destinationFileName = appName + ".png";
            Path destinationPath = destinationDir.resolve(destinationFileName);
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            log.error("Errore imprevisto durante la copia dell'immagine '{}'", imageFilePath, e);
        }
    }

    public boolean batchModifyShortcuts(String appName, ShortcutEditLists shortcutEditLists) {
        boolean added = false;
        boolean updated = false;
        boolean removed = false;
        if(shortcutEditLists.getAdded() != null) {
            for (Shortcut shortcut : shortcutEditLists.getAdded()) {
                added = shortcutRepository.insertShortcut(shortcut);
            }
        }
        if(shortcutEditLists.getUpdated() != null) {
            for (Shortcut shortcut : shortcutEditLists.getUpdated()) {
                updated = shortcutRepository.updateShortcut(shortcut);
            }
        }
        if(shortcutEditLists.getRemoved() != null) {
            for (Shortcut shortcut : shortcutEditLists.getRemoved()) {
                removed = shortcutRepository.deleteShortcut(shortcut.getId());
            }
        }
        if (appShortcutsCache.asMap().containsKey(appName.toLowerCase())) {
            appShortcutsCache.invalidate(appName.toLowerCase());
            appShortcutsCache.put(appName.toLowerCase(), shortcutRepository.findAppShortcutsByName(appName.toLowerCase()));
        }

        return added || updated || removed;
    }

    public boolean restoreDefaultShortcut(String appName, long shortcutId) {
        boolean restored = shortcutRepository.restoreDefaultShortcut(shortcutId);
        if (restored && appShortcutsCache.asMap().containsKey(appName.toLowerCase())) {
            appShortcutsCache.invalidate(appName.toLowerCase());
            appShortcutsCache.put(appName.toLowerCase(), shortcutRepository.findAppShortcutsByName(appName.toLowerCase()));
        }
        return restored;
    }

    /**
     * Updates the starred status of a shortcut.
     * 
     * @param shortcutId The ID of the shortcut to update
     * @param starred The new starred status
     * @return True if the update was successful, false otherwise
     */
    public boolean updateShortcutStarred(long shortcutId, boolean starred) {
        log.debug("Updating starred status to {} for shortcut ID: {}", starred, shortcutId);
        boolean updated = shortcutRepository.updateShortcutStarred(shortcutId, starred);

        // Find which app this shortcut belongs to and invalidate its cache
        if (updated) {
            for (Map.Entry<String, AppShortcuts> entry : appShortcutsCache.asMap().entrySet()) {
                String appName = entry.getKey();
                AppShortcuts appShortcuts = entry.getValue();

                // Check if this app contains the shortcut
                boolean containsShortcut = appShortcuts.getShortcuts().stream()
                        .anyMatch(shortcut -> shortcut.getId() == shortcutId);

                if (containsShortcut) {
                    log.debug("Invalidating cache for app: {} after updating shortcut starred status", appName);
                    appShortcutsCache.invalidate(appName);
                    appShortcutsCache.put(appName, shortcutRepository.findAppShortcutsByName(appName));
                    break;
                }
            }
        }

        return updated;
    }

}
