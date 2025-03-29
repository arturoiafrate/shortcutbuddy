package it.arturoiafrate.shortcutbuddy;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import it.arturoiafrate.shortcutbuddy.controller.ShortcutController;
import it.arturoiafrate.shortcutbuddy.model.constant.Label;
import it.arturoiafrate.shortcutbuddy.model.enumerator.Languages;
import it.arturoiafrate.shortcutbuddy.model.interceptor.foreground.ForegroundAppInterceptor;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyListener;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import it.arturoiafrate.shortcutbuddy.model.manager.shortcut.ShortcutManager;
import it.arturoiafrate.shortcutbuddy.model.manager.tray.TrayManager;
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
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class ShortcutBuddyApp extends Application {
    private Stage primaryStage;
    private Stage splashStage;
    private Stage settingsStage;
    private KeyListener keyListener;
    private TrayIcon trayIcon;
    private ShortcutController shortcutController;
    private ForegroundAppInterceptor foregroundAppInterceptor;
    private ResourceBundle bundle;
    private TrayManager trayManager;
    private static HostServices appHostServices;

    @Override
    public void init() {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    }

    @Override
    public void start(Stage primaryStage) {
        this.appHostServices = getHostServices();
        this.primaryStage = primaryStage;
        this.primaryStage.initStyle(StageStyle.UNDECORATED);
        this.primaryStage.hide();
        Platform.setImplicitExit(false);
        if (!System.getProperty("os.name").toLowerCase().contains("windows") || !SystemTray.isSupported()) {
            throw new RuntimeException(bundle.getString(Label.ERROR_ICONTRAY));
        }
        showSplashScreen();
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
                updateProgress(0, 100);
                Thread.sleep(500);
                SettingsManager.getInstance().load();
                keyListener = new KeyListener();
                foregroundAppInterceptor = new ForegroundAppInterceptor();
                updateProgress(25, 100);
                initPrimaryStage();
                updateProgress(50, 100);
                ShortcutManager.getInstance().load();
                updateProgress(75, 100);
                startTrayIcon();
                updateProgress(100, 100);
                Thread.sleep(100);
                return null;
            }
        };

        loadTask.setOnSucceeded(event -> splashStage.close());

        progressBar.progressProperty().bind(loadTask.progressProperty());

        new Thread(loadTask).start();
    }

    private void startTrayIcon(){
        trayManager = new TrayManager(bundle, appHostServices);
        Platform.runLater(() -> {
            trayManager.setSettingsStage(settingsStage);
            trayManager.setShortcutController(shortcutController);
            trayManager.startTray();
        });
    }

    private void initPrimaryStage() {
        Platform.runLater(() -> {
            String chosenTheme = SettingsManager.getInstance().getSetting("theme").value();
            if(!StringUtils.isEmpty(chosenTheme)){
                if(chosenTheme.equals("dark")){
                    Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
                }else if(chosenTheme.equals("light")){
                    Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
                }
            }
            String chosenLanguage = SettingsManager.getInstance().getSetting("language").value();
            Locale locale = Languages.getLocale(chosenLanguage);
            bundle = ResourceBundle.getBundle("i18n/messages", locale);
            primaryStage.setTitle(bundle.getString(Label.APP_TITLE));
            FXMLLoader fxmlLoader = new FXMLLoader(ShortcutBuddyApp.class.getResource("/view/shortcut-view.fxml"), bundle);
            try {
                Scene scene = new Scene(fxmlLoader.load(), Integer.parseInt(SettingsManager.getInstance().getSetting("width").value()), Integer.parseInt(SettingsManager.getInstance().getSetting("height").value()));
                primaryStage.setScene(scene);
                keyListener.subscribe(NativeKeyEvent.VC_CONTROL, fxmlLoader.getController());
                keyListener.subscribe(NativeKeyEvent.VC_PERIOD, fxmlLoader.getController());
                keyListener.subscribe(NativeKeyEvent.VC_ESCAPE, fxmlLoader.getController());
                shortcutController = fxmlLoader.getController();
                shortcutController.setForegroundAppInterceptor(foregroundAppInterceptor);
                shortcutController.setBundle(bundle);
                shortcutController.setStage(primaryStage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void main(String[] args) {
        launch();
    }
}