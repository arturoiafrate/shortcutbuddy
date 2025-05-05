package it.arturoiafrate.shortcutbuddy.service.impl;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import it.arturoiafrate.shortcutbuddy.model.interceptor.clipboard.User32Ext;
import it.arturoiafrate.shortcutbuddy.model.interceptor.clipboard.Kernel32Ext;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import it.arturoiafrate.shortcutbuddy.service.IClipboardChangedService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.Optional;

@Singleton
@Slf4j
public class ClipboardMonitorService implements WinUser.WindowProc { // Implementiamo WindowProc

    private final SettingsManager settingsManager;

    private volatile boolean running = false;
    private Thread messageLoopThread;
    private WinDef.HWND listenerHwnd = null;
    private User32Ext user32 = User32Ext.INSTANCE;
    private Kernel32Ext kernel32 = Kernel32Ext.INSTANCE;
    private WinDef.HMODULE hMod;
    private final String windowClassName = "ShortcutBuddyClipboardListener";
    private Optional<IClipboardChangedService> subscriber = Optional.empty();


    @Inject
    public ClipboardMonitorService(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public void subscribe(IClipboardChangedService subscriber) {
        this.subscriber = Optional.of(subscriber);
    }

    public void unsubscribe() {
        this.subscriber = Optional.empty();
    }

    /**
     * Avvia il monitoraggio della clipboard se l'impostazione è abilitata.
     */
    public synchronized void start() {
        // Assicurati che l'impostazione esista e sia letta correttamente
        if (!settingsManager.isEnabled("enableClipboardManager")) {
            log.info("Clipboard Manager feature is disabled in settings.");
            return;
        }
        if (running) {
            log.warn("Clipboard monitor is already running.");
            return;
        }

        log.info("Starting Clipboard Monitor Service...");
        running = true;
        messageLoopThread = new Thread(this::runMessageLoop, "ClipboardListenerThread");
        messageLoopThread.setDaemon(true);
        messageLoopThread.start();
    }

    /**
     * Ferma il monitoraggio della clipboard.
     */
    public synchronized void stop() {
        if (!running) {
            log.warn("Clipboard monitor is not running.");
            return;
        }
        log.info("Stopping Clipboard Monitor Service...");
        running = false;

        // Invia un messaggio per terminare il message loop in modo pulito
        if (listenerHwnd != null) {
            log.debug("Posting WM_QUIT to listener window thread.");
            // Usiamo PostMessage invece di PostQuitMessage perché non siamo sullo stesso thread
            user32.PostMessage(listenerHwnd, WinUser.WM_QUIT, new WinDef.WPARAM(0), new WinDef.LPARAM(0));
        }

        // Aspetta che il thread termini
        try {
            if (messageLoopThread != null && messageLoopThread.isAlive()) {
                log.debug("Waiting for message loop thread to terminate...");
                messageLoopThread.join(1000); // Attesa massima di 1 secondo
                if (messageLoopThread.isAlive()) {
                    log.warn("Message loop thread did not terminate gracefully, interrupting.");
                    messageLoopThread.interrupt(); // Interrompi se non termina
                } else {
                    log.debug("Message loop thread terminated successfully.");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for message loop thread to stop.", e);
        }
        messageLoopThread = null;
        listenerHwnd = null; // Resetta l'handle
        log.info("Clipboard Monitor Service stopped.");
    }

    private void runMessageLoop() {
        log.debug("Clipboard listener thread started.");
        hMod = kernel32.GetModuleHandle(null);
        if (hMod == null) {
            log.error("Failed to get module handle, Error: {}", kernel32.GetLastError());
            running = false;
            return;
        }

        // Registra la classe della finestra
        WinUser.WNDCLASSEX wClass = new WinUser.WNDCLASSEX();
        wClass.hInstance = hMod;
        wClass.lpfnWndProc = ClipboardMonitorService.this; // Usa l'istanza corrente come WindowProc
        wClass.lpszClassName = windowClassName;
        wClass.cbSize = wClass.size(); // Importantissimo!

        if (user32.RegisterClassEx(wClass).longValue() == 0) {
            int lastError = kernel32.GetLastError();
            // Ignora errore se la classe è già registrata (ERROR_CLASS_ALREADY_EXISTS = 1410)
            if (lastError != 1410) {
                log.error("Failed to register window class '{}', Error: {}", windowClassName, lastError);
                running = false;
                return;
            } else {
                log.warn("Window class '{}' already registered.", windowClassName);
            }
        } else {
            log.debug("Window class '{}' registered successfully.", windowClassName);
        }


        // Crea una finestra nascosta (non message-only per poter avere un WndProc personalizzato facilmente)
        listenerHwnd = user32.CreateWindowEx(
                0,                          // No extended styles
                windowClassName,            // Class name
                "ShortcutBuddyClipboardListener", // Window name (debug)
                0,                          // Basic style (WS_OVERLAPPED might work too)
                0, 0, 0, 0,                 // Position and size (irrilevanti)
                null,                       // No parent window
                null,                       // No menu
                hMod,                       // Instance handle
                null                        // No extra parameters
        );

        if (listenerHwnd == null) {
            log.error("Failed to create listener window, Error: {}", kernel32.GetLastError());
            cleanupResources(true); // Prova a de-registrare la classe se l'abbiamo registrata noi
            running = false;
            return;
        }
        log.info("Listener window created successfully (HWND: {})", Pointer.nativeValue(listenerHwnd.getPointer()));


        // Aggiungi il listener
        if (!user32.AddClipboardFormatListener(listenerHwnd)) {
            log.error("Failed to add clipboard format listener, Error: {}", kernel32.GetLastError());
            cleanupResources(true); // Distrugge la finestra e de-registra la classe
            running = false;
            return;
        }
        log.info("Successfully added clipboard format listener.");

        // Message Loop
        WinUser.MSG msg = new WinUser.MSG();
        int result;
        log.debug("Starting message loop...");
        while (running && (result = user32.GetMessage(msg, null, 0, 0)) != 0) { // Ascolta tutti i messaggi del thread
            if (result == -1) {
                log.error("Error in GetMessage: {}", kernel32.GetLastError());
                running = false; // Esci dal loop in caso di errore
                break;
            } else {
                user32.TranslateMessage(msg);
                user32.DispatchMessage(msg);
            }
        }
        log.debug("Message loop finished.");

        // Cleanup finale quando il loop termina (o per WM_QUIT o per errore/stop)
        cleanupResources(false); // Non provare a de-registrare la classe se il loop è finito normalmente
    }

    // Implementazione di WindowProc
    @Override
    public WinDef.LRESULT callback(WinDef.HWND hwnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
        if (hwnd.equals(listenerHwnd)) { // Assicurati che il messaggio sia per la nostra finestra
            switch (uMsg) {
                case User32Ext.WM_CLIPBOARDUPDATE:
                    log.debug("WM_CLIPBOARDUPDATE received by WindowProc");
                    // Esegui l'azione sulla UI thread di JavaFX
                    Platform.runLater(this::handleClipboardChange);
                    return new WinDef.LRESULT(0); // Messaggio gestito

                case WinUser.WM_DESTROY:
                    log.debug("WM_DESTROY received by WindowProc, posting quit message.");
                    // Potremmo voler terminare il loop qui se la finestra viene distrutta esternamente
                    user32.PostQuitMessage(0);
                    return new WinDef.LRESULT(0); // Messaggio gestito
            }
        }
        // Per tutti gli altri messaggi, chiama la procedura di default
        return user32.DefWindowProc(hwnd, uMsg, wParam, lParam);
    }


    private void handleClipboardChange() {
        log.info("Processing clipboard change...");
        Clipboard clipboard = null;
        Transferable contents = null;
        DataFlavor[] flavors = null;

        try {
            // Optional: Breve attesa se sospetti problemi di timing
            // try { Thread.sleep(50); } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }

            clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            // Tenta di ottenere i contenuti e i flavor, gestendo eccezioni specifiche
            try {
                contents = clipboard.getContents(null);
                if (contents == null) {
                    log.info("Clipboard content is null (possibly cleared).");
                    // Qui potresti notificare l'history manager che la clipboard è stata svuotata
                    // historyManager.addClearEvent();
                    return; // Nessun contenuto da processare
                }
                // Ottenere i flavor disponibili può già sollevare eccezioni per tipi sconosciuti
                flavors = contents.getTransferDataFlavors();

            } catch (IllegalStateException e) {
                log.warn("Clipboard is currently unavailable: {}", e.getMessage());
                // Potresti implementare un piccolo ritardo e riprovare una volta se questo errore è frequente
                return; // Non possiamo procedere se la clipboard è bloccata
            } catch (Exception e) {
                // Cattura eccezioni più generiche durante l'analisi iniziale dei flavor
                // Questo potrebbe includere ClassNotFoundException interne di AWT
                log.warn("Could not get all available clipboard flavors, possibly due to unknown data types: {}", e.getMessage());
                // Non usciamo subito, proviamo comunque a leggere i tipi standard noti
                // Se contents è stato ottenuto, possiamo ancora usarlo
                if (contents == null) {
                    try { // Ritenta getContents se il primo tentativo è fallito insieme a getTransferDataFlavors
                        contents = clipboard.getContents(null);
                        if (contents == null) return; // Se ancora null, usciamo
                    } catch (IllegalStateException e2) {
                        log.warn("Clipboard became unavailable on second attempt: {}", e2.getMessage());
                        return;
                    }
                }
                // In mancanza di 'flavors', non possiamo fare il log dettagliato dei tipi disponibili dopo
            }

            boolean processed = false; // Flag per sapere se abbiamo già gestito un formato utile

            // --- Priorità 1: Testo ---
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                    log.info("Clipboard now contains TEXT: '{}'", text);
                    this.subscriber.ifPresent(s -> s.clipboardChanged(text, "TEXT"));
                    processed = true; // Abbiamo trovato il testo, potremmo fermarci o continuare per altri formati
                } catch (UnsupportedFlavorException | IOException e) {
                    log.error("Error retrieving String data from clipboard", e);
                } catch (Exception e) { // Catch più generico per sicurezza
                    log.error("Unexpected error retrieving String data", e);
                }
            }

            //TODO
            // --- Priorità 2: Immagini (se vuoi gestirle e non hai già preso il testo) ---
            // Modifica la condizione `&& !processed` se vuoi salvare *sia* testo che immagine quando presenti
//            if (contents != null && contents.isDataFlavorSupported(DataFlavor.imageFlavor) /* && !processed */) {
//                try {
//                    java.awt.Image img = (java.awt.Image) contents.getTransferData(DataFlavor.imageFlavor);
//                    log.info("Clipboard now contains an IMAGE: {}x{}", img.getWidth(null), img.getHeight(null));
//                    // historyManager.addEntry(img); // <<-- Qui aggiungeresti l'immagine (o una sua rappresentazione)
//                    processed = true;
//                } catch (UnsupportedFlavorException | IOException e) {
//                    log.error("Error retrieving Image data from clipboard", e);
//                } catch (Exception e) {
//                    log.error("Unexpected error retrieving Image data", e);
//                }
//            }

            // --- Priorità 3: Lista File (se vuoi gestirla) ---
//            if (contents != null && contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor) /* && !processed */) {
//                try {
//                    java.util.List<java.io.File> fileList = (java.util.List<java.io.File>) contents.getTransferData(DataFlavor.javaFileListFlavor);
//                    if (fileList != null && !fileList.isEmpty()) {
//                        log.info("Clipboard now contains FILES: {}", fileList);
//                        // historyManager.addEntry(fileList); // <<-- Qui aggiungeresti la lista file
//                        processed = true;
//                    }
//                } catch (UnsupportedFlavorException | IOException e) {
//                    log.error("Error retrieving File List data from clipboard", e);
//                } catch (Exception e) {
//                    log.error("Unexpected error retrieving File List data", e);
//                }
//            }

            // Se non abbiamo processato nessun formato noto, logga i tipi disponibili (se li abbiamo ottenuti)
//            if (!processed && flavors != null) {
//                log.info("Clipboard contains data, but not in a directly handled format (String, Image, FileList). Available flavors:");
//                for (DataFlavor flavor : flavors) {
//                    // Loggare il flavor può ancora causare ClassNotFoundException se il logger prova a chiamare toString() su un flavor malformato
//                    try {
//                        log.debug("  - MIME Type: {}, Representation Class: {}", flavor.getMimeType(), flavor.getRepresentationClass());
//                    } catch (Exception e) {
//                        log.debug("  - Error logging flavor details: {}", e.getMessage());
//                    }
//                }
//            }

        } catch (Exception e) { // Catch-all finale per robustezza
            log.error("Unexpected critical error in handleClipboardChange", e);
        }
    }

    /**
     * Pulisce le risorse native (listener, finestra, classe).
     * @param unregisterClass Indica se tentare di de-registrare la classe della finestra.
     */
    private synchronized void cleanupResources(boolean unregisterClass) {
        if (listenerHwnd != null) {
            log.debug("Removing clipboard format listener for HWND: {}", Pointer.nativeValue(listenerHwnd.getPointer()));
            if (!user32.RemoveClipboardFormatListener(listenerHwnd)) {
                log.warn("Failed to remove clipboard format listener, Error: {}", kernel32.GetLastError());
            }

            log.debug("Destroying listener window HWND: {}", Pointer.nativeValue(listenerHwnd.getPointer()));
            if (!user32.DestroyWindow(listenerHwnd)) {
                log.warn("Failed to destroy listener window, Error: {}", kernel32.GetLastError());
            }
            listenerHwnd = null; // L'handle non è più valido
        }

        if (unregisterClass && hMod != null) {
            log.debug("Unregistering window class: {}", windowClassName);
            if (!user32.UnregisterClass(windowClassName, hMod)) {
                int lastError = kernel32.GetLastError();
                // Potrebbe fallire se altre istanze usano la stessa classe, non necessariamente un problema grave
                log.warn("Failed to unregister window class '{}', Error: {}", windowClassName, lastError);
            }
        }
    }
}