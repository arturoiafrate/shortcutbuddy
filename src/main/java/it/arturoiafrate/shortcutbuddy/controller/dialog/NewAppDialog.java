package it.arturoiafrate.shortcutbuddy.controller.dialog;

import atlantafx.base.theme.Styles;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import it.arturoiafrate.shortcutbuddy.model.bean.AppShortcuts;
import it.arturoiafrate.shortcutbuddy.model.interceptor.foreground.ForegroundAppInterceptor;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.IKeyObserver;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyListener;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyOperation;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;
import java.util.ResourceBundle;

@Slf4j
public class NewAppDialog extends Dialog<AppShortcuts> implements IKeyObserver {
    private Stage stage;
    private final ResourceBundle resources;
    private Button imagePickerButton;
    private final ForegroundAppInterceptor appInterceptor;
    private final KeyListener keyListener;
    private Label pathLabel;
    private Label appPicker;
    private Button clearAppButton;
    private TextField appDescriptionField;
    private Button saveButton;


    public NewAppDialog(ResourceBundle resources, ForegroundAppInterceptor appInterceptor, KeyListener keyListener) {
        this.resources = resources;
        this.appInterceptor = appInterceptor;
        this.keyListener = keyListener;
        this.keyListener.subscribe(NativeKeyEvent.VC_W, this);
        configureDialog();
        VBox mainLayout = createMainLayout();
        HBox row1 = createFirstRow();
        HBox row2 = createSecondRow();
        HBox buttonBar = getButtonBar();
        mainLayout.getChildren().addAll(row1, row2, buttonBar);
        getDialogPane().setContent(mainLayout);
    }

