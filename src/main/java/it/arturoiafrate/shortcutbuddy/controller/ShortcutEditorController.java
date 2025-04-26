package it.arturoiafrate.shortcutbuddy.controller;

import atlantafx.base.theme.Styles;
import it.arturoiafrate.shortcutbuddy.ShortcutBuddyApp;
import it.arturoiafrate.shortcutbuddy.config.ApplicationComponent;
import it.arturoiafrate.shortcutbuddy.controller.dialog.InlineCSS;
import it.arturoiafrate.shortcutbuddy.controller.dialog.NewAppDialog;
import it.arturoiafrate.shortcutbuddy.model.bean.AppShortcuts;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import it.arturoiafrate.shortcutbuddy.model.manager.shortcut.ShortcutManager;
import jakarta.inject.Inject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

@Slf4j
public class ShortcutEditorController {

    @FXML private ResourceBundle resources;
    @FXML private Label titleLabel;
    @FXML private TilePane appsPane;
    @FXML private Button addButton;

    @Inject ShortcutManager shortcutManager;
    @Inject SettingsManager settingsManager;
    @Inject ApplicationComponent appComponent;


    private ObservableList<AppShortcuts> allAppsList;

    @Inject
    public ShortcutEditorController() {}

    @FXML
    private void initialize(){
        addButton.setGraphic(new FontIcon(Feather.PLUS));
        addButton.getStyleClass().addAll(Styles.ACCENT, Styles.FLAT);
        titleLabel.getStyleClass().addAll(Styles.TITLE_3, Styles.ACCENT);
        loadAndDisplayApplications();
    }

    @FXML
    void handleAddButtonAction() {
        getStage().setOpacity(0.5);
        var newApp = new NewAppDialog(resources, appComponent.getForegroundAppInterceptor(), appComponent.getKeyListener()).showAndWait();//TODO usare la DI
        getStage().setOpacity(1);
        newApp.ifPresent(app -> {
            boolean saved = shortcutManager.addAppShortcuts(app);
            if(saved && !StringUtils.isEmpty(app.getAppIconPath())){
                shortcutManager.copyAppImage(app.getAppIconPath(), app.getAppName());
            }
            loadAndDisplayApplications();
        });
    }

    private void loadAndDisplayApplications() {
        allAppsList = FXCollections.observableArrayList(shortcutManager.getAllApps());
        appsPane.getChildren().clear();
        if(allAppsList.isEmpty()) {
            Label emptyLabel = new Label(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_NO_APP_FOUND_MESSAGE));
            emptyLabel.getStyleClass().add(Styles.TEXT_MUTED);
            appsPane.getChildren().add(emptyLabel);
            return;
        }
        for (AppShortcuts app : allAppsList) {
            Node appNode = createAppViewNode(app);
            appsPane.getChildren().add(appNode);
        }
    }

    private Node createAppViewNode(AppShortcuts appInfo) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setStyle(InlineCSS.SHORTCUT_CONTAINER_BORDER);
        card.setCursor(Cursor.HAND);

        ImageView iconView = new ImageView();
        iconView.setFitHeight(48);
        iconView.setFitWidth(48);
        iconView.setPreserveRatio(true);
        String imagePath = settingsManager.getAppImagePath(appInfo.getAppName());
        File imageFile = new File(imagePath);
        if(!imageFile.exists()){
            imagePath = settingsManager.getAppImagePath("openjdk");
            imageFile = new File(imagePath);
        }
        final Image appIcon = new Image(imageFile.toURI().toString());
        iconView.setImage(appIcon);

        Label nameLabel = new Label(appInfo.getAppDescription());
        nameLabel.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.ACCENT);
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(TextAlignment.CENTER);

        card.getChildren().addAll(iconView, nameLabel);
        card.setOnMouseClicked(event -> navigateToAppShortcuts(appInfo, appIcon));

        return card;
    }

    private void navigateToAppShortcuts(AppShortcuts appInfo, Image appIcon) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/appShortcutEditor-view.fxml"), resources);
            loader.setControllerFactory(appComponent.getControllerFactory());
            Parent root = loader.load();

            AppShortcutEditorController detailController = loader.getController();

            Stage currentStage = (Stage) this.appsPane.getScene().getWindow();

            Stage detailStage = new Stage();
            detailStage.setTitle(MessageFormat.format(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_EDITOR_TITLE), appInfo.getAppDescription()));
            detailStage.initModality(Modality.WINDOW_MODAL);
            detailStage.initOwner(currentStage);

            Scene detailScene = new Scene(root);
            detailStage.setScene(detailScene);
            detailController.initData(appInfo, appIcon);

            // Register the detail stage with the application
            ShortcutBuddyApp.getInstance().registerStage(detailStage);

            currentStage.setOpacity(0.5);
            detailStage.showAndWait();
            currentStage.setOpacity(1);

        } catch (Exception e) {
            log.error("Error loading AppShortcutEditor-view.fxml", e);
            Alert errorAlert = new Alert(Alert.AlertType.ERROR, resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_ERROR_MESSAGE));

            // Register the alert's stage with the application
            errorAlert.setOnShown(event -> {
                Stage alertStage = (Stage) errorAlert.getDialogPane().getScene().getWindow();
                ShortcutBuddyApp.getInstance().registerStage(alertStage);
            });

            errorAlert.showAndWait();
        }
    }

    private Stage getStage() {
        return (Stage) appsPane.getScene().getWindow();
    }
}
