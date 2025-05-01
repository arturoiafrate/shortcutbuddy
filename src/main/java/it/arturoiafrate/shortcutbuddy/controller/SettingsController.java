package it.arturoiafrate.shortcutbuddy.controller;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import it.arturoiafrate.shortcutbuddy.ShortcutBuddyApp;
import it.arturoiafrate.shortcutbuddy.model.bean.Setting;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.*;
import java.util.stream.Collectors;

public class SettingsController {

    @FXML private ResourceBundle resources;
    @FXML private TabPane tabPane;
    @FXML private Button saveButton;

    private final Map<String, Control> settingFields = new HashMap<>();
    private List<Setting> currentSettings;

    @Inject
    SettingsManager settingsManager;

    @Inject
    public SettingsController() {}

    @FXML
    public void initialize() {
        this.currentSettings = settingsManager.getSettingsAll();
        Platform.runLater(() -> setSettingsData(currentSettings));
        saveButton.setGraphic(new FontIcon(Feather.SAVE));
        saveButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.SUCCESS);
    }

    private void setSettingsData(List<Setting> settingsList) {
        tabPane.getTabs().clear();
        settingFields.clear();

        Map<String, List<Setting>> grouped = settingsList.stream()
                .filter(s -> !s.isHide())
                .sorted(Comparator.comparing(Setting::getOrder))
                .collect(Collectors.groupingBy(s -> Optional.ofNullable(s.getGroupName()).orElse("general")));

        for (Map.Entry<String, List<Setting>> entry : grouped.entrySet()) {
            String groupName = entry.getKey();
            List<Setting> groupSettings = entry.getValue();

            VBox container = new VBox(10);
            container.setPadding(new Insets(10));

            TitledPane titledPane = new TitledPane(resources.getString("settings.groupName." + groupName), buildSettingsGrid(groupSettings));
            container.getChildren().add(titledPane);

            ScrollPane scrollPane = new ScrollPane(container);
            scrollPane.setFitToWidth(true);

            Tab tab = new Tab(resources.getString("settings.groupName." + groupName), scrollPane);
            tab.setClosable(false);
            tabPane.getTabs().add(tab);
        }
    }

    private GridPane buildSettingsGrid(List<Setting> settings) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        for (int i = 0; i < settings.size(); i++) {
            Setting setting = settings.get(i);
            Label label = createLabel(setting);
            Control control = createControl(setting);
            grid.add(label, 0, i);
            grid.add(control, 1, i);
            settingFields.put(setting.getKey(), control);
        }

        return grid;
    }

    private Label createLabel(Setting setting) {
        Label label = new Label(resources.getString("settings.setting." + setting.getKey()) + ":");
        label.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_BOLD);

        FontIcon infoIcon = new FontIcon(Feather.INFO);
        infoIcon.setIconSize(14);
        infoIcon.getStyleClass().add(Styles.TEXT_SUBTLE);
        label.setGraphic(infoIcon);
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.setGraphicTextGap(5);

        Tooltip tooltip = new Tooltip(resources.getString("settings.setting." + setting.getKey() + ".tooltip"));
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        Tooltip.install(infoIcon, tooltip);

        return label;
    }

    private Control createControl(Setting setting) {
        if (setting.getOptions() == null) {
            TextField textField = new TextField(setting.getValue());
            textField.setEditable(!setting.isReadonly());
            if ("NUMBER_INT".equals(setting.getValueType())) {
                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal.matches("\\d*")) {
                        textField.setText(newVal.replaceAll("[^\\d]", ""));
                    }
                });
            }
            return textField;
        } else {
            if (setting.getOptions().length == 2 && "y".equals(setting.getOptions()[0]) && "n".equals(setting.getOptions()[1])) {
                ToggleSwitch toggleSwitch = new ToggleSwitch();
                toggleSwitch.setSelected("y".equals(setting.getValue()));
                return toggleSwitch;
            } else {
                ComboBox<String> comboBox = new ComboBox<>();
                comboBox.setItems(FXCollections.observableArrayList(setting.getOptions()));
                comboBox.setValue(setting.getValue());
                comboBox.setDisable(setting.isReadonly());
                return comboBox;
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
                newValue = textField.getText();
            } else if (control instanceof ComboBox<?> comboBox) {
                newValue = (String) comboBox.getValue();
            } else if (control instanceof ToggleSwitch toggleSwitch) {
                newValue = toggleSwitch.isSelected() ? "y" : "n";
            }
            newSettings.add(new Setting(currentSetting.getKey(), newValue, currentSetting.getValueType(), currentSetting.isReadonly(), currentSetting.getOptions(), currentSetting.isHide(), currentSetting.getOrder(), currentSetting.getGroupName()));
        }
        boolean isSaved = settingsManager.save(newSettings);
        Alert alert = isSaved ? new Alert(Alert.AlertType.INFORMATION) : new Alert(Alert.AlertType.ERROR);
        String title = isSaved ? resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_SUCCESS_TITLE) : resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_ERROR_TITLE);
        String message = isSaved ? resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_SUCCESS_TEXT) : resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SETTINGS_SAVE_ERROR_TEXT);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Stage owner = (Stage) tabPane.getScene().getWindow();
        alert.initOwner(owner);

        alert.setOnShown(e -> {
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            ShortcutBuddyApp.getInstance().registerStage(alertStage);
        });

        alert.showAndWait();
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tabPane.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
