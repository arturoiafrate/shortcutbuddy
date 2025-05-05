package it.arturoiafrate.shortcutbuddy.model.manager.clipboard;

import it.arturoiafrate.shortcutbuddy.model.bean.ClipboardEntry;
import it.arturoiafrate.shortcutbuddy.model.enumerator.ClipboardContentType;
import it.arturoiafrate.shortcutbuddy.model.manager.database.repository.ClipboardHistoryRepository;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import it.arturoiafrate.shortcutbuddy.service.IClipboardChangedService;
import it.arturoiafrate.shortcutbuddy.service.impl.ClipboardMonitorService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
@Slf4j
public class ClipboardHistoryManager implements IClipboardChangedService {

    private final SettingsManager settingsManager;
    private final ClipboardHistoryRepository repository;
    private final ConcurrentLinkedDeque<ClipboardEntry> historyInMemory;
    private final List<ClipboardEntry> newEntriesToSave;
    private final AtomicBoolean hasUnsavedChanges = new AtomicBoolean(false);

    private int historySizeLimit;
    private ClipboardContentType lastAddedType = null;
    private String lastAddedContent = null;
    private ClipboardMonitorService clipboardMonitorService;

    @Inject
    public ClipboardHistoryManager(SettingsManager settingsManager, ClipboardHistoryRepository repository, ClipboardMonitorService clipboardMonitorService) {
        this.settingsManager = settingsManager;
        this.repository = repository;
        this.clipboardMonitorService = clipboardMonitorService;
        this.historyInMemory = new ConcurrentLinkedDeque<>();
        this.newEntriesToSave = Collections.synchronizedList(new ArrayList<>());
    }

    public void initialize() {
        log.info("Initializing ClipboardHistoryManager...");
        if (!settingsManager.isEnabled("enableClipboardManager")) {
            log.info("Clipboard Manager feature is disabled in settings.");
            return;
        }
        clipboardMonitorService.subscribe(this);
        clipboardMonitorService.start();
        try {
            this.historySizeLimit = Integer.parseInt(settingsManager.getSetting("clipboardHistorySize").getValue());
            log.debug("Clipboard history size limit set to: {}", historySizeLimit);
        } catch (Exception e) {
            log.error("Invalid or missing clipboard history size setting, using default 25", e);
            this.historySizeLimit = 25;
        }
        loadHistoryFromDb();

        repository.getMostRecentEntryTypeAndContent().ifPresent(data -> {
            this.lastAddedType = (ClipboardContentType) data[0];
            this.lastAddedContent = (String) data[1];
        });

        log.info("ClipboardHistoryManager initialized. Loaded {} entries from DB. Last entry type: {}, content starts with: '{}'",
                historyInMemory.size(),
                lastAddedType,
                lastAddedContent != null ? lastAddedContent.substring(0, Math.min(lastAddedContent.length(), 30)) : "N/A");
    }

    /**
     * Aggiunge una nuova voce alla cronologia.
     * @param representation La rappresentazione stringa del contenuto (testo, path, etc.)
     * @param type Il tipo di contenuto.
     */
    public synchronized void addEntry(String representation, ClipboardContentType type) {
        if (type == null || StringUtils.isBlank(representation)) {
            log.warn("Attempted to add invalid clipboard entry (null type or blank content).");
            return;
        }

        // Evita duplicati *consecutivi* dello stesso tipo e contenuto
        // Usa Objects.equals per gestire il caso iniziale in cui lastAddedType è null
        if (Objects.equals(type, lastAddedType) && Objects.equals(representation, lastAddedContent)) {
            log.trace("Skipping duplicate consecutive clipboard entry (Type: {}, Content: '{}')", type, representation.substring(0, Math.min(representation.length(), 50)));
            return;
        }

        // Find duplicate entry and update its timestamp instead of skipping it
        ClipboardEntry existingEntry = historyInMemory.stream()
                .filter(entry -> entry.getContentType() == type && entry.getContent().equals(representation))
                .findFirst()
                .orElse(null);

        if (existingEntry != null) {
            log.trace("Found duplicate clipboard entry, updating timestamp (Type: {}, Content: '{}')", 
                    type, representation.substring(0, Math.min(representation.length(), 50)));

            // Remove the existing entry from the history
            historyInMemory.remove(existingEntry);

            // Update the timestamp
            long newTimestamp = System.currentTimeMillis();
            existingEntry.setTimestamp(newTimestamp);

            // Add it back to the front of the history
            historyInMemory.addFirst(existingEntry);
            lastAddedType = type;
            lastAddedContent = representation;

            // Add to the list for saving to DB
            synchronized (newEntriesToSave) {
                newEntriesToSave.add(existingEntry);
            }
            hasUnsavedChanges.set(true);

            return;
        }


        log.debug("Adding new clipboard entry - Type: {}, Content starts with: '{}'", type, representation.substring(0, Math.min(representation.length(), 50)));
        long timestamp = System.currentTimeMillis();
        ClipboardEntry newEntry = new ClipboardEntry(type, representation, timestamp);

        historyInMemory.addFirst(newEntry);
        lastAddedType = type;
        lastAddedContent = representation;

        // Gestione limite dimensione in memoria e pulizia cache immagini (se implementata)
        while (historyInMemory.size() > historySizeLimit) {
            ClipboardEntry removed = historyInMemory.removeLast();
            /*if (removed.getContentType() == ClipboardContentType.IMAGE_PATH) {
                // TODO: Implementare la cancellazione del file immagine dalla cache
                // deleteCachedImageFile(removed.getContent());
                log.trace("Removed oldest entry (IMAGE_PATH): {}", removed.getContent());
            } else {
                log.trace("Removed oldest entry (Type: {})", removed.getContentType());
            }*/
        }

        // Aggiungi alla lista per il salvataggio
        synchronized (newEntriesToSave) {
            newEntriesToSave.add(newEntry);
        }
        hasUnsavedChanges.set(true);
    }