    private void configureDialog() {
        setTitle(this.resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_NEWAPPDIALOG_NEWAPP_TITLE));
        setResizable(false);
        this.stage = (Stage) getDialogPane().getScene().getWindow();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.getIcons().add(getApplicationIcon());
        stage.setOnCloseRequest(e -> this.hide());
        setGraphic(null);
        getDialogPane().setMinWidth(450);
        getDialogPane().setMinHeight(300);
        getDialogPane().setPrefWidth(450);
        getDialogPane().setPrefHeight(300);
    }

    private VBox createMainLayout() {
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20, 30, 20, 30));
        mainLayout.setAlignment(Pos.TOP_CENTER);
        return mainLayout;
    }

    private HBox createFirstRow() {
        HBox row1 = new HBox(10);
        row1.setAlignment(Pos.BASELINE_LEFT);
        row1.setPadding(new Insets(40, 0, 25, 45));

        imagePickerButton = createImagePickerButton();
        VBox imagePickerLayout = createImagePickerLayout();

        row1.getChildren().addAll(imagePickerButton, imagePickerLayout);
        return row1;
    }

    private HBox createSecondRow() {
        HBox row2 = new HBox(30);
        row2.setAlignment(Pos.BASELINE_CENTER);

        appDescriptionField = new TextField();
        appDescriptionField.setPromptText(this.resources.getString("dialog.newApp.appDescription"));
        appDescriptionField.setMinWidth(300);
        appDescriptionField.setMaxWidth(300);
        appDescriptionField.getStyleClass().addAll(Styles.LARGE, Styles.TEXT_ITALIC, Styles.TEXT_MUTED);
        appDescriptionField.setEditable(false);
        appDescriptionField.textProperty().addListener((observable, oldValue, newValue) -> saveButton.setDisable(StringUtils.isEmpty(newValue)));
        row2.getChildren().add(appDescriptionField);
        return row2;
    }

    private HBox getButtonBar() {
        HBox buttonBar = new HBox();
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(20, 0, 0, 400));

        saveButton = new Button();
        saveButton.setDefaultButton(true);
        saveButton.setGraphic(new FontIcon(Feather.SAVE));
        saveButton.getStyleClass().addAll(Styles.SUCCESS, Styles.FLAT, Styles.BUTTON_ICON);
        saveButton.setOnAction(event -> {
            AppShortcuts appShortcuts = new AppShortcuts();
            appShortcuts.setAppName(appPicker.getText());
            appShortcuts.setAppDescription(appDescriptionField.getText());
            if(!StringUtils.isEmpty(pathLabel.getText()) && pathLabel.getText().equals(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_NEWAPPDIALOG_NEWAPP_IMAGEPATH)){
                appShortcuts.setAppIconPath(pathLabel.getText());
            }
            this.setResult(appShortcuts);
        });
        saveButton.setDisable(true);
        buttonBar.getChildren().add(saveButton);
        return buttonBar;
    }

    private Button createImagePickerButton() {
        Button imagePickerButton = new Button();
        imagePickerButton.setGraphic(new FontIcon(Feather.IMAGE));
        imagePickerButton.setMinWidth(75);
        imagePickerButton.setMinHeight(75);
        imagePickerButton.setMaxWidth(75);
        imagePickerButton.setMaxHeight(75);
        imagePickerButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.BUTTON_ICON);
        imagePickerButton.setOnAction(event -> this.choosePngFile());
        return imagePickerButton;
    }

    private VBox createImagePickerLayout() {
        VBox imagePickerLayout = new VBox(5);
        imagePickerLayout.setAlignment(Pos.TOP_LEFT);

        pathLabel = new Label(this.resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_NEWAPPDIALOG_NEWAPP_IMAGEPATH));
        pathLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_ITALIC, Styles.TEXT_MUTED);

        HBox appPickerLayout = new HBox(5);
        appPickerLayout.setAlignment(Pos.BOTTOM_LEFT);
        appPickerLayout.setFillHeight(true);
        appPickerLayout.setMaxWidth(Double.MAX_VALUE);

        appPicker = new Label();
        appPicker.setText(this.resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_NEWAPPDIALOG_NEWAPP_APPPICKER));
        appPicker.getStyleClass().addAll(Styles.TEXT_ITALIC, Styles.TEXT_MUTED);
        appPicker.setStyle(InlineCSS.SHORTCUT_BORDER);
        appPicker.setMaxWidth(Double.MAX_VALUE);

        clearAppButton = new Button();
        clearAppButton.setDisable(true);
        clearAppButton.setGraphic(new FontIcon(Feather.TRASH_2));
        clearAppButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.BUTTON_ICON);
        clearAppButton.setOnAction(event -> {
            this.keyListener.subscribe(NativeKeyEvent.VC_W, this);
            appPicker.setText(this.resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_NEWAPPDIALOG_NEWAPP_APPPICKER));
            clearAppButton.setDisable(true);
            appDescriptionField.setText("");
            appDescriptionField.setPromptText(this.resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_NEWAPPDIALOG_NEWAPP_DESCRIPTION));
            appDescriptionField.setEditable(false);
        });

        HBox.setHgrow(appPicker, Priority.ALWAYS);
        appPickerLayout.getChildren().addAll(appPicker, clearAppButton);

        imagePickerLayout.getChildren().addAll(pathLabel, appPickerLayout);
        return imagePickerLayout;
    }



    private javafx.scene.image.Image getApplicationIcon() {
        try {
            return new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png")));
        } catch (Exception e) {
            log.error("Impossibile caricare l'icona dell'applicazione /images/logo_128.png", e);
        }
        return null;
    }

    private void choosePngFile() {
        try {
            var fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle(this.resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_NEWAPPDIALOG_NEWAPP_IMAGEPICKER));
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png"));
            var selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                pathLabel.setText(selectedFile.getAbsolutePath());
                var imageView = new javafx.scene.image.ImageView(selectedFile.toURI().toString());
                imageView.setFitHeight(65);
                imageView.setFitWidth(65);
                imagePickerButton.setGraphic(imageView);
            }
        } catch (Exception e) {
            log.error("Error while opening file chooser", e);
        }
    }

    @Override
    public void update(int keyCode, KeyOperation mode) {
        Platform.runLater(() -> {
            if(keyCode == NativeKeyEvent.VC_W && mode == KeyOperation.KEY_PRESS) {
                appPicker.setText(appInterceptor.getForegroundAppName().toLowerCase());
                clearAppButton.setDisable(false);
                appDescriptionField.setEditable(true);
                keyListener.unsubscribe(NativeKeyEvent.VC_W, this);
                if(stage.isIconified()) stage.setIconified(false);
                stage.toFront();
                stage.requestFocus();
                appDescriptionField.setText("");
                appDescriptionField.pseudoClassStateChanged(Styles.STATE_WARNING, true);
            }
        });
    }
}
