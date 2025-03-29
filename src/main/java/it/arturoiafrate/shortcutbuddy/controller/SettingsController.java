package it.arturoiafrate.shortcutbuddy.controller;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
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

public class SettingsController {

    @FXML private ResourceBundle resources;
    @FXML private GridPane settingsGrid; // Inietta la GridPane

    private final Map<String, Control> settingFields = new HashMap<>();
    private List<Setting> currentSettings;

    @FXML
    public void initialize() {
        this.currentSettings = SettingsManager.getInstance().getSettingsAll();
        Platform.runLater(() -> setSettingsData(currentSettings));
    }

    private void setSettingsData(List<Setting> settingsList) {

        settingsGrid.getChildren().clear();
        settingsGrid.getColumnConstraints().clear();
        settingFields.clear();

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(15);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(25);
        settingsGrid.getColumnConstraints().addAll(col1, col2);

        // Popola la griglia
        for (int i = 0; i < settingsList.size(); i++) {
            Setting setting = settingsList.get(i);

            Label keyLabel = new Label(setting.key() + ":");
            keyLabel.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_BOLD);
            keyLabel.setAlignment(Pos.CENTER_LEFT);

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

    @FXML
    void handleSaveSettings(ActionEvent event) {
        List<Setting> newSettings = new ArrayList<>();
        for (Map.Entry<String, Control> entry : settingFields.entrySet()) {
            String key = entry.getKey();
            Control control = entry.getValue();
            Setting currentSetting = SettingsManager.getInstance().getSetting(key);
            String newValue = "";
            if (control instanceof TextField textField) {
                newValue =textField.getText();
            } else if (control instanceof ComboBox<?> comboBox) {
                newValue = (String) comboBox.getValue();
            } else if (control instanceof ToggleSwitch toggleSwitch) {
                newValue=toggleSwitch.isSelected() ? "y" : "n";
            }
            newSettings.add(new Setting(currentSetting.key(), newValue, currentSetting.readonly(), currentSetting.options()));
        }
        boolean isSaved = SettingsManager.getInstance().save(newSettings);
        Alert alert = isSaved ? new Alert(Alert.AlertType.INFORMATION) : new Alert(Alert.AlertType.ERROR);
        String title = isSaved ? resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_SUCCESS_TITLE) : resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_ERROR_TITLE);
        String message = isSaved ? resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_SUCCESS_TEXT) : resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_ERROR_TEXT);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Stage owner = (Stage) settingsGrid.getScene().getWindow();
        alert.initOwner(owner);

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
