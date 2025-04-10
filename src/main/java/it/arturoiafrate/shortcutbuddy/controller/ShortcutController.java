package it.arturoiafrate.shortcutbuddy.controller;

import atlantafx.base.theme.Styles;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import it.arturoiafrate.shortcutbuddy.model.bean.Shortcut;
import it.arturoiafrate.shortcutbuddy.model.interceptor.foreground.ForegroundAppInterceptor;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.IKeyObserver;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyOperation;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import it.arturoiafrate.shortcutbuddy.model.manager.shortcut.ShortcutManager;
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
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ShortcutController implements IKeyObserver {

    @FXML
    private TextField searchBox;

    @FXML
    private Label messageLabel;

    @FXML
    private GridPane shortcutsGrid;

    @FXML
    private ImageView appIconImageView;

    @FXML
    private Label appNameLabel;

    @FXML
    private ScrollPane scrollPane;

    private static final int VISIBLE_ROWS = 10;
    private static final double ESTIMATED_ROW_HEIGHT = 28.0;

    private ForegroundAppInterceptor foregroundAppInterceptor;
    private Stage stage;
    private boolean blockView = false;
    private boolean isSettingsShown = false;
    private ResourceBundle bundle;


    @FXML
    public void initialize(){
        appNameLabel.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_BOLD, Styles.TEXT_ITALIC, Styles.TITLE_3);
        messageLabel.getStyleClass().addAll(Styles.WARNING, Styles.TEXT_BOLD);
        searchBox.getStyleClass().addAll(Styles.LARGE, Styles.ROUNDED);
    }


    @Override
    public void update(int keyCode, KeyOperation mode) {
        if(isSettingsShown) return;
        switch (keyCode) {
            case NativeKeyEvent.VC_CONTROL:
                manageCtrlKey(mode);
                break;
            case NativeKeyEvent.VC_PERIOD:
                managePeriodKey(mode);
                break;
            case NativeKeyEvent.VC_ESCAPE:
                manageEscKey(mode);
                break;
            default:
                break;
        }
    }

    public void setSettingsShown(boolean settingsShown) {
        isSettingsShown = settingsShown;
    }

    public void setForegroundAppInterceptor(ForegroundAppInterceptor foregroundAppInterceptor) {
        this.foregroundAppInterceptor = foregroundAppInterceptor;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void manageCtrlKey(KeyOperation mode) {
        if(mode == KeyOperation.KEY_HOLD){
            if(!stage.isShowing()){
                shortcutsGrid.getChildren().clear();
                searchBox.clear();
                String appName = foregroundAppInterceptor.getForegroundAppName();
                int width = Integer.parseInt(SettingsManager.getInstance().getSetting("width").value());
                int height = Integer.parseInt(SettingsManager.getInstance().getSetting("height").value());
                Rectangle2D appBounds = foregroundAppInterceptor.getForegroundAppBounds();
                List<Shortcut> shortcutList = ShortcutManager.getInstance().getShortcutsForApp(appName);
                setShortcuts(shortcutList);
                String appDescription = ShortcutManager.getInstance().getAppDescription(appName);
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
            searchBox.clear();
            stage.hide();
        });
    }
    private void managePeriodKey(KeyOperation mode) {
        if(mode == KeyOperation.KEY_PRESS && !blockView && stage.isShowing()){
            blockView = true;
            Platform.runLater(() -> searchBox.requestFocus());
        }
    }
    private void setHeader(String appName, String appDescription) {
        boolean supportedApp = !StringUtils.isEmpty(appDescription);
        appNameLabel.setText(supportedApp ? appDescription : appName);
        String appImageName = supportedApp ? appName : "openjdk";
        String imagePath = SettingsManager.getInstance().getAppImagePath(appImageName);
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
        Label descriptionLabel = new Label(shortcut.description());
        descriptionLabel.getStyleClass().addAll(Styles.TEXT, Styles.TEXT_MUTED);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(Double.MAX_VALUE);
        descriptionLabel.setTextAlignment(TextAlignment.CENTER);
        descriptionLabel.setAlignment(Pos.CENTER);

        Node shortcutRepresentationNode;
        if (shortcut.keys() != null && !shortcut.keys().isEmpty()) {
            HBox keysContainer = new HBox();
            keysContainer.setSpacing(4);
            keysContainer.setAlignment(Pos.CENTER);

            for (String key : shortcut.keys()) {
                Label keyLabel = new Label(formatKeyName(key));
                keyLabel.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-padding: 2px 6px; -fx-border-style: solid;");
                keyLabel.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.ACCENT);
                keysContainer.getChildren().add(keyLabel);
            }
            shortcutRepresentationNode = keysContainer;
        } else {

            Label shortcutLabel = new Label(shortcut.shortcut());
            shortcutLabel.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.ACCENT);
            shortcutLabel.setWrapText(true);
            if (!useSmallerText) {
                shortcutLabel.getStyleClass().add(Styles.TITLE_4);
            }
            shortcutRepresentationNode = shortcutLabel;
        }

        VBox contentVBox = new VBox(shortcutRepresentationNode, descriptionLabel);
        contentVBox.setAlignment(Pos.CENTER);

        if (!useSmallerText) {
            descriptionLabel.getStyleClass().add(Styles.TITLE_4);
        }

        Label categoryLabel = null;
        if (!StringUtils.isEmpty(shortcut.category())) {
            categoryLabel = new Label(shortcut.category().toLowerCase());
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
                .filter(shortcut -> shortcut.description().toLowerCase().contains(filter.toLowerCase()))
                .collect(Collectors.toList());
        filteredShortcuts.addAll(shortcuts.stream()
                .filter(shortcut -> !StringUtils.isEmpty(shortcut.category()) && shortcut.category().toLowerCase().contains(filter.toLowerCase()) && !filteredShortcuts.contains(shortcut))
                .toList()
        );
        updateShortcutsGrid(filteredShortcuts);

        if(filteredShortcuts.isEmpty()){
            shortcutsGrid.add(createEmptyShortcutEntryNode(), 0, 0);
        }
    }
}