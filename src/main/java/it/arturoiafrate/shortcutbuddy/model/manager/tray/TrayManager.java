package it.arturoiafrate.shortcutbuddy.model.manager.tray;

import it.arturoiafrate.shortcutbuddy.ShortcutBuddyApp;
import it.arturoiafrate.shortcutbuddy.controller.ShortcutController;
import it.arturoiafrate.shortcutbuddy.model.constant.Label;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import it.arturoiafrate.shortcutbuddy.utility.AppInfo;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class TrayManager {
    private final ResourceBundle bundle;
    private static HostServices appHostServices;
    private Stage settingsStage;
    private ShortcutController shortcutController;
    private TrayIcon trayIcon;

    public TrayManager(ResourceBundle bundle, HostServices appHostServices) {
        this.bundle = bundle;
        this.appHostServices = appHostServices;
    }
    public void setShortcutController(ShortcutController shortcutController) {
        this.shortcutController = shortcutController;
    }
    public void setSettingsStage(Stage settingsStage) {
        this.settingsStage = settingsStage;
    }

    public void startTray(){
        if(!SystemTray.isSupported()){
            throw new RuntimeException("SystemTray not supported");
        }
        Image image = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/images/logo_128.png"));
        MenuItem settingsItem = new MenuItem(bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.BUTTON_SETTINGS));
        MenuItem aboutItem = new MenuItem(bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.BUTTON_ABOUT));
        MenuItem exitItem = new MenuItem(bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.BUTTON_EXIT));

        settingsItem.addActionListener(e -> Platform.runLater(this::openSettingsWindow));
        aboutItem.addActionListener(e -> Platform.runLater(this::showAboutDialog));
        exitItem.addActionListener(e -> {
            SystemTray.getSystemTray().remove(trayIcon);
            Platform.runLater(Platform::exit);
        });

        PopupMenu popup = new PopupMenu();
        popup.add(settingsItem);
        popup.addSeparator();
        popup.add(aboutItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.APP_TITLE), popup);
        trayIcon.setImageAutoSize(true);

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            throw new RuntimeException(bundle.getString(Label.ERROR_ICONTRAY));
        }
    }

    private void showAboutDialog() {

        String messageContent = MessageFormat.format(
                bundle.getString(Label.ABOUT_WINDOW_MESSAGE),
                AppInfo.getName(),
                AppInfo.getVersion(),
                AppInfo.getDeveloper(),
                AppInfo.getLicense());

        Alert aboutAlert = new Alert(Alert.AlertType.INFORMATION);
        aboutAlert.setTitle(MessageFormat.format(bundle.getString(Label.ABOUT_WINDOW_PREFIX), AppInfo.getName()));
        aboutAlert.setHeaderText(null);
        VBox content = new VBox(5);
        content.setPadding(new Insets(10));
        javafx.scene.control.Label aboutLabel = new javafx.scene.control.Label(messageContent);
        content.getChildren().add(aboutLabel);
        Hyperlink githubLink = new Hyperlink(AppInfo.getGithubUrl());
        githubLink.setOnAction(e -> {
            if (appHostServices != null) {
                appHostServices.showDocument(AppInfo.getGithubUrl());
            }
        });
        content.getChildren().add(githubLink);
        DialogPane dialogPane = aboutAlert.getDialogPane();
        dialogPane.setContent(content);
        aboutAlert.showAndWait();
    }


    private void openSettingsWindow(){
        try{
            if (settingsStage != null && settingsStage.isShowing()) {
                settingsStage.toFront();
                return;
            }
            FXMLLoader fxmlLoader = new FXMLLoader(ShortcutBuddyApp.class.getResource("/view/settings-view.fxml"), bundle);
            settingsStage = new Stage();
            settingsStage.setTitle(bundle.getString(Label.SETTINGS_TITLE));
            Scene settingsScene = new Scene(fxmlLoader.load(), Integer.parseInt(SettingsManager.getInstance().getSetting("width").value()), Integer.parseInt(SettingsManager.getInstance().getSetting("height").value()));
            settingsStage.setScene(settingsScene);
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.setOnCloseRequest(event -> {
                shortcutController.setSettingsShown(false);
                settingsStage = null;
            });
            shortcutController.setSettingsShown(true);
            settingsStage.sizeToScene();
            settingsStage.showAndWait();
        } catch (Exception e) {
            settingsStage = null;
            throw new RuntimeException(e);
        }
    }
}
