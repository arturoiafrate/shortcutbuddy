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
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
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
    @FXML private VBox shortcutsBox;

    private GridPane starredShortcutsGrid;

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
    private Stage stage;
    private boolean blockView = false;
    @Setter
    private boolean isSettingsShown = false;
    @Setter
    private boolean isUserShortcutsShown = false;
    private ResourceBundle bundle;
    private GridNavigator gridNavigator;
    private GridNavigator starredGridNavigator;
    private boolean isInStarredGrid = false;
    private List<Shortcut> currentDisplayedShortcuts;
    private AtomicBoolean ctrlPressed;
    private final Map<Long, Label> starredShortcutNumberLabels = new HashMap<>();

    @Inject
    public ShortcutController() {
    }

    @FXML
    public void initialize(){
        appNameLabel.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_BOLD, Styles.TEXT_ITALIC, Styles.TITLE_3);
        messageLabel.getStyleClass().addAll(Styles.TEXT, Styles.TEXT_ITALIC, Styles.WARNING, Styles.TEXT_CAPTION);
        messageLabel.setWrapText(true);
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        messageLabel.setAlignment(Pos.CENTER);
        searchBox.getStyleClass().addAll(Styles.LARGE, Styles.ROUNDED);
        exitSearchModeLabel.setVisible(false);
        navigationTooltipLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_ITALIC, Styles.ACCENT);
        exitSearchModeLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.DANGER);

        starredShortcutsGrid = new GridPane();
        starredShortcutsGrid.setHgap(10.0);
        starredShortcutsGrid.setVgap(5.0);
        starredShortcutsGrid.setVisible(false);

        gridNavigator = new GridNavigator(shortcutsGrid, scrollPane, InlineCSS.SHORTCUT_BORDER, InlineCSS.SELECTED_SHORTCUT_BORDER);
        starredGridNavigator = new GridNavigator(starredShortcutsGrid, scrollPane, InlineCSS.SHORTCUT_BORDER, InlineCSS.SELECTED_SHORTCUT_BORDER);

        ctrlPressed = new AtomicBoolean();

        searchBox.focusedProperty().addListener((observable, oldValue, newValue) -> updateNumberLabelsVisibility());
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
            case NativeKeyEvent.VC_1,
                 NativeKeyEvent.VC_2,
                 NativeKeyEvent.VC_3,
                 NativeKeyEvent.VC_4,
                 NativeKeyEvent.VC_5:
                if (mode == KeyOperation.KEY_PRESS) {
                    int index = keyCode -2;
                    selectStarredShortcutByIndex(index);
                }
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
            if (currentDisplayedShortcuts == null) return;

            Optional<Shortcut> selectedShortcut;
            if (isInStarredGrid && starredGridNavigator != null) {
                // Get selected shortcut from starred grid
                selectedShortcut = starredGridNavigator.getSelectedShortcut(
                    currentDisplayedShortcuts.stream()
                        .filter(Shortcut::isStarred)
                        .limit(5)
                        .toList()
                );
            } else if (gridNavigator != null) {
                selectedShortcut = gridNavigator.getSelectedShortcut(
                    currentDisplayedShortcuts.stream()
                        .filter(s -> !s.isStarred())
                        .toList()
                );
            } else {
                return;
            }

            if(selectedShortcut.isPresent() && selectedShortcut.get().getKeys() != null && !selectedShortcut.get().getKeys().isEmpty()){
                List<String> keysToPress = selectedShortcut.get().getKeys();
                Platform.runLater(() -> manageEscKey(mode));
                keyEmulator.emulateKeysAsync(keysToPress, 500);
            }
        }
    }

    private void manageNavigation(int keyCode, KeyOperation mode) {
        blockView = true;
        Platform.runLater(() -> {
            // Update number labels visibility when navigation mode changes
            updateNumberLabelsVisibility();
            exitSearchModeLabel.setText(bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.LABEL_EXIT_SEARCH));
            exitSearchModeLabel.setVisible(true);
            if(List.of(NativeKeyEvent.VC_UP, NativeKeyEvent.VC_DOWN, NativeKeyEvent.VC_LEFT, NativeKeyEvent.VC_RIGHT).contains(keyCode)
                    && mode.equals(KeyOperation.KEY_PRESS)){

                // Check if starred shortcuts grid is visible
                boolean starredGridVisible = starredShortcutsGrid.isVisible() && !starredShortcutsGrid.getChildren().isEmpty();

                // If nothing is selected and starred grid is visible, prioritize it first
                if (starredGridVisible && !isInStarredGrid && !gridNavigator.isSomethingSelected() 
                        && !starredGridNavigator.isSomethingSelected()) {
                    isInStarredGrid = true;
                    starredGridNavigator.navigate(NativeKeyEvent.VC_RIGHT);
                    return;
                }

                if (starredGridVisible) {
                    // If we're in the main grid and pressing UP, move to the starred grid
                    // Either when nothing is selected or when we're in the first row
                    if (keyCode == NativeKeyEvent.VC_UP && !isInStarredGrid && 
                            (!gridNavigator.isSomethingSelected() || gridNavigator.isInFirstRow())) {
                        // Reset selection in the main grid before switching
                        gridNavigator.resetSelection();
                        // Switch to starred grid
                        isInStarredGrid = true;
                        // Force selection of the first item in the starred grid
                        starredGridNavigator.navigate(NativeKeyEvent.VC_RIGHT);
                    }
                    // If we're in the starred grid and pressing DOWN, move to the main grid
                    else if (keyCode == NativeKeyEvent.VC_DOWN && isInStarredGrid) {
                        // Reset selection in the starred grid before switching
                        starredGridNavigator.resetSelection();
                        // Switch to main grid
                        isInStarredGrid = false;
                        gridNavigator.navigate(keyCode);
                    }
                    // Handle navigation within the current grid
                    else {
                        if (isInStarredGrid) {
                            starredGridNavigator.navigate(keyCode);

                            // If we navigated out of the starred grid, switch to the main grid
                            if (!starredGridNavigator.isSomethingSelected()) {
                                // Reset selection in the starred grid before switching
                                starredGridNavigator.resetSelection();
                                isInStarredGrid = false;
                                gridNavigator.navigate(keyCode);
                            }
                        } else {
                            gridNavigator.navigate(keyCode);

                            // If we navigated out of the main grid (upward), switch to the starred grid
                            if (keyCode == NativeKeyEvent.VC_UP && 
                                    (!gridNavigator.isSomethingSelected() || gridNavigator.isInFirstRow())) {
                                // Reset selection in the main grid before switching
                                gridNavigator.resetSelection();
                                isInStarredGrid = true;
                                // Force selection of the first item in the starred grid
                                starredGridNavigator.navigate(NativeKeyEvent.VC_RIGHT);
                            }
                        }
                    }
                } else {
                    // If no starred shortcuts, just navigate the main grid
                    gridNavigator.navigate(keyCode);
                }

                // If nothing is selected in either grid after navigation, prioritize the starred grid
                if (!gridNavigator.isSomethingSelected() && !starredGridNavigator.isSomethingSelected() && 
                    starredShortcutsGrid.isVisible() && !starredShortcutsGrid.getChildren().isEmpty()) {
                    isInStarredGrid = true;
                    // Force selection of the first item in the starred grid
                    starredGridNavigator.navigate(NativeKeyEvent.VC_RIGHT);
                }
            }
        });
    }

    private void manageCtrlKey(KeyOperation mode) {
        if(mode == KeyOperation.KEY_PRESS){
            ctrlPressed.set(true);
        } else if(mode == KeyOperation.KEY_HOLD){
            if(!stage.isShowing()){
                shortcutsGrid.getChildren().clear();
                shortcutsGrid.getColumnConstraints().clear();
                starredShortcutsGrid.getChildren().clear();
                starredShortcutsGrid.getColumnConstraints().clear();
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
                    stage.setX(appBounds.getMinX() + (appBounds.getWidth() - width) / 2);
                    stage.setY(appBounds.getMinY() + (appBounds.getHeight() - height) / 2);
                    PauseTransition focusDelay = new PauseTransition(Duration.millis(100));
                    focusDelay.setOnFinished(evt -> {
                        stage.requestFocus();
                        foregroundAppInterceptor.forceFocus();
                        updateNumberLabelsVisibility();
                    });
                    focusDelay.play();
                });
            }
        } else if(mode == KeyOperation.KEY_RELEASE){
            ctrlPressed.set(false);
            if(stage.isShowing() && !blockView){
                Platform.runLater(() -> {
                    stage.hide();
                    searchBox.clear();
                    // Reset both grid navigators
                    if (gridNavigator != null) {
                        gridNavigator.resetSelection();
                    }
                    if (starredGridNavigator != null) {
                        starredGridNavigator.resetSelection();
                    }
                    isInStarredGrid = false;
                });
            }
        }
    }
    private void manageEscKey(KeyOperation mode) {
        blockView = false;
        // Update number labels visibility when exiting navigation mode
        updateNumberLabelsVisibility();
        Platform.runLater(() -> {
            exitSearchModeLabel.setVisible(false);
            searchBox.clear();
            // Reset both grid navigators
            if (gridNavigator != null) {
                gridNavigator.resetSelection();
            }
            if (starredGridNavigator != null) {
                starredGridNavigator.resetSelection();
            }
            isInStarredGrid = false;
            stage.hide();
        });
    }
    private void manageSearch(KeyOperation mode) {
        if(mode == KeyOperation.KEY_PRESS && stage.isShowing()){
            if(!blockView){
                blockView = true;
                // Update number labels visibility when entering navigation mode
                updateNumberLabelsVisibility();
                Platform.runLater(() -> {
                    exitSearchModeLabel.setText(bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.LABEL_EXIT_SEARCH));
                    exitSearchModeLabel.setVisible(true);

                    // First clear the text box before focusing it
                    searchBox.setText(StringUtils.EMPTY);

                    // Use a small delay to ensure the key event is processed before focusing the textbox
                    // This helps prevent the search key character from being displayed
                    PauseTransition pause = new PauseTransition(Duration.millis(10));
                    pause.setOnFinished(event -> {
                        searchBox.requestFocus();
                        // Clear again after focus to ensure no character is displayed
                        searchBox.setText(StringUtils.EMPTY);
                        // Update number labels visibility when search box gains focus
                        updateNumberLabelsVisibility();
                    });
                    pause.play();
                });
            } else if((gridNavigator != null && gridNavigator.isSomethingSelected()) || 
                      (starredGridNavigator != null && starredGridNavigator.isSomethingSelected())){
                Platform.runLater(() -> {
                    // Clear the text box before focusing it
                    searchBox.setText(StringUtils.EMPTY);

                    // Reset both grid navigators
                    if (gridNavigator != null) {
                        gridNavigator.resetSelection();
                    }
                    if (starredGridNavigator != null) {
                        starredGridNavigator.resetSelection();
                    }
                    isInStarredGrid = false;

                    // Use a small delay to ensure the key event is processed before focusing the textbox
                    PauseTransition pause = new PauseTransition(Duration.millis(10));
                    pause.setOnFinished(event -> {
                        searchBox.requestFocus();
                        // Clear again after focus to ensure no character is displayed
                        searchBox.setText(StringUtils.EMPTY);
                        // Update number labels visibility when search box gains focus
                        updateNumberLabelsVisibility();
                    });
                    pause.play();
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

        // Add starredShortcutsGrid to shortcutsBox if not already added
        if (!shortcutsBox.getChildren().contains(starredShortcutsGrid)) {
            shortcutsBox.getChildren().addFirst(starredShortcutsGrid);
            // Add margin to create spacing between starred shortcuts and application grid
            VBox.setMargin(starredShortcutsGrid, new Insets(0, 0, 15, 0));
        }

        updateShortcutsGrid(shortcuts);

        searchBox.getParent().requestFocus();

        searchBox.textProperty().addListener((observable, oldValue, newValue) -> updateFilteredShortcuts(shortcuts, newValue));
    }

    private void updateShortcutsGrid(List<Shortcut> shortcuts) {
        // Clear the number labels map when the grid is updated
        starredShortcutNumberLabels.clear();

        shortcutsGrid.getChildren().clear();
        shortcutsGrid.getColumnConstraints().clear();
        starredShortcutsGrid.getChildren().clear();
        starredShortcutsGrid.getColumnConstraints().clear();

        if (shortcuts == null) {
            shortcuts = List.of();
        }

        List<Shortcut> starredShortcuts = shortcuts.stream()
                .filter(Shortcut::isStarred)
                .limit(5)
                .toList();

        List<Shortcut> nonStarredShortcuts = shortcuts.stream()
                .filter(s -> !starredShortcuts.contains(s))
                .toList();

        starredShortcutsGrid.setVisible(!starredShortcuts.isEmpty());

        if (!starredShortcuts.isEmpty()) {
            int starredColumns = Math.min(starredShortcuts.size(), 5);
            double starredPercentWidth = 100.0 / starredColumns;

            for (int i = 0; i < starredColumns; i++) {
                ColumnConstraints colConst = new ColumnConstraints();
                colConst.setPercentWidth(starredPercentWidth);
                starredShortcutsGrid.getColumnConstraints().add(colConst);
            }

            for (int i = 0; i < starredShortcuts.size(); i++) {
                Node shortcutEntryNode = createShortcutEntryNode(starredShortcuts.get(i), true, i);
                ((StackPane) shortcutEntryNode).setPrefWidth(120);
                ((StackPane) shortcutEntryNode).setPrefHeight(100);
                ((StackPane) shortcutEntryNode).setPickOnBounds(false);
                starredShortcutsGrid.add(shortcutEntryNode, i, 0);
            }
        }

        // Configure non-starred shortcuts grid
        int itemCount = nonStarredShortcuts.size();
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

        // Add non-starred shortcuts to the grid
        for (int i = 0; i < itemCount; i++) {
            Node shortcutEntryNode = createShortcutEntryNode(nonStarredShortcuts.get(i), numColumns == 3, -1);
            shortcutsGrid.add(shortcutEntryNode, i % numColumns, i / numColumns);
        }

        if(shortcutsGrid.getChildren().isEmpty()){
            shortcutsGrid.add(createEmptyShortcutEntryNode(), 0, 0);
        }

        // Update both GridNavigators with their respective grid data
        if (gridNavigator != null) {
            // Update main grid navigator
            Platform.runLater(() -> gridNavigator.setGridData(itemCount, numColumns));
        }

        if (starredGridNavigator != null && !starredShortcuts.isEmpty()) {
            // Update starred grid navigator
            int starredColumns = Math.min(starredShortcuts.size(), 5);
            Platform.runLater(() -> starredGridNavigator.setGridData(starredShortcuts.size(), starredColumns));
        }

        // Reset the active grid flag when updating the grids
        isInStarredGrid = false;

        // Store all shortcuts (starred and non-starred) for navigation
        this.currentDisplayedShortcuts = shortcuts;

        // Update number labels visibility after updating the grid
         updateNumberLabelsVisibility();
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
        finalContainer.setStyle(InlineCSS.SHORTCUT_CONTAINER_BORDER);
        finalContainer.setMaxWidth(Double.MAX_VALUE);
        return finalContainer;
    }

    private Node createShortcutEntryNode(Shortcut shortcut, boolean useSmallerText, int starredDisplayIndex) {
        Label descriptionLabel = new Label(shortcut.getDescription());
        descriptionLabel.getStyleClass().addAll(Styles.TEXT, Styles.TEXT_MUTED);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(Double.MAX_VALUE);
        descriptionLabel.setTextAlignment(TextAlignment.CENTER);
        descriptionLabel.setAlignment(Pos.CENTER);

        // Apply appropriate text size based on useSmallerText parameter
        if (useSmallerText) {
            // For starred shortcuts, use smaller text that adapts better to limited space
            descriptionLabel.getStyleClass().add(Styles.TEXT_SMALL);
        } else {
            // For regular shortcuts, use larger text
            descriptionLabel.getStyleClass().add(Styles.TITLE_4);
        }

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

        Label categoryLabel = null;
        Label numberLabel = null;
        if (!StringUtils.isEmpty(shortcut.getCategory())) {
            categoryLabel = new Label(shortcut.getCategory().toLowerCase());
            categoryLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_ITALIC);
            VBox.setMargin(categoryLabel, new Insets(0, 0, 3, 0));
        }

        if (shortcut.isStarred() && starredDisplayIndex >= 0) {
            numberLabel = new Label("[" + (starredDisplayIndex + 1) + "]");
            numberLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.ACCENT);
            numberLabel.setStyle(InlineCSS.STARRED_SHORTCUT_LABEL);
            numberLabel.setVisible(true);
            numberLabel.setMouseTransparent(true);
            starredShortcutNumberLabels.put(shortcut.getId(), numberLabel);
        }

        // Create star icon for favorites
        Label starIcon = new Label(shortcut.isStarred() ? "★" : "☆");
        starIcon.getStyleClass().addAll(Styles.TEXT_BOLD);
        starIcon.setStyle(InlineCSS.STAR_SHORTCUT_ICON);
        starIcon.setOnMouseClicked(event -> {
            boolean newStarredStatus = !shortcut.isStarred();

            if (newStarredStatus) {
                long starredCount = currentDisplayedShortcuts.stream()
                        .filter(Shortcut::isStarred)
                        .count();

                if (starredCount >= 5) {
                    return;
                }
            }

            shortcut.setStarred(newStarredStatus);
            starIcon.setText(newStarredStatus ? "★" : "☆");
            shortcutManager.updateShortcutStarred(shortcut.getId(), newStarredStatus);

            updateShortcutsGrid(currentDisplayedShortcuts);
        });

        StackPane starContainer = new StackPane(starIcon);
        starContainer.setAlignment(Pos.TOP_RIGHT);
        StackPane.setMargin(starIcon, new Insets(2, 5, 0, 0));

        starIcon.setMouseTransparent(false);
        starIcon.toFront();

        StackPane numberContainer = null;
        if (numberLabel != null) {
            numberContainer = new StackPane(numberLabel);
            numberContainer.setAlignment(Pos.TOP_LEFT);
            StackPane.setMargin(numberLabel, new Insets(2, 0, 0, 5));
            // Ensure number container doesn't block mouse events for elements below it
            numberContainer.setMouseTransparent(true);
        }

        StackPane contentWithStar = new StackPane();

        VBox finalContainer = new VBox();
        finalContainer.setSpacing(2);
        finalContainer.setAlignment(Pos.CENTER);

        if (categoryLabel != null) {
            finalContainer.getChildren().add(categoryLabel);
        }
        finalContainer.getChildren().add(contentVBox);

        // Add the content, star, and number label to the overlay container
        contentWithStar.getChildren().add(finalContainer);
        contentWithStar.getChildren().add(starContainer);
        // Ensure star container is on top and clickable
        starContainer.setMouseTransparent(false);
        starContainer.toFront();
        if (numberContainer != null) {
            contentWithStar.getChildren().add(numberContainer);
            starContainer.toFront();
        }

        contentWithStar.setStyle(InlineCSS.SHORTCUT_CONTAINER_BORDER);
        contentWithStar.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(contentWithStar, true);

        // Ensure proper event handling for the entire container
        contentWithStar.setPickOnBounds(true);

        return contentWithStar;
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
            // Hide starred shortcuts grid when no results
            starredShortcutsGrid.setVisible(false);
            shortcutsGrid.add(createEmptyShortcutEntryNode(), 0, 0);
        }

        // Reset navigation state when filtering
        isInStarredGrid = false;
        if (gridNavigator != null) {
            gridNavigator.resetSelection();
        }
        if (starredGridNavigator != null) {
            starredGridNavigator.resetSelection();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if(!isFocused && !ctrlPressed.get()){
                Platform.runLater(() -> manageEscKey(KeyOperation.KEY_PRESS));
            }
        });
    }

    /**
     * Updates the visibility of number labels for starred shortcuts.
     * Labels are only visible when in navigation mode and focus is not on search textbox.
     */
    private void updateNumberLabelsVisibility() {
        boolean shouldBeVisible = !searchBox.isFocused();
        starredShortcutNumberLabels.values().forEach(label -> label.setVisible(shouldBeVisible));
    }

    /**
     * Selects a starred shortcut by index and simulates Enter key press.
     * @param index The index of the starred shortcut (0-based)
     */
    private void selectStarredShortcutByIndex(int index) {
        if (currentDisplayedShortcuts == null || !stage.isShowing() || searchBox.isFocused()) {
            return;
        }

        List<Shortcut> starredShortcuts = currentDisplayedShortcuts.stream()
                .filter(Shortcut::isStarred)
                .limit(5)
                .toList();

        if (index >= 0 && index < starredShortcuts.size()) {
            Shortcut shortcut = starredShortcuts.get(index);

            if (shortcut.getKeys() != null && !shortcut.getKeys().isEmpty()) {
                List<String> keysToPress = shortcut.getKeys();
                Platform.runLater(() -> manageEscKey(KeyOperation.KEY_PRESS));
                keyEmulator.emulateKeysAsync(keysToPress, 500);
            }
        }
    }
}
