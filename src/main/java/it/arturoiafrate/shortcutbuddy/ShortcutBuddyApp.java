package it.arturoiafrate.shortcutbuddy;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import it.arturoiafrate.shortcutbuddy.config.ApplicationComponent;
import it.arturoiafrate.shortcutbuddy.config.DaggerApplicationComponent;
import it.arturoiafrate.shortcutbuddy.config.module.FxModule;
import it.arturoiafrate.shortcutbuddy.config.module.NotificationModule;
import it.arturoiafrate.shortcutbuddy.controller.ShortcutController;
import it.arturoiafrate.shortcutbuddy.model.constant.KeyOption;
import it.arturoiafrate.shortcutbuddy.model.constant.Label;
import it.arturoiafrate.shortcutbuddy.model.enumerator.Languages;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyListener;
import it.arturoiafrate.shortcutbuddy.model.keyemulator.KeyEmulator;
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
    private static ShortcutBuddyApp instance;

    private Stage primaryStage;
    private Stage splashStage;
    private Stage settingsStage;
    private Stage userShortcutsStage;
    private KeyListener keyListener;
    private KeyEmulator keyEmulator;
    private ShortcutController shortcutController;
    private ResourceBundle bundle;
    private TrayManager trayManager;
    private INotificationService notificationService;
    private UpdateCheckerService updateCheckerService;
    private ApplicationComponent applicationComponent;
    private ShortcutManager shortcutManager;
    private ScheduledExecutorService backgroundScheduler;
    private static HostServices appHostServices;

    private final Set<Stage> openStages = ConcurrentHashMap.newKeySet();

    /**
     * Gets the singleton instance of the application.
     * 
     * @return The ShortcutBuddyApp instance
     */
    public static ShortcutBuddyApp getInstance() {
        return instance;
    }

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
     * Starts the application, initializes the primary stage, and begins the application loading process.
     * 
     * @param primaryStage The primary stage provided by JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        log.debug("Starting application");
        instance = this;

        appHostServices = getHostServices();
        this.primaryStage = primaryStage;
        this.primaryStage.initStyle(StageStyle.UNDECORATED);
        this.primaryStage.hide();
        Platform.setImplicitExit(false);
        checkAppSupport();
        loadAppIcon();
        showSplashScreen();
        startBackgroundTasks();

        registerStage(primaryStage);
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
        if(keyListener != null) {
            log.debug("Shutting down key listener");
            keyListener.shutdown();
        }
        if(trayManager != null) {
            log.debug("Removing tray icon");
            trayManager.exitTray();
        }
        if(shortcutManager != null) {
            log.debug("Flushing shortcut usage count");
            shortcutManager.flushUsageCount();
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
     * Loads the application icon and sets it for the primary stage.
     */
    private void loadAppIcon(){
        log.debug("Loading application icon");
        try{
            javafx.scene.image.Image appIcon = new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png")));
            primaryStage.getIcons().add(appIcon);
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
                log.info("Loading Dagger2 components and setting up SQLite database");
                updateProgress(0, 100);
                applicationComponent = DaggerApplicationComponent.builder()
                        .fxModule(new FxModule(bundle, appHostServices))
                        .notificationModule(new NotificationModule())
                        .build();
                applicationComponent.getShortcutRepository().touch();

                log.info("Loading settings");
                applicationComponent.getSettingsManager().load();
                updateProgress(15, 100);

                log.info("Initializing KeyListener");
                keyListener = applicationComponent.getKeyListener();
                updateProgress(30, 100);

                initPrimaryStage();
                updateProgress(50, 100);

                log.info("Loading shortcuts");
                applicationComponent.getShortcutManager().load();
                updateProgress(75, 100);

                log.info("Starting the tray icon");
                startTrayIcon();
                shortcutManager = applicationComponent.getShortcutManager();
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
     * Initializes the primary stage of the application.
     * This method loads the user's theme and language preferences, sets up the main view,
     * and initializes the shortcut controller.
     */
    private void initPrimaryStage() {
        log.debug("Initializing primary stage");
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

            String chosenLanguage = applicationComponent.getSettingsManager().getSetting("language").getValue();
            log.debug("Applying user language: {}", chosenLanguage);
            Locale locale = Languages.getLocale(chosenLanguage);
            bundle = ResourceBundle.getBundle("i18n/messages", locale);
            primaryStage.setTitle(bundle.getString(Label.APP_TITLE));

            log.debug("Loading main view");
            FXMLLoader fxmlLoader = new FXMLLoader(ShortcutBuddyApp.class.getResource("/view/shortcut-view.fxml"), bundle);
            fxmlLoader.setControllerFactory(applicationComponent.getControllerFactory());
            try {
                int width = Integer.parseInt(applicationComponent.getSettingsManager().getSetting("width").getValue());
                int height = Integer.parseInt(applicationComponent.getSettingsManager().getSetting("height").getValue());
                log.debug("Creating scene with dimensions: {}x{}", width, height);
                Scene scene = new Scene(fxmlLoader.load(), width, height);
                primaryStage.setScene(scene);

                subscribeKeyEvents(fxmlLoader);
                shortcutController = fxmlLoader.getController();
                applicationComponent.inject(shortcutController);
                shortcutController.setBundle(bundle);
                shortcutController.setStage(primaryStage);
                log.debug("Primary stage initialization complete");
            } catch (IOException e) {
                log.error("Error while loading primary stage", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Subscribes to key events for the application.
     * This method sets up listeners for various keyboard keys that the application responds to.
     * 
     * @param fxmlLoader The FXMLLoader containing the controller to subscribe to events
     */
    private void subscribeKeyEvents(FXMLLoader fxmlLoader){
        log.debug("Subscribing to key events");
        keyListener.subscribe(NativeKeyEvent.VC_CONTROL, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_ESCAPE, fxmlLoader.getController());

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

        keyListener.subscribe(keyCode, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_DOWN, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_UP, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_LEFT, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_RIGHT, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_ENTER, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_1, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_2, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_3, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_4, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_5, fxmlLoader.getController());
        log.debug("Key event subscriptions complete");
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
