package it.arturoiafrate.shortcutbuddy.controller;

import atlantafx.base.theme.Styles;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import it.arturoiafrate.shortcutbuddy.config.ApplicationComponent;
import it.arturoiafrate.shortcutbuddy.controller.dialog.DialogUtils;
import it.arturoiafrate.shortcutbuddy.controller.dialog.InlineCSS;
import it.arturoiafrate.shortcutbuddy.model.bean.AppShortcuts;
import it.arturoiafrate.shortcutbuddy.model.bean.Shortcut;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.IKeyObserver;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyListener;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyOperation;
import jakarta.inject.Inject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class AppShortcutEditorDialogController implements IKeyObserver {

    @FXML private ResourceBundle resources;
    @FXML private ImageView appIcon;
    @FXML private Label appNameDescription;
    @FXML private Label keyLabel;
    @FXML private Label shortcutDescriptionLabel;
    @FXML private TextField descriptionField;
    @FXML private Label shortcutCategoryLabel;
    @FXML private TextField categoryField;
    @FXML private Label recordingInfoLabel;
    @FXML private Button recordKeysButton;
    @FXML private HBox keyDisplayBox;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    @Inject ApplicationComponent appComponent;

    private AppShortcuts currentApp;
    private Shortcut currentShortcut;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private ObservableList<Node> keyDisplayBoxChildren;
    private KeyListener keyListener;
    private Consumer<Shortcut> onSaveCallback;

    @Inject
    public AppShortcutEditorDialogController() {}

    @FXML
    private void initialize() {
        appNameDescription.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_BOLD, Styles.TITLE_4);
        recordingInfoLabel.getStyleClass().addAll(Styles.TEXT_ITALIC, Styles.TEXT_SMALL, Styles.DANGER);
        keyLabel.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_SUBTLE, Styles.TEXT_BOLD);
        keyLabel.setStyle(InlineCSS.FONT_MONOSPACE);
        shortcutDescriptionLabel.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_SUBTLE, Styles.TEXT_BOLD);
        shortcutDescriptionLabel.setStyle(InlineCSS.FONT_MONOSPACE);
        shortcutCategoryLabel.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_SUBTLE, Styles.TEXT_BOLD);
        shortcutCategoryLabel.setStyle(InlineCSS.FONT_MONOSPACE);
        okButton.setGraphic(new FontIcon(Feather.PLUS_CIRCLE));
        okButton.getStyleClass().addAll(Styles.SUCCESS, Styles.FLAT, Styles.BUTTON_ICON);
        cancelButton.setGraphic(new FontIcon(Feather.X_CIRCLE));
        cancelButton.getStyleClass().addAll(Styles.DANGER, Styles.FLAT, Styles.BUTTON_ICON);
        recordKeysButton.setGraphic(new FontIcon(Feather.MIC));
        recordKeysButton.getStyleClass().addAll(Styles.ACCENT, Styles.FLAT, Styles.BUTTON_ICON);
        keyDisplayBox.getChildren().clear();
        descriptionField.setStyle(InlineCSS.FONT_MONOSPACE);
        categoryField.setStyle(InlineCSS.FONT_MONOSPACE);
        keyListener = appComponent.getKeyListener();
    }


    public void initModalWindow(AppShortcuts currentApp, Image appImage, Shortcut shortcut, Consumer<Shortcut> onSaveCallback) {
        this.currentApp = currentApp;
        this.currentShortcut = shortcut;
        this.appIcon.setImage(appImage);
        this.onSaveCallback = onSaveCallback;
        appNameDescription.setText(currentApp.getAppDescription());
        if(!StringUtils.isEmpty(shortcut.getDescription())){
            descriptionField.setText(shortcut.getDescription());
        }
        descriptionField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) stopRecording();
        });
        if(!StringUtils.isEmpty(shortcut.getCategory())){
            categoryField.setText(shortcut.getCategory());
        }
        categoryField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) stopRecording();
        });
        if(shortcut.getKeys() != null && !shortcut.getKeys().isEmpty()){
            for (String key : shortcut.getKeys()) {
                addKeyLabel(key);
            }
        }
        categoryField.getScene().getWindow().setOnCloseRequest(event -> {
            keyListener.unsubscribe(KeyListener.KEY_ALL, this);
        });
    }

    private void addKeyLabel(String keyName){
        Label keyLabel = new Label(keyName);
        keyLabel.setStyle(InlineCSS.SHORTCUT_BORDER);
        keyLabel.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.ACCENT);
        keyDisplayBox.getChildren().add(keyLabel);
    }


    @FXML
    public void recordButtonPress(){
        if(isRecording.get()){
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void stopRecording(){
        keyListener.unsubscribe(KeyListener.KEY_ALL, this);
        isRecording.set(false);
        if(keyDisplayBox.getChildren().isEmpty() && keyDisplayBoxChildren != null){
            keyDisplayBox.getChildren().addAll(FXCollections.observableArrayList(keyDisplayBoxChildren));
            keyDisplayBoxChildren.clear();
        }
        recordingInfoLabel.setVisible(false);
    }

    private void startRecording(){
        keyListener.subscribe(KeyListener.KEY_ALL, this);
        isRecording.set(true);
        keyDisplayBoxChildren = FXCollections.observableArrayList(keyDisplayBox.getChildren());
        keyDisplayBox.getChildren().clear();
        recordingInfoLabel.setVisible(true);
    }

    @Override
    public void update(int keyCode, KeyOperation mode) {
        if(!(mode == KeyOperation.KEY_PRESS)){
            return;
        }
        if(keyCode == NativeKeyEvent.VC_ESCAPE){
            stopRecording();
            return;
        }
        String key = KeyListener.getKeyName(keyCode);
        addKeyLabel(key);
    }

    @FXML
    public void cancelButtonPressed(){
        keyListener.unsubscribe(KeyListener.KEY_ALL, this);
        ((Stage)cancelButton.getScene().getWindow()).close();
    }

    @FXML
    public void saveButtonPressed(){
        if(keyDisplayBox.getChildren().isEmpty()){
            String caption = resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_DIALOG_ERROR_TITLE);
            var textArea = new javafx.scene.control.TextArea();
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setText(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_DIALOG_ERROR_SHORTCUTEMPTY));
            DialogUtils.showInfoDialog(caption, textArea, Optional.ofNullable(this.getApplicationIcon()), Optional.of((okButton.getScene().getWindow())));
            return;
        }
        currentShortcut.setDescription(descriptionField.getText());
        currentShortcut.setCategory(categoryField.getText());
        currentShortcut.setKeys(keyDisplayBox.getChildren().stream()
                .map(node -> ((Label) node).getText())
                .toList());
        onSaveCallback.accept(currentShortcut);
        keyListener.unsubscribe(KeyListener.KEY_ALL, this);
        ((Stage)okButton.getScene().getWindow()).close();
    }

    private javafx.scene.image.Image getApplicationIcon() {
        try {
            return new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png")));
        } catch (Exception e) {
            log.error("Impossibile caricare l'icona dell'applicazione /images/logo_128.png", e);
        }
        return null;
    }
}
