package it.arturoiafrate.shortcutbuddy.model.manager.tray;

import it.arturoiafrate.shortcutbuddy.ShortcutBuddyApp;
import it.arturoiafrate.shortcutbuddy.controller.ShortcutController;
import it.arturoiafrate.shortcutbuddy.controller.dialog.DialogUtils;
import it.arturoiafrate.shortcutbuddy.model.bean.ReleaseInfo;
import it.arturoiafrate.shortcutbuddy.model.constant.Label;
import it.arturoiafrate.shortcutbuddy.service.impl.ChangelogService;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import it.arturoiafrate.shortcutbuddy.service.INotificationService;
import it.arturoiafrate.shortcutbuddy.service.IUpdateCheckerService;
import it.arturoiafrate.shortcutbuddy.utility.AppInfo;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@Slf4j
public class TrayManager implements INotificationService {
    private final ResourceBundle bundle;
    private static HostServices appHostServices;
    @Setter
    private Stage settingsStage;
    @Setter
    private Stage userShortcutsStage;
    @Setter
    private ShortcutController shortcutController;
    @Setter
    private IUpdateCheckerService updateCheckerService;
    private TrayIcon trayIcon;
    private javafx.scene.image.Image appIcon;
    private final ChangelogService changelogService;

    public TrayManager(ResourceBundle bundle, HostServices appHostServices) {
        this.bundle = bundle;
        TrayManager.appHostServices = appHostServices;
        changelogService = new ChangelogService();
    }

    public void startTray(){
        if(!SystemTray.isSupported()){
            throw new RuntimeException("SystemTray not supported");
        }
        Image image = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/images/logo_128.png"));
        MenuItem settingsItem = new MenuItem(bundle.getString(Label.BUTTON_SETTINGS));
        MenuItem checkForUpdatesItem = new MenuItem(bundle.getString(Label.SETTINGS_SETTING_CHECKFORUPDATES));
        MenuItem aboutItem = new MenuItem(bundle.getString(Label.BUTTON_ABOUT));
        MenuItem changelogItem = new MenuItem(bundle.getString(Label.BUTTON_CHANGELOG));
        MenuItem exitItem = new MenuItem(bundle.getString(Label.BUTTON_EXIT));
        MenuItem userShortcutsItem = new MenuItem(bundle.getString(Label.BUTTON_USERSHORTCUTS));

        settingsItem.addActionListener(e -> Platform.runLater(this::openSettingsWindow));
        aboutItem.addActionListener(e -> Platform.runLater(this::showAboutDialog));
        changelogItem.addActionListener(e -> Platform.runLater(this::showChangelogAction));
        checkForUpdatesItem.addActionListener(e -> Platform.runLater(this::checkForUpdates));
        userShortcutsItem.addActionListener(e -> Platform.runLater(this::openUserShortcutsWindow));
        exitItem.addActionListener(e -> exitTray());

        PopupMenu popup = new PopupMenu();
        //TODO
        //popup.add(userShortcutsItem);
        popup.add(settingsItem);
        popup.addSeparator();
        popup.add(checkForUpdatesItem);
        popup.add(changelogItem);
        popup.add(aboutItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, bundle.getString(Label.APP_TITLE), popup);
        trayIcon.setImageAutoSize(true);

        try {
            SystemTray.getSystemTray().add(trayIcon);
            showNotification(bundle.getString(Label.NOTIFICATION_APPSTARTED_TITLE), bundle.getString(Label.NOTIFICATION_APPSTARTED_TEXT), TrayIcon.MessageType.NONE);
        } catch (AWTException e) {
            log.error("Error adding tray icon", e);
            throw new RuntimeException(bundle.getString(Label.ERROR_ICONTRAY));
        }
    }

    private void checkForUpdates() {
        if (updateCheckerService != null) {
            updateCheckerService.checkForUpdatesAsync(true);
        } else {
            //TODO show notification
            log.error("UpdateCheckerService non inizializzato.");
        }
    }

    public void exitTray(){
        SystemTray.getSystemTray().remove(trayIcon);
        trayIcon = null;
        Platform.runLater(Platform::exit);
    }

    private javafx.scene.image.Image getApplicationIcon() {
        if (appIcon == null) {
            try {
                appIcon = new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png")));
            } catch (Exception e) {
                log.error("Impossibile caricare l'icona dell'applicazione /images/logo_128.png", e);
                // Restituisci null o un'icona di default
            }
        }
        return appIcon;
    }

