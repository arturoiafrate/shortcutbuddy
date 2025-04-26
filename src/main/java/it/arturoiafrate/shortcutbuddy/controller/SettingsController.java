package it.arturoiafrate.shortcutbuddy.controller;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import it.arturoiafrate.shortcutbuddy.ShortcutBuddyApp;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import java.util.*;

import it.arturoiafrate.shortcutbuddy.model.bean.Setting;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public class SettingsController {

    @FXML private ResourceBundle resources;
    @FXML private GridPane settingsGrid;

    private final Map<String, Control> settingFields = new HashMap<>();
    private List<Setting> currentSettings;

    @Inject
    SettingsManager settingsManager;

    @Inject
    public SettingsController(){

    }

    @FXML
    public void initialize() {
        this.currentSettings = settingsManager.getSettingsAll();
        Platform.runLater(() -> setSettingsData(currentSettings));
    }

    private void setSettingsData(List<Setting> settingsList) {

        settingsGrid.getChildren().clear();
        settingsGrid.getColumnConstraints().clear();
        settingFields.clear();

        for (int i = 0; i < settingsList.size(); i++) {
            Setting setting = settingsList.get(i);
            if(!setting.isHide()){
                Label keyLabel = new Label(resources.getString("settings.setting."+setting.key()) + ":");
                keyLabel.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_BOLD);
                keyLabel.setAlignment(Pos.CENTER_LEFT);
                keyLabel.setWrapText(true);

                FontIcon infoIcon = new FontIcon(Feather.INFO);
                infoIcon.setIconSize(14);
                infoIcon.getStyleClass().add(Styles.TEXT_SUBTLE);
                keyLabel.setGraphic(infoIcon);
                keyLabel.setContentDisplay(ContentDisplay.RIGHT);
                keyLabel.setGraphicTextGap(5);
                Tooltip tooltip = new Tooltip(resources.getString("settings.setting."+setting.key()+".tooltip"));
                tooltip.setWrapText(true);
                tooltip.setMaxWidth(300);
                Tooltip.install(infoIcon, tooltip);

                Control valueField;
                if (setting.options() == null) {
                    TextField valueTextField = new TextField(setting.value());
                    valueTextField.setEditable(!setting.readonly());
                    valueTextField.setMaxWidth(Double.MAX_VALUE);
                    valueField = valueTextField;
                } else {
                    if(setting.options().length == 2 && "y".equals(setting.options()[0]) && "n".equals(setting.options()[1])){
                        ToggleSwitch toggleSwitch = new ToggleSwitch();
                        toggleSwitch.setLabelPosition(HorizontalDirection.RIGHT);
                        toggleSwitch.setSelected("y".equals(setting.value()));
                        valueField = toggleSwitch;
                    } else {
                        ComboBox<String> comboBox = new ComboBox<>();
                        comboBox.setItems(FXCollections.observableArrayList(setting.options()));
                        comboBox.setValue(setting.value());
                        comboBox.setDisable(setting.readonly());
                        valueField = comboBox;
                    }
                }


                settingsGrid.add(keyLabel, 0, i);
                settingsGrid.add(valueField, 1, i);

                settingFields.put(setting.key(), valueField);
            }
        }
    }

    @FXML
    void handleSaveSettings(ActionEvent event) {
        List<Setting> newSettings = new ArrayList<>();
        for (Map.Entry<String, Control> entry : settingFields.entrySet()) {
            String key = entry.getKey();
            Control control = entry.getValue();
            Setting currentSetting = settingsManager.getSetting(key);
            String newValue = "";
            if (control instanceof TextField textField) {
                newValue =textField.getText();
            } else if (control instanceof ComboBox<?> comboBox) {
                newValue = (String) comboBox.getValue();
            } else if (control instanceof ToggleSwitch toggleSwitch) {
                newValue=toggleSwitch.isSelected() ? "y" : "n";
            }
            newSettings.add(new Setting(currentSetting.key(), newValue, currentSetting.readonly(), currentSetting.options(), currentSetting.isHide(), currentSetting.order()));
        }
        boolean isSaved = settingsManager.save(newSettings);
        Alert alert = isSaved ? new Alert(Alert.AlertType.INFORMATION) : new Alert(Alert.AlertType.ERROR);
        String title = isSaved ? resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_SUCCESS_TITLE) : resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_ERROR_TITLE);
        String message = isSaved ? resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_SUCCESS_TEXT) : resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_ERROR_TEXT);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Stage owner = (Stage) settingsGrid.getScene().getWindow();
        alert.initOwner(owner);

        // Register the alert's stage with the application
        alert.setOnShown(e -> {
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            ShortcutBuddyApp.getInstance().registerStage(alertStage);
        });

        alert.showAndWait();

        closeWindow();

    }

    private void closeWindow() {
        // Usiamo settingsGrid per ottenere la scena e quindi la finestra (Stage)
        Stage stage = (Stage) settingsGrid.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
