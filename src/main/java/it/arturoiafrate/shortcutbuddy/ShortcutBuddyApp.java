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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShortcutBuddyApp extends Application {
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

    @Override
    public void init() {
        log.info("Initializing application - setting default language and theme");
        this.bundle = ResourceBundle.getBundle("i18n/messages", Languages.getLocale("english"));
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    }

    @Override
    public void start(Stage primaryStage) {
        appHostServices = getHostServices();
        this.primaryStage = primaryStage;
        this.primaryStage.initStyle(StageStyle.UNDECORATED);
        this.primaryStage.hide();
        Platform.setImplicitExit(false);
        checkAppSupport();
        loadAppIcon();
        showSplashScreen();
        startBackgroundTasks();
    }

    @Override
    public void stop() throws Exception {
        shutdownScheduler();
        if(keyListener != null) {
            keyListener.shutdown();
        }
        if(trayManager != null) {
            trayManager.exitTray();
        }
        if(shortcutManager != null) {
            shortcutManager.flushUsageCount();
        }
        super.stop();
    }

    private void checkAppSupport(){
        if (!System.getProperty("os.name").toLowerCase().contains("windows") || !SystemTray.isSupported()) {
            log.error("System tray is not supported on this OS");
            throw new RuntimeException(bundle.getString(Label.ERROR_ICONTRAY));
        }
    }

    private void loadAppIcon(){
        try{
            javafx.scene.image.Image appIcon = new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png")));
            primaryStage.getIcons().add(appIcon);
        } catch (Exception e) {
            log.error("Error loading application icon", e);
        }
    }

    private void showSplashScreen() {
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
        splashStage.show();
        loadApplication(progressBar);
    }

    private void loadApplication(ProgressBar progressBar) {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                log.info("********[Loading application]********");
                log.info("*Loading Dagger2 components and setup SQLite database...");
                updateProgress(0, 100);
                applicationComponent = DaggerApplicationComponent.builder()
                        .fxModule(new FxModule(bundle, appHostServices))
                        .notificationModule(new NotificationModule())
                        .build();
                applicationComponent.getShortcutRepository().touch();
                log.info("**Loading settings...");
                applicationComponent.getSettingsManager().load();
                updateProgress(15, 100);
                log.info("***Initializing KeyListener...");
                keyListener = applicationComponent.getKeyListener();
                updateProgress(30, 100);
                initPrimaryStage();
                updateProgress(50, 100);
                log.info("****Loading shortcuts...");
                applicationComponent.getShortcutManager().load();
                updateProgress(75, 100);
                log.info("*****Starting the tray icon...");
                startTrayIcon();
                shortcutManager = applicationComponent.getShortcutManager();
                updateProgress(100, 100);
                log.info("******Check for updates...");
                if(applicationComponent.getSettingsManager().isAppVersionUpdated()){
                    Platform.runLater(() -> {
                        notificationService.showNotification(bundle.getString(Label.NOTIFICATION_APPUPDATE_TITLE), MessageFormat.format(bundle.getString(Label.NOTIFICATION_APPUPDATE_TEXT), AppInfo.getVersion()), TrayIcon.MessageType.NONE);
                    });
                }
                if(applicationComponent.getSettingsManager().isEnabled("checkForUpdates")) {
                    updateCheckerService.checkForUpdatesAsync(false);
                }
                log.info("********[ShortcutBuddy is live and running!]********");
                return null;
            }
        };

        loadTask.setOnSucceeded(event -> splashStage.close());

        progressBar.progressProperty().bind(loadTask.progressProperty());

        new Thread(loadTask).start();
    }

    private void startTrayIcon(){
        trayManager = applicationComponent.getTrayManager();
        notificationService = applicationComponent.getApplicationTrayNotificationService();
        updateCheckerService = applicationComponent.getUpdateCheckerService();
        trayManager.setUpdateCheckerService(updateCheckerService);
        Platform.runLater(() -> {
            trayManager.setSettingsStage(settingsStage);
            trayManager.setShortcutController(shortcutController);
            trayManager.setUserShortcutsStage(userShortcutsStage);
            trayManager.startTray();
        });
    }

    private void initPrimaryStage() {
        Platform.runLater(() -> {
            String chosenTheme = applicationComponent.getSettingsManager().getSetting("theme").value();
            if(!StringUtils.isEmpty(chosenTheme)){
                if(chosenTheme.equals("dark")){
                    Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
                }else if(chosenTheme.equals("light")){
                    Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
                }
            }
            String chosenLanguage = applicationComponent.getSettingsManager().getSetting("language").value();
            Locale locale = Languages.getLocale(chosenLanguage);
            bundle = ResourceBundle.getBundle("i18n/messages", locale);
            primaryStage.setTitle(bundle.getString(Label.APP_TITLE));
            FXMLLoader fxmlLoader = new FXMLLoader(ShortcutBuddyApp.class.getResource("/view/shortcut-view.fxml"), bundle);
            fxmlLoader.setControllerFactory(applicationComponent.getControllerFactory());
            try {
                Scene scene = new Scene(fxmlLoader.load(), Integer.parseInt(applicationComponent.getSettingsManager().getSetting("width").value()), Integer.parseInt(applicationComponent.getSettingsManager().getSetting("height").value()));
                primaryStage.setScene(scene);
                subscribeKeyEvents(fxmlLoader);
                shortcutController = fxmlLoader.getController();
                applicationComponent.inject(shortcutController);
                shortcutController.setBundle(bundle);
                shortcutController.setStage(primaryStage);
            } catch (IOException e) {
                log.error("Error while loading primary stage", e);
                throw new RuntimeException(e);
            }
        });
    }

    private void subscribeKeyEvents(FXMLLoader fxmlLoader){
        keyListener.subscribe(NativeKeyEvent.VC_CONTROL, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_ESCAPE, fxmlLoader.getController());
        int keyCode = NativeKeyEvent.VC_PERIOD;// Default value: .
        if(KeyOption.SPACE.equals(applicationComponent.getSettingsManager().getSetting("searchKey").value())) {
            keyCode = NativeKeyEvent.VC_SPACE;
        } else if(KeyOption.MINUS.equals(applicationComponent.getSettingsManager().getSetting("searchKey").value())) {
            keyCode = NativeKeyEvent.VC_MINUS;
        }
        else if(KeyOption.P.equals(applicationComponent.getSettingsManager().getSetting("searchKey").value())) {
            keyCode = NativeKeyEvent.VC_P;
        }
        keyListener.subscribe(keyCode, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_DOWN, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_UP, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_LEFT, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_RIGHT, fxmlLoader.getController());
        keyListener.subscribe(NativeKeyEvent.VC_ENTER, fxmlLoader.getController());

    }

    private void startBackgroundTasks(){
        ThreadFactory daemonThreadFactory = runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName("ShortcutBuddy-UsageFlushThread");
            thread.setDaemon(true);
            return thread;
        };
        backgroundScheduler = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory);
        Runnable flushTask = () -> {
            log.info("Flushing background tasks");
            try {
                if(shortcutManager != null) {
                    shortcutManager.flushUsageCount();
                }
            } catch (Exception e) {
                log.error("Error flushing usage count", e);
            }
        };
        backgroundScheduler.scheduleAtFixedRate(flushTask, 15, 15, TimeUnit.MINUTES);
        log.info("Background tasks scheduled every 15 minutes");
    }

    private void shutdownScheduler() {
        if (backgroundScheduler != null && !backgroundScheduler.isShutdown()) {
            backgroundScheduler.shutdown();
            try {
                if (!backgroundScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("The background scheduler did not terminate in the specified time.");
                    backgroundScheduler.shutdownNow();
                } else {
                    log.info("Background scheduler successfully terminated.");
                }
            } catch (InterruptedException e) {
                log.warn("Background scheduler interrupted during shutdown.", e);
                backgroundScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}