    @Override
    public void showNotification(String caption, String text, TrayIcon.MessageType messageType) {
        if(!SettingsManager.getInstance().isEnabled("enableNotification") && !messageType.equals(TrayIcon.MessageType.ERROR)){
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (trayIcon != null) {
                trayIcon.displayMessage(caption, text, messageType);
            }
        });
    }

    @Override
    public void showDialog(String caption, String text, Consumer<ActionEvent> action){
        var textArea = new javafx.scene.control.TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setText(text);
        DialogUtils.showActionDialog(caption, textArea, Optional.of(this.getApplicationIcon()), Optional.empty(), action);
    }

    private void showChangelogAction() {
        String title = MessageFormat.format(bundle.getString(Label.CHANGELOG_WINDOW_TITLE), AppInfo.getName(), AppInfo.getVersion());
        var notes = changelogService.getNotesForVersion(AppInfo.getVersion());
        var changelogTextArea = new javafx.scene.control.TextArea();
        changelogTextArea.setEditable(false);
        changelogTextArea.setWrapText(true);

        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(MessageFormat.format(
                bundle.getString(Label.CHANGELOG_WINDOW_RELEASEDATE),
                changelogService.getReleaseDateForVersion(AppInfo.getVersion())
        )).append("\n");
        contentBuilder.append(bundle.getString(Label.CHANGELOG_WINDOW_WHATSNEW)).append("\n\n");
        if(notes == null || notes.isEmpty()){
            contentBuilder.append("  ").append(bundle.getString(Label.CHANGELOG_WINDOW_NOCHANGELOG)).append("\n");
        } else {
            for (String note : notes) {
                contentBuilder.append("• ").append(note.trim()).append("\n");
            }
        }
        var lastReleases = changelogService.getLastReleases(3, AppInfo.getVersion());
        if(lastReleases != null && !lastReleases.isEmpty()){
            contentBuilder.append("\n").append(bundle.getString(Label.CHANGELOG_WINDOW_LASTRELEASES)).append("\n\n");
            for(var release : lastReleases) {
                contentBuilder.append(MessageFormat.format(
                    bundle.getString(Label.CHANGELOG_WINDOW_GENERICRELEASE),
                    release.version(),
                    release.date()
                )).append("\n");
                if(release.notes() == null || release.notes().isEmpty()){
                    contentBuilder.append("  ").append(bundle.getString(Label.CHANGELOG_WINDOW_NOCHANGELOG)).append("\n");
                } else {
                    for(String note : release.notes()){
                        contentBuilder.append("• ").append(note.trim()).append("\n");
                    }
                }
                contentBuilder.append("\n");
            }
        }

        changelogTextArea.setText(contentBuilder.toString());
        changelogTextArea.setPrefRowCount(15);
        changelogTextArea.setStyle("-fx-font-family: 'monospace';");
        DialogUtils.showInfoDialog(title, changelogTextArea, Optional.of(this.getApplicationIcon()), Optional.empty());
    }

    private void showAboutDialog() {

        String appName = AppInfo.getName();
        String title = MessageFormat.format(bundle.getString(Label.ABOUT_WINDOW_PREFIX), AppInfo.getName());
        HBox content = new HBox(2);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().add(new javafx.scene.image.ImageView(Objects.requireNonNull(this.getClass().getResource("/images/logo_128.png")).toString()));
        VBox aboutContent = new VBox(5);
        aboutContent.setPadding(new Insets(10));
        aboutContent.setAlignment(Pos.CENTER_LEFT);
        String messageContent = MessageFormat.format(
                bundle.getString(Label.ABOUT_WINDOW_MESSAGE),
                appName,
                AppInfo.getVersion(),
                AppInfo.getDeveloper(),
                AppInfo.getLicense());
        var aboutLabel = new javafx.scene.control.Label(messageContent);
        aboutLabel.setWrapText(true);
        aboutContent.getChildren().add(aboutLabel);
        Hyperlink githubLink = new Hyperlink(AppInfo.getGithubUrl());
        githubLink.setOnAction(e -> {
            if (appHostServices != null) {
                appHostServices.showDocument(AppInfo.getGithubUrl());
            }
        });
        aboutContent.getChildren().add(githubLink);
        content.getChildren().add(aboutContent);
        DialogUtils.showInfoDialog(title, content, Optional.of(this.getApplicationIcon()), Optional.empty());
    }

    private void openUserShortcutsWindow(){
        try {
            if(userShortcutsStage != null && userShortcutsStage.isShowing()){
                userShortcutsStage.toFront();
                return;
            }
            FXMLLoader fxmlLoader = new FXMLLoader(ShortcutBuddyApp.class.getResource("/view/userShortcuts-view.fxml"), bundle);
            userShortcutsStage = new Stage();
            userShortcutsStage.setTitle(bundle.getString(Label.USER_SHORTCUTS_TITLE));
            userShortcutsStage.getIcons().add(this.getApplicationIcon());
            Scene userShortcutsScene = new Scene(fxmlLoader.load(), Integer.parseInt(SettingsManager.getInstance().getSetting("width").value()), Integer.parseInt(SettingsManager.getInstance().getSetting("height").value()));
            userShortcutsStage.setScene(userShortcutsScene);
            userShortcutsStage.initModality(Modality.APPLICATION_MODAL);
            userShortcutsStage.setOnCloseRequest( event -> {
                shortcutController.setUserShortcutsShown(false);
                userShortcutsStage = null;
            });
            shortcutController.setUserShortcutsShown(true);
            userShortcutsStage.sizeToScene();
            userShortcutsStage.showAndWait();
        } catch (Exception e) {
            log.error("Error while opening user shortcuts window", e);
            userShortcutsStage = null;
            throw new RuntimeException(e);
        }
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
            settingsStage.getIcons().add(this.getApplicationIcon());
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
            log.error("Error while opening settings window", e);
            settingsStage = null;
            throw new RuntimeException(e);
        }
    }
}
