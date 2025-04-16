package it.arturoiafrate.shortcutbuddy.controller;

import atlantafx.base.theme.Styles;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import it.arturoiafrate.shortcutbuddy.controller.dialog.InlineCSS;
import it.arturoiafrate.shortcutbuddy.controller.util.GridNavigator;
import it.arturoiafrate.shortcutbuddy.model.bean.Shortcut;
import it.arturoiafrate.shortcutbuddy.model.constant.KeyOption;
import it.arturoiafrate.shortcutbuddy.model.interceptor.foreground.ForegroundAppInterceptor;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.IKeyObserver;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyOperation;
import it.arturoiafrate.shortcutbuddy.model.keyemulator.KeyEmulator;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import it.arturoiafrate.shortcutbuddy.model.manager.shortcut.ShortcutManager;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class ShortcutController implements IKeyObserver {

    @FXML private TextField searchBox;
    @FXML private Label messageLabel;
    @FXML private GridPane shortcutsGrid;
    @FXML private ImageView appIconImageView;
    @FXML private Label appNameLabel;
    @FXML private Label exitSearchModeLabel;
    @FXML private Label navigationTooltipLabel;
    @FXML private ScrollPane scrollPane;

    @Inject
    SettingsManager settingsManager;
    @Inject
    ShortcutManager shortcutManager;
    @Inject
    ForegroundAppInterceptor foregroundAppInterceptor;
    @Inject
    KeyEmulator keyEmulator;

    private static final int VISIBLE_ROWS = 10;
    private static final double ESTIMATED_ROW_HEIGHT = 28.0;

    @Setter
    private Stage stage;
    private boolean blockView = false;
    @Setter
    private boolean isSettingsShown = false;
    @Setter
    private boolean isUserShortcutsShown = false;
    private ResourceBundle bundle;
    private GridNavigator gridNavigator;
    private List<Shortcut> currentDisplayedShortcuts;

    @Inject
    public ShortcutController() {
    }

    @FXML
    public void initialize(){
        appNameLabel.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_BOLD, Styles.TEXT_ITALIC, Styles.TITLE_3);
        messageLabel.getStyleClass().addAll(Styles.WARNING, Styles.TEXT_BOLD);
        searchBox.getStyleClass().addAll(Styles.LARGE, Styles.ROUNDED);
        exitSearchModeLabel.setVisible(false);
        navigationTooltipLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_ITALIC, Styles.ACCENT);
        exitSearchModeLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.DANGER);
        gridNavigator = new GridNavigator(shortcutsGrid, scrollPane, InlineCSS.SHORTCUT_BORDER, InlineCSS.SELECTED_SHORTCUT_BORDER);
    }


    @Override
    public void update(int keyCode, KeyOperation mode) {
        if(isSettingsShown || isUserShortcutsShown) return;
        switch (keyCode) {
            case NativeKeyEvent.VC_CONTROL:
                manageCtrlKey(mode);
                break;
            case NativeKeyEvent.VC_PERIOD:
                if(KeyOption.DOT.equals(settingsManager.getSetting("searchKey").value())){
                    manageSearch(mode);
                }
                break;
            case NativeKeyEvent.VC_SPACE:
                if(KeyOption.SPACE.equals(settingsManager.getSetting("searchKey").value())){
                    manageSearch(mode);
                }
                break;
            case NativeKeyEvent.VC_MINUS:
                if(KeyOption.MINUS.equals(settingsManager.getSetting("searchKey").value())){
                    manageSearch(mode);
                }
                break;
            case NativeKeyEvent.VC_P:
                if(KeyOption.P.equals(settingsManager.getSetting("searchKey").value())){
                    manageSearch(mode);
                }
                break;
            case NativeKeyEvent.VC_ESCAPE:
                manageEscKey(mode);
                break;
            case NativeKeyEvent.VC_DOWN, NativeKeyEvent.VC_UP, NativeKeyEvent.VC_LEFT, NativeKeyEvent.VC_RIGHT:
                manageNavigation(keyCode, mode);
                break;
            case NativeKeyEvent.VC_ENTER:
                manageEnterKey(mode);
                break;
            default:
                break;
        }
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        String promptText = MessageFormat.format(
                bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.TEXTBOX_PROMPT),
                settingsManager.getSetting("searchKey").value()
        );
        searchBox.setPromptText(promptText);
    }

    private void manageEnterKey(KeyOperation mode) {
        if(mode == KeyOperation.KEY_PRESS && blockView && stage.isShowing()){
            if (gridNavigator == null || currentDisplayedShortcuts == null) return;
            Optional<Shortcut> selectedShortcut = gridNavigator.getSelectedShortcut(currentDisplayedShortcuts);
            if(selectedShortcut.isPresent() && selectedShortcut.get().getKeys() != null && !selectedShortcut.get().getKeys().isEmpty()){
                List<String> keysToPress = selectedShortcut.get().getKeys();
                Platform.runLater(() -> {
                    manageEscKey(mode);
                });
                keyEmulator.emulateKeysAsync(keysToPress, 500);
            }
        }
    }

    private void manageNavigation(int keyCode, KeyOperation mode) {
        blockView = true;
        Platform.runLater(() -> {
            exitSearchModeLabel.setText(bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.LABEL_EXIT_SEARCH));
            exitSearchModeLabel.setVisible(true);
            if(List.of(NativeKeyEvent.VC_UP, NativeKeyEvent.VC_DOWN, NativeKeyEvent.VC_LEFT, NativeKeyEvent.VC_RIGHT).contains(keyCode)
                    && mode.equals(KeyOperation.KEY_PRESS)){
                gridNavigator.navigate(keyCode);
            }
        });
    }

    private void manageCtrlKey(KeyOperation mode) {
        if(mode == KeyOperation.KEY_HOLD){
            if(!stage.isShowing()){
                shortcutsGrid.getChildren().clear();
                searchBox.clear();
                String appName = foregroundAppInterceptor.getForegroundAppName();
                int width = Integer.parseInt(settingsManager.getSetting("width").value());
                int height = Integer.parseInt(settingsManager.getSetting("height").value());
                Rectangle2D appBounds = foregroundAppInterceptor.getForegroundAppBounds();
                List<Shortcut> shortcutList = shortcutManager.getShortcutsForApp(appName);
                setShortcuts(shortcutList);
                String appDescription = shortcutManager.getAppDescription(appName);
                setHeader(appName, appDescription);
                Platform.runLater(() -> {
                    stage.show();
                    stage.toFront();
                    stage.requestFocus();
                    stage.setX(appBounds.getMinX() + (appBounds.getWidth() - width) / 2);
                    stage.setY(appBounds.getMinY() + (appBounds.getHeight() - height) / 2);
                    foregroundAppInterceptor.forceFocus();
                });
            }
        } else if(mode == KeyOperation.KEY_RELEASE){
            if(stage.isShowing() && !blockView){
                Platform.runLater(() -> {
                    stage.hide();
                    searchBox.clear();
                });
            }
        }
    }
    private void manageEscKey(KeyOperation mode) {
        blockView = false;
        Platform.runLater(() -> {
            exitSearchModeLabel.setVisible(false);
            searchBox.clear();
            stage.hide();
        });
    }
    private void manageSearch(KeyOperation mode) {
        if(mode == KeyOperation.KEY_PRESS && stage.isShowing()){
            if(!blockView){
                blockView = true;
                Platform.runLater(() -> {
                    exitSearchModeLabel.setText(bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.LABEL_EXIT_SEARCH));
                    exitSearchModeLabel.setVisible(true);
                    searchBox.requestFocus();
                });
            } else if(gridNavigator != null && gridNavigator.isSomethingSelected()){
                Platform.runLater(() -> {
                    searchBox.setText(StringUtils.EMPTY);
                    gridNavigator.resetSelection();
                    searchBox.requestFocus();
                });
            }
        }
    }
    private void setHeader(String appName, String appDescription) {
        boolean supportedApp = !StringUtils.isEmpty(appDescription);
        appNameLabel.setText(supportedApp ? appDescription : appName);
        String appImageName = supportedApp ? appName : "openjdk";
        String imagePath = settingsManager.getAppImagePath(appImageName);
        File imageFile = new File(imagePath);
        if(!imageFile.exists()){
            appIconImageView.setVisible(false);
            return;
        }
        Image appIcon = new Image(imageFile.toURI().toString());
        appIconImageView.setImage(appIcon);
    }
    private void setShortcuts(List<Shortcut> shortcuts) {
        messageLabel.setVisible(false);
        if (shortcuts == null || shortcuts.isEmpty()) {
            messageLabel.setText(bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.WARNING_NO_SHORTCUT));
            messageLabel.setVisible(true);
            return;
        }

        updateShortcutsGrid(shortcuts);

        searchBox.getParent().requestFocus();

        searchBox.textProperty().addListener((observable, oldValue, newValue) -> updateFilteredShortcuts(shortcuts, newValue));
    }

    private void updateShortcutsGrid(List<Shortcut> shortcuts) {
        shortcutsGrid.getChildren().clear();
        shortcutsGrid.getColumnConstraints().clear();
        if (shortcuts == null) {
            shortcuts = List.of();
        }
        int itemCount = shortcuts.size();
        int numColumns = (itemCount <= 20) ? 2 : 3;
        if (itemCount > 0) {
            double percentWidth = 100.0 / numColumns;
            for (int i = 0; i < numColumns; i++) {
                ColumnConstraints colConst = new ColumnConstraints();
                colConst.setPercentWidth(percentWidth);
                shortcutsGrid.getColumnConstraints().add(colConst);
            }
        } else {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPercentWidth(100.0);
            shortcutsGrid.getColumnConstraints().add(colConst);
        }
        if (scrollPane != null) {
            double gridTopPadding = shortcutsGrid.getPadding().getTop();
            double gridBottomPadding = shortcutsGrid.getPadding().getBottom();
            double totalVGap = (VISIBLE_ROWS > 1) ? (VISIBLE_ROWS - 1) * shortcutsGrid.getVgap() : 0;
            double calculatedPrefHeight = (VISIBLE_ROWS * ESTIMATED_ROW_HEIGHT) + totalVGap + gridTopPadding + gridBottomPadding;

            scrollPane.setPrefViewportHeight(calculatedPrefHeight);
            scrollPane.setFitToWidth(true);
        }
        for (int i = 0; i < itemCount; i++) {
            Node shortcutEntryNode = createShortcutEntryNode(shortcuts.get(i), numColumns == 3);
            shortcutsGrid.add(shortcutEntryNode, i % numColumns, i / numColumns);
        }
        if (gridNavigator != null) {
            Platform.runLater(() -> gridNavigator.setGridData(itemCount, numColumns));
        }
        this.currentDisplayedShortcuts = shortcuts;
    }

    private String formatKeyName(String rawKey) {
        if (StringUtils.isEmpty(rawKey)) return "";
        return StringUtils.capitalize(rawKey.toLowerCase());
    }

    private Node createEmptyShortcutEntryNode() {
        Label emptyLabel = new Label(bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.WARNING_NO_RESULTS));
        emptyLabel.getStyleClass().addAll(Styles.TEXT, Styles.TEXT_ITALIC, Styles.WARNING, Styles.TEXT_CAPTION);
        emptyLabel.setWrapText(true);
        emptyLabel.setTextAlignment(TextAlignment.CENTER);
        emptyLabel.setAlignment(Pos.CENTER);
        VBox finalContainer = new VBox(emptyLabel);
        finalContainer.setSpacing(2);
        finalContainer.setPadding(new Insets(10));
        finalContainer.setMinHeight(50);
        finalContainer.setAlignment(Pos.CENTER);
        finalContainer.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-padding: 4px;");
        finalContainer.setMaxWidth(Double.MAX_VALUE);
        return finalContainer;
    }

    private Node createShortcutEntryNode(Shortcut shortcut, boolean useSmallerText) {
        Label descriptionLabel = new Label(shortcut.getDescription());
        descriptionLabel.getStyleClass().addAll(Styles.TEXT, Styles.TEXT_MUTED);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(Double.MAX_VALUE);
        descriptionLabel.setTextAlignment(TextAlignment.CENTER);
        descriptionLabel.setAlignment(Pos.CENTER);

        Node shortcutRepresentationNode;
        HBox keysContainer = new HBox();
        keysContainer.setSpacing(4);
        keysContainer.setAlignment(Pos.CENTER);

        for (String key : shortcut.getKeys()) {
            Label keyLabel = new Label(formatKeyName(key));
            keyLabel.setStyle(InlineCSS.SHORTCUT_BORDER);
            keyLabel.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.ACCENT);
            keysContainer.getChildren().add(keyLabel);
        }
        shortcutRepresentationNode = keysContainer;

        VBox contentVBox = new VBox(shortcutRepresentationNode, descriptionLabel);
        contentVBox.setAlignment(Pos.CENTER);

        if (!useSmallerText) {
            descriptionLabel.getStyleClass().add(Styles.TITLE_4);
        }

        Label categoryLabel = null;
        if (!StringUtils.isEmpty(shortcut.getCategory())) {
            categoryLabel = new Label(shortcut.getCategory().toLowerCase());
            categoryLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_ITALIC);
            VBox.setMargin(categoryLabel, new Insets(0, 0, 3, 0));
        }

        VBox finalContainer = new VBox();
        finalContainer.setSpacing(2);
        finalContainer.setAlignment(Pos.CENTER);

        if (categoryLabel != null) {
            finalContainer.getChildren().add(categoryLabel);
        }
        finalContainer.getChildren().add(contentVBox);

        finalContainer.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-padding: 4px;");
        finalContainer.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(finalContainer, true);

        return finalContainer;
    }

    private void updateFilteredShortcuts(List<Shortcut> shortcuts, String filter) {
        List<Shortcut> filteredShortcuts = shortcuts.stream()
                .filter(shortcut -> shortcut.getDescription().toLowerCase().contains(filter.toLowerCase()))
                .collect(Collectors.toList());
        filteredShortcuts.addAll(shortcuts.stream()
                .filter(shortcut -> !StringUtils.isEmpty(shortcut.getCategory()) && shortcut.getCategory().toLowerCase().contains(filter.toLowerCase()) && !filteredShortcuts.contains(shortcut))
                .toList()
        );
        updateShortcutsGrid(filteredShortcuts);

        if(filteredShortcuts.isEmpty()){
            shortcutsGrid.add(createEmptyShortcutEntryNode(), 0, 0);
        }
    }
}