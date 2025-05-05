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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

import java.util.*;
import java.util.stream.Collectors;

public class SettingsController {

    @FXML private ResourceBundle resources;
    @FXML private TabPane tabPane;
    @FXML private Button saveButton;

    private final Map<String, Control> settingFields = new HashMap<>();
    private final Map<String, Node> buttonBoxes = new HashMap<>();
    private final Map<String, List<String>> dependentSettings = new HashMap<>();
    private List<Setting> currentSettings;

    @Inject
    SettingsManager settingsManager;

    @Inject
    public SettingsController() {}

    @FXML
    public void initialize() {
        this.currentSettings = settingsManager.getSettingsAll();
        buildDependencyMap(currentSettings);
        Platform.runLater(() -> setSettingsData(currentSettings));
        saveButton.setGraphic(new FontIcon(Feather.SAVE));
        saveButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.SUCCESS);
    }

    private void buildDependencyMap(List<Setting> settings) {
        dependentSettings.clear();
        for (Setting setting : settings) {
            if (!StringUtils.isEmpty(setting.getConditionalEnabling())) {
                String conditionalKey = setting.getConditionalEnabling();
                dependentSettings.computeIfAbsent(conditionalKey, k -> new ArrayList<>()).add(setting.getKey());
            }
        }
    }

    private boolean shouldBeEnabled(Setting setting) {
        if (StringUtils.isEmpty(setting.getConditionalEnabling())) {
            return true;
        }
        return settingsManager.isEnabled(setting.getConditionalEnabling());
    }

    private void updateDependentControls(String conditionalKey, boolean enabled) {
        List<String> dependents = dependentSettings.get(conditionalKey);
        if (dependents != null) {
            for (String dependentKey : dependents) {
                Control control = settingFields.get(dependentKey);
                if (control != null) {
                    control.setDisable(!enabled);
                }

                // Also disable/enable the button box if it exists
                Node buttonBox = buttonBoxes.get(dependentKey);
                if (buttonBox != null) {
                    buttonBox.setDisable(!enabled);
                }
            }
        }
    }

    private void setSettingsData(List<Setting> settingsList) {
        tabPane.getTabs().clear();
        settingFields.clear();
        buttonBoxes.clear();

        Map<String, List<Setting>> grouped = settingsList.stream()
                .filter(s -> !s.isHide())
                .sorted(Comparator.comparing(Setting::getOrder))
                .collect(Collectors.groupingBy(s -> Optional.ofNullable(s.getGroupName()).orElse("general")));

        for (Map.Entry<String, List<Setting>> entry : grouped.entrySet()) {
            String groupName = entry.getKey();
            List<Setting> groupSettings = entry.getValue();
            boolean areAllDev = groupSettings.stream().allMatch(Setting::isDev);
            if(!settingsManager.isDevMode() && areAllDev) {
                continue;
            }
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
            if(!setting.isDev() || settingsManager.isDevMode()){
                Label label = createLabel(setting);
                Control control = createControl(setting);
                grid.add(label, 0, i);
                grid.add(control, 1, i);
                settingFields.put(setting.getKey(), control);

                // Add additional controls for PICKER type
                if ("PICKER".equals(setting.getValueType()) && control instanceof TextField textField) {
                    // Create a button to open directory chooser
                    Button browseButton = new Button();
                    FontIcon folderIcon = new FontIcon(Feather.FOLDER);
                    browseButton.setGraphic(folderIcon);
                    browseButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);

                    // Create a button to clear the text field
                    Button clearButton = new Button();
                    FontIcon clearIcon = new FontIcon(Feather.X);
                    clearButton.setGraphic(clearIcon);
                    clearButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);

                    // Set up the directory chooser action
                    browseButton.setOnAction(event -> {
                        DirectoryChooser directoryChooser = new DirectoryChooser();
                        directoryChooser.setTitle(resources.getString("settings.setting." + setting.getKey()));

                        // Set initial directory if there's a value
                        if (!textField.getText().isEmpty()) {
                            File initialDir = new File(textField.getText());
                            if (initialDir.exists() && initialDir.isDirectory()) {
                                directoryChooser.setInitialDirectory(initialDir);
                            }
                        }

                        // Show directory chooser dialog
                        File selectedDirectory = directoryChooser.showDialog(browseButton.getScene().getWindow());
                        if (selectedDirectory != null) {
                            textField.setText(selectedDirectory.getAbsolutePath());
                        }
                    });

                    // Set up the clear button action
                    clearButton.setOnAction(event -> textField.setText(""));

                    // Create an HBox to hold the buttons
                    HBox buttonBox = new HBox(5, browseButton, clearButton);
                    grid.add(buttonBox, 2, i);
                    buttonBoxes.put(setting.getKey(), buttonBox);

                    // Ensure the button box is disabled if the control is disabled
                    if (control.isDisabled()) {
                        buttonBox.setDisable(true);
                    }
                }
            }
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
        Control control;

        if (setting.getOptions() == null) {
            TextField textField = new TextField(setting.getValue());
            textField.setEditable(!setting.isReadonly());
            if ("NUMBER_INT".equals(setting.getValueType())) {
                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal.matches("\\d*")) {
                        textField.setText(newVal.replaceAll("[^\\d]", ""));
                    }
                });
            } else if ("PICKER".equals(setting.getValueType())){
                // Make the text field non-editable for PICKER type
                textField.setEditable(false);
            }
            control = textField;
        } else {
            if (setting.getOptions().length == 2 && "y".equals(setting.getOptions()[0]) && "n".equals(setting.getOptions()[1])) {
                ToggleSwitch toggleSwitch = new ToggleSwitch();
                toggleSwitch.setSelected("y".equals(setting.getValue()));

                // Add listener for conditional enabling
                if (dependentSettings.containsKey(setting.getKey())) {
                    toggleSwitch.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        updateDependentControls(setting.getKey(), newVal);
                    });
                }

                control = toggleSwitch;
            } else {
                ComboBox<String> comboBox = new ComboBox<>();
                comboBox.setItems(FXCollections.observableArrayList(setting.getOptions()));
                comboBox.setValue(setting.getValue());
                comboBox.setDisable(setting.isReadonly());

                // Add listener for conditional enabling if this is a y/n choice
                if (dependentSettings.containsKey(setting.getKey())) {
                    comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                        updateDependentControls(setting.getKey(), "y".equals(newVal));
                    });
                }

                control = comboBox;
            }
        }

        // Apply conditional enabling
        if (!StringUtils.isEmpty(setting.getConditionalEnabling())) {
            boolean shouldEnable = shouldBeEnabled(setting);
            control.setDisable(!shouldEnable);
        }

        return control;
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
            newSettings.add(new Setting(currentSetting.getKey(), newValue, currentSetting.getValueType(), currentSetting.isReadonly(), currentSetting.getOptions(), currentSetting.isHide(), currentSetting.getOrder(), currentSetting.getGroupName(), currentSetting.isDev(), currentSetting.getConditionalEnabling()));
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