    // ... (getHistory, loadHistoryFromDb, saveHistoryToDb rimangono simili,
    //      ma saveHistoryToDb ora salva entry con il contentType corretto)

    public List<ClipboardEntry> getHistory() {
        return List.copyOf(historyInMemory);
    }

    private void loadHistoryFromDb() {
        log.debug("Loading initial history from database...");
        List<ClipboardEntry> entries = repository.loadRecentEntries(historySizeLimit);
        historyInMemory.clear();
        historyInMemory.addAll(entries);
        log.debug("History loaded into memory. Size: {}", historyInMemory.size());
        // Aggiorna lastAdded con il più recente caricato
        if (!historyInMemory.isEmpty()) {
            ClipboardEntry mostRecent = historyInMemory.peekFirst();
            lastAddedType = mostRecent.getContentType();
            lastAddedContent = mostRecent.getContent();
        } else {
            lastAddedType = null;
            lastAddedContent = null;
        }
    }

    public synchronized void saveHistoryToDb() {
        if (!hasUnsavedChanges.get()) {
            log.trace("No unsaved clipboard changes to save.");
            return;
        }

        List<ClipboardEntry> entriesToSave;
        synchronized (newEntriesToSave) {
            if (newEntriesToSave.isEmpty()) {
                hasUnsavedChanges.set(false); // Flag potrebbe essere true ma la lista è vuota (race condition?)
                return;
            }
            entriesToSave = new ArrayList<>(newEntriesToSave);
            newEntriesToSave.clear();
        }

        log.info("Saving {} new clipboard entries to the database...", entriesToSave.size());
        boolean saved = repository.saveEntries(entriesToSave);

        if (saved) {
            log.debug("New entries saved successfully.");
            // Pulisci le entry più vecchie nel DB, mantenendone `historySizeLimit`
            repository.deleteOldEntries(historySizeLimit);
        } else {
            log.error("Failed to save new clipboard entries. Re-queuing them for next save attempt.");
            // Ri-aggiungi le entry non salvate all'inizio della coda per ritentare
            synchronized (newEntriesToSave) {
                newEntriesToSave.addAll(0, entriesToSave);
            }
            // Non resettare hasUnsavedChanges se il salvataggio fallisce
            return;
        }

        // Resetta il flag solo se il salvataggio è OK e non ci sono nuove entry nel frattempo
        synchronized (newEntriesToSave) {
            if (newEntriesToSave.isEmpty()) {
                hasUnsavedChanges.set(false);
                log.debug("Unsaved changes flag reset.");
            } else {
                log.debug("New entries arrived during save, keeping unsaved changes flag set.");
            }
        }
    }

    @Override
    public void clipboardChanged(String content, String type) {
        ClipboardContentType clipboardContentType = ClipboardContentType.valueOf(type.toUpperCase());
        if (clipboardContentType == null) {
            log.warn("Unsupported clipboard content type: {}", type);
            return;
        }
        if (clipboardContentType == ClipboardContentType.TEXT) {
            addEntry(content, clipboardContentType);
        }
        //TODO altri tipi
    }

    public void shutdown() {
        log.info("Shutting down ClipboardHistoryManager...");
        clipboardMonitorService.unsubscribe();
        saveHistoryToDb();
        log.debug("Stopping Clipboard Monitor Service");
        clipboardMonitorService.stop();
        log.info("ClipboardHistoryManager shut down successfully.");
    }
}
