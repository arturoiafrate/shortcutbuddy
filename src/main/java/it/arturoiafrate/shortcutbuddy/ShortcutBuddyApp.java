package it.arturoiafrate.shortcutbuddy;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import it.arturoiafrate.shortcutbuddy.config.ApplicationComponent;
import it.arturoiafrate.shortcutbuddy.config.DaggerApplicationComponent;
import it.arturoiafrate.shortcutbuddy.config.module.FxModule;
import it.arturoiafrate.shortcutbuddy.config.module.NotificationModule;
import it.arturoiafrate.shortcutbuddy.controller.ClipboardSnippetController;
import it.arturoiafrate.shortcutbuddy.controller.ShortcutController;
import it.arturoiafrate.shortcutbuddy.model.constant.KeyOption;
import it.arturoiafrate.shortcutbuddy.model.constant.Label;
import it.arturoiafrate.shortcutbuddy.model.enumerator.Languages;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyOperation;
import it.arturoiafrate.shortcutbuddy.model.manager.clipboard.ClipboardHistoryManager;
import it.arturoiafrate.shortcutbuddy.model.manager.hotkey.GlobalHotkeyManager;
import it.arturoiafrate.shortcutbuddy.model.manager.shortcut.ShortcutManager;
import it.arturoiafrate.shortcutbuddy.model.manager.tray.TrayManager;
import it.arturoiafrate.shortcutbuddy.service.INotificationService;
import it.arturoiafrate.shortcutbuddy.service.impl.UpdateCheckerService;
import it.arturoiafrate.shortcutbuddy.utility.AppInfo;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShortcutBuddyApp extends Application {
    @Getter
    private static ShortcutBuddyApp instance;

    private Stage shortcutStage;
    private Stage settingsStage;
    private Stage userShortcutsStage;
    private Stage clipboardSnippetStage;
    private Stage splashStage;
    private ShortcutController shortcutController;
    private ClipboardSnippetController clipboardSnippetController;
    private ResourceBundle bundle;
    private TrayManager trayManager;
    private INotificationService notificationService;
    private UpdateCheckerService updateCheckerService;
    private ApplicationComponent applicationComponent;
    private ShortcutManager shortcutManager;
    private ClipboardHistoryManager clipboardHistoryManager;
    private ScheduledExecutorService backgroundScheduler;
    private GlobalHotkeyManager globalHotkeyManager;
    private static HostServices appHostServices;

    private final Set<Stage> openStages = ConcurrentHashMap.newKeySet();

    /**
     * Initializes the application before the start method is called.
     * Sets up the default language and theme.
     */
    @Override
    public void init() {
        log.info("Initializing application - setting default language and theme");
        this.bundle = ResourceBundle.getBundle("i18n/messages", Languages.getLocale("english"));
        log.debug("Default language set to English");
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        log.debug("Default theme set to Light");
    }

    /**
     * Starts the application, initializes the Shortcut stage, and begins the application loading process.
     * 
     * @param shortcutStage The shortcut stage provided by JavaFX
     */
    @Override
    public void start(Stage shortcutStage) {
        log.debug("Starting application");
        instance = this;

        appHostServices = getHostServices();
        this.shortcutStage = shortcutStage;
        this.shortcutStage.initStyle(StageStyle.UNDECORATED);
        this.shortcutStage.hide();
        Platform.setImplicitExit(false);
        checkAppSupport();
        loadAppIcon();
        showSplashScreen();
        startBackgroundTasks();

        registerStage(shortcutStage);
        log.debug("Application startup complete");
    }

    /**
     * Stops the application and performs cleanup operations.
     * Closes all open windows, shuts down the scheduler, and releases resources.
     * 
     * @throws Exception If an error occurs during shutdown
     */
    @Override
    public void stop() throws Exception {
        log.info("Stopping application");
        closeAllOpenWindows();

        shutdownScheduler();
        if(globalHotkeyManager != null) {
            log.debug("Shutting down globalHotkeyManager");
            globalHotkeyManager.shutdown();
        }
        if(trayManager != null) {
            log.debug("Removing tray icon");
            trayManager.exitTray();
        }
        if(shortcutManager != null) {
            log.debug("Flushing shortcut usage count");
            shortcutManager.flushUsageCount();
        }
        if(clipboardHistoryManager != null) {
            log.debug("Stopping Clipboard Monitor Service");
            clipboardHistoryManager.shutdown();
        }
        log.info("Application shutdown complete");
        super.stop();
    }


    /**
     * Closes all open windows that have been registered with the application.
     * This ensures that no windows remain open when the application exits.
     */
    private void closeAllOpenWindows() {
        log.info("Closing all open windows: {} windows to close", openStages.size());
        Set<Stage> stagesToClose = ConcurrentHashMap.newKeySet();
        stagesToClose.addAll(openStages);

        for (Stage stage : stagesToClose) {
            try {
                if (stage != null && stage.isShowing()) {
                    log.info("Closing window: {}", stage.getTitle());
                    Platform.runLater(stage::close);
                }
            } catch (Exception e) {
                log.error("Error closing stage", e);
            }
        }
        openStages.clear();
    }

    /**
     * Registers a window with the application so it can be tracked and closed when the application exits.
     * 
     * @param stage The stage to register
     */
    public void registerStage(Stage stage) {
        if (stage != null) {
            openStages.add(stage);
            log.debug("Registered stage: {}, total open stages: {}", stage.getTitle(), openStages.size());
            stage.setOnHidden(event -> unregisterStage(stage));
        }
    }

    /**
     * Unregisters a window from the application.
     * 
     * @param stage The stage to unregister
     */
    public void unregisterStage(Stage stage) {
        if (stage != null) {
            openStages.remove(stage);
            log.debug("Unregistered stage: {}, remaining open stages: {}", stage.getTitle(), openStages.size());
        }
    }

    /**
     * Checks if the application is supported on the current operating system.
     * The application requires Windows OS and system tray support.
     * 
     * @throws RuntimeException If the system does not support the required features
     */
    private void checkAppSupport(){
        log.debug("Checking application support");
        if (!System.getProperty("os.name").toLowerCase().contains("windows") || !SystemTray.isSupported()) {
            log.error("System tray is not supported on this OS");
            throw new RuntimeException(bundle.getString(Label.ERROR_ICONTRAY));
        }
        log.debug("Application support check passed");
    }

    /**
     * Loads the application icon and sets it for the shortcut stage.
     */
    private void loadAppIcon(){
        log.debug("Loading application icon");
        try{
            javafx.scene.image.Image appIcon = new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png")));
            shortcutStage.getIcons().add(appIcon);
            log.debug("Application icon loaded successfully");
        } catch (Exception e) {
            log.error("Error loading application icon", e);
        }
    }

    /**
     * Creates and displays the splash screen with a progress bar.
     * The splash screen is shown while the application is loading.
     */
    private void showSplashScreen() {
        log.debug("Creating splash screen");
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        ImageView splashImage = new ImageView(Objects.requireNonNull(this.getClass().getResource("/images/splashscreen_16_9.jpg")).toExternalForm());
        double aspectRatio = 16.0 / 9.0;
        double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
        double imageWidth = screenWidth * 0.5;
        double imageHeight = imageWidth / aspectRatio;
        if (imageHeight > screenHeight * 0.5) {
            imageHeight = screenHeight * 0.5;
            imageWidth = imageHeight * aspectRatio;
        }
        splashImage.setFitWidth(imageWidth);
        splashImage.setFitHeight(imageHeight);
        splashImage.setPreserveRatio(true);
        ProgressBar progressBar = new ProgressBar();
        StackPane root = new StackPane();
        VBox layoutVBox = new VBox();
        layoutVBox.getChildren().addAll(splashImage, progressBar);
        layoutVBox.setAlignment(Pos.CENTER);
        root.getChildren().addAll(layoutVBox);
        Scene splashScene = new Scene(root);
        splashStage.setScene(splashScene);

        registerStage(splashStage);

        log.debug("Displaying splash screen");
        splashStage.show();
        loadApplication(progressBar);
    }

    /**
     * Loads the application components in a background thread and updates the progress bar.
     * This method initializes all the necessary components for the application to run.
     * 
     * @param progressBar The progress bar to update during loading
     */
    private void loadApplication(ProgressBar progressBar) {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                log.info("Starting application loading process");
                log.info("Setting up SQLite database");
                updateProgress(0, 100);

                // Create a temporary component just to access the settings
                ApplicationComponent tempComponent = DaggerApplicationComponent.builder()
                        .fxModule(new FxModule(bundle, appHostServices))
                        .notificationModule(new NotificationModule())
                        .build();
                tempComponent.getShortcutRepository().touch();

                log.info("Loading settings");
                tempComponent.getSettingsManager().load();
                updateProgress(10, 100);

                // Update the bundle with the user's language preference
                String chosenLanguage = tempComponent.getSettingsManager().getSetting("language").getValue();
                log.debug("Applying user language from settings: {}", chosenLanguage);
                Locale locale = Languages.getLocale(chosenLanguage);
                bundle = ResourceBundle.getBundle("i18n/messages", locale);

                // Now create the real component with the correct bundle
                log.info("Loading Dagger2 components with user language");
                applicationComponent = DaggerApplicationComponent.builder()
                        .fxModule(new FxModule(bundle, appHostServices))
                        .notificationModule(new NotificationModule())
                        .build();
                applicationComponent.getShortcutRepository().touch();
                applicationComponent.getSettingsManager().load();
                updateProgress(15, 100);
                Platform.runLater(() -> {
                    String chosenTheme = applicationComponent.getSettingsManager().getSetting("theme").getValue();
                    if(!StringUtils.isEmpty(chosenTheme)){
                        log.debug("Applying user theme: {}", chosenTheme);
                        if(chosenTheme.equals("dark")){
                            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
                        }else if(chosenTheme.equals("light")){
                            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
                        }
                    }
                });

                log.info("Initializing Hotkey Manager");
                updateProgress(30, 100);
                globalHotkeyManager = applicationComponent.getGlobalHotkeyManager();


                initShortcutStage();
                if(applicationComponent.getSettingsManager().isEnabled("enableClipboardManager")){
                    initClipboardSnippetStage();
                }

                updateProgress(40, 100);

                log.info("Loading shortcuts");
                applicationComponent.getShortcutManager().load();
                updateProgress(65, 100);

                log.info("Starting the tray icon");
                startTrayIcon();
                shortcutManager = applicationComponent.getShortcutManager();
                updateProgress(85, 100);
                initPlugins();
                updateProgress(100, 100);

                log.info("Checking for updates");
                if(applicationComponent.getSettingsManager().isAppVersionUpdated()){
                    Platform.runLater(() -> {
                        notificationService.showNotification(bundle.getString(Label.NOTIFICATION_APPUPDATE_TITLE), 
                            MessageFormat.format(bundle.getString(Label.NOTIFICATION_APPUPDATE_TEXT), AppInfo.getVersion()), 
                            TrayIcon.MessageType.NONE);
                    });
                }
                if(applicationComponent.getSettingsManager().isEnabled("checkForUpdates")) {
                    updateCheckerService.checkForUpdatesAsync(false);
                }
                log.info("Application loading complete - ShortcutBuddy is live and running");
                return null;
            }
        };

        loadTask.setOnSucceeded(event -> {
            log.debug("Loading task completed, closing splash screen");
            splashStage.close();
        });

        progressBar.progressProperty().bind(loadTask.progressProperty());

        log.debug("Starting application loading thread");
        new Thread(loadTask).start();
    }


    private void initPlugins(){
        log.info("Initializing plugins");
        clipboardHistoryManager = applicationComponent.getClipboardHistoryManager();
        try {
            clipboardHistoryManager.initialize();
            log.info("Clipboard Manager initialized");
        } catch (Exception e) {
            log.error("Failed to initialize Clipboard Manager", e);
        }
        log.info("Plugin services started.");
    }

    /**
     * Initializes and starts the system tray icon.
     * This method sets up the tray icon with the necessary references and starts it.
     */
    private void startTrayIcon(){
        log.debug("Initializing tray icon");
        trayManager = applicationComponent.getTrayManager();
        notificationService = applicationComponent.getApplicationTrayNotificationService();
        updateCheckerService = applicationComponent.getUpdateCheckerService();
        trayManager.setUpdateCheckerService(updateCheckerService);
        Platform.runLater(() -> {
            log.debug("Setting up tray icon components");
            trayManager.setSettingsStage(settingsStage);
            trayManager.setShortcutController(shortcutController);
            trayManager.setUserShortcutsStage(userShortcutsStage);
            log.debug("Starting tray icon");
            trayManager.startTray();
        });
    }

    /**
     * Initializes the shortcut stage of the application.
     * This method loads the user's theme and language preferences, sets up the main view,
     * and initializes the shortcut controller.
     */
    private void initShortcutStage() {
        log.debug("Initializing shortcut stage");
        Platform.runLater(() -> {

            shortcutStage.setTitle(bundle.getString(Label.APP_TITLE));

            log.debug("Loading main view");
            FXMLLoader fxmlLoader = new FXMLLoader(ShortcutBuddyApp.class.getResource("/view/shortcut-view.fxml"), bundle);
            fxmlLoader.setControllerFactory(applicationComponent.getControllerFactory());
            try {
                int width = Integer.parseInt(applicationComponent.getSettingsManager().getSetting("width").getValue());
                int height = Integer.parseInt(applicationComponent.getSettingsManager().getSetting("height").getValue());
                log.debug("Creating scene with dimensions: {}x{}", width, height);
                Scene scene = new Scene(fxmlLoader.load(), width, height);
                shortcutStage.setScene(scene);

                shortcutController = fxmlLoader.getController();
                subscribeShowShortcutStage();
                applicationComponent.inject(shortcutController);
                shortcutController.setBundle(bundle);
                shortcutController.setStage(shortcutStage);
                log.debug("Shortcut stage initialization complete");
            } catch (IOException e) {
                log.error("Error while loading Shortcut stage", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Subscribes to key events for the application.
     * This method sets up listeners for various keyboard keys that the application responds to.
     *
     */

    private void subscribeShowShortcutStage(){
        globalHotkeyManager = applicationComponent.getGlobalHotkeyManager();
        globalHotkeyManager.subscribeKeyHold(NativeKeyEvent.VC_CONTROL, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_CONTROL, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_ESCAPE, shortcutController);
        int keyCode = NativeKeyEvent.VC_PERIOD;
        String searchKey = applicationComponent.getSettingsManager().getSetting("searchKey").getValue();
        log.debug("Configuring search key: {}", searchKey);

        if(KeyOption.SPACE.equals(searchKey)) {
            keyCode = NativeKeyEvent.VC_SPACE;
        } else if(KeyOption.MINUS.equals(searchKey)) {
            keyCode = NativeKeyEvent.VC_MINUS;
        }
        else if(KeyOption.P.equals(searchKey)) {
            keyCode = NativeKeyEvent.VC_P;
        }
        globalHotkeyManager.subscribeKeyEvent(keyCode, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_DOWN, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_UP, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_LEFT, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_RIGHT, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_ENTER, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_1, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_2, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_3, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_4, shortcutController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_5, shortcutController);
    }



    /**
     * Initializes the clipboard snippet stage of the application.
     * This method loads the user's theme and language preferences, sets up the clipboard snippet view,
     * and initializes the clipboard snippet controller.
     */
    private void initClipboardSnippetStage() {
        log.debug("Initializing clipboard snippet stage");
        Platform.runLater(() -> {
            clipboardSnippetStage = new Stage();
            clipboardSnippetStage.initStyle(StageStyle.UNDECORATED);
            clipboardSnippetStage.setTitle(bundle.getString(Label.APP_TITLE));
            registerStage(clipboardSnippetStage);

            log.debug("Loading clipboard snippet view");
            FXMLLoader fxmlLoader = new FXMLLoader(ShortcutBuddyApp.class.getResource("/view/clipboardSnippet-view.fxml"), bundle);
            fxmlLoader.setControllerFactory(applicationComponent.getControllerFactory());
            try {
                int width = 450;
                int height = 350;
                log.debug("Creating scene with dimensions: {}x{}", width, height);
                Scene scene = new Scene(fxmlLoader.load(), width, height);
                clipboardSnippetStage.setScene(scene);

                clipboardSnippetController = fxmlLoader.getController();
                subscribeShowClipboardSnippetStage();
                applicationComponent.inject(clipboardSnippetController);
                clipboardSnippetController.setBundle(bundle);
                clipboardSnippetController.setStage(clipboardSnippetStage);
                log.debug("Clipboard snippet stage initialization complete");
            } catch (IOException e) {
                log.error("Error while loading Clipboard snippet stage", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Subscribes to key events for the clipboard snippet view.
     * This method sets up listeners for various keyboard keys that the clipboard snippet view responds to.
     */
    private void subscribeShowClipboardSnippetStage() {
        globalHotkeyManager = applicationComponent.getGlobalHotkeyManager();

        // Set up a shortcut for Ctrl+\ to show the clipboard snippet view
        Set<Integer> ctrlBackslash = ConcurrentHashMap.newKeySet();
        ctrlBackslash.add(NativeKeyEvent.VC_CONTROL);
        ctrlBackslash.add(NativeKeyEvent.VC_BACK_SLASH);

        globalHotkeyManager.subscribeShortcut(ctrlBackslash, (keyCode, mode, nativeKeyEvent) -> {
            if (mode == KeyOperation.KEY_PRESS) {
                log.debug("Ctrl+Shift+V pressed, showing clipboard snippet view");
                clipboardSnippetController.showStage();
            }
        });

        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_ESCAPE, clipboardSnippetController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_DOWN, clipboardSnippetController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_UP, clipboardSnippetController);
        globalHotkeyManager.subscribeKeyEvent(NativeKeyEvent.VC_ENTER, clipboardSnippetController);
    }

    /**
     * Starts background tasks for the application.
     * This method sets up a scheduled executor to periodically flush usage count data.
     */
    private void startBackgroundTasks(){
        log.debug("Setting up background tasks");
        ThreadFactory daemonThreadFactory = runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName("ShortcutBuddy-UsageFlushThread");
            thread.setDaemon(true);
            return thread;
        };
        backgroundScheduler = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory);

        Runnable flushTask = () -> {
            log.info("Executing scheduled background task: flushing usage count");
            try {
                if(shortcutManager != null) {
                    shortcutManager.flushUsageCount();
                    log.debug("Usage count flushed successfully");
                } else {
                    log.debug("Shortcut manager not initialized, skipping flush");
                }
            } catch (Exception e) {
                log.error("Error flushing usage count", e);
            }
            if(applicationComponent != null && applicationComponent.getSettingsManager() != null && clipboardHistoryManager != null){
                if(applicationComponent.getSettingsManager().isEnabled("enableClipboardManager")){
                    clipboardHistoryManager.saveHistoryToDb();
                }
            }

            log.info("Scheduled background task completed");
        };

        backgroundScheduler.scheduleAtFixedRate(flushTask, 15, 15, TimeUnit.MINUTES);
        log.info("Background tasks scheduled to run every 15 minutes");
    }

    /**
     * Shuts down the background task scheduler.
     * This method ensures that the scheduler is properly terminated before the application exits.
     */
    private void shutdownScheduler() {
        log.debug("Shutting down background scheduler");
        if (backgroundScheduler != null && !backgroundScheduler.isShutdown()) {
            backgroundScheduler.shutdown();
            try {
                log.debug("Waiting for background tasks to complete");
                if (!backgroundScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("The background scheduler did not terminate in the specified time");
                    backgroundScheduler.shutdownNow();
                    log.debug("Forced shutdown of background scheduler");
                } else {
                    log.info("Background scheduler successfully terminated");
                }
            } catch (InterruptedException e) {
                log.warn("Background scheduler interrupted during shutdown", e);
                backgroundScheduler.shutdownNow();
                Thread.currentThread().interrupt();
                log.debug("Thread interrupted flag set");
            }
        } else {
            log.debug("Background scheduler already shutdown or not initialized");
        }
    }

    /**
     * The main entry point for the application.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        launch();
    }
}
