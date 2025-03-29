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
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
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

    private Node createShortcutEntryNode(Shortcut shortcut, boolean useSmallerText) {
        Label shortcutLabel = new Label(shortcut.shortcut());
        shortcutLabel.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.ACCENT);

        Label descriptionLabel = new Label(shortcut.description());
        descriptionLabel.getStyleClass().addAll(Styles.TEXT, Styles.TEXT_ITALIC, Styles.SUCCESS);

        if (!useSmallerText) {
            shortcutLabel.getStyleClass().add(Styles.TITLE_4);
            descriptionLabel.getStyleClass().add(Styles.TITLE_4);
        }

        shortcutLabel.setWrapText(true);
        descriptionLabel.setWrapText(true);

        Pane container;
        if(shortcutLabel.getText().length() > 30 || descriptionLabel.getText().length() > 30){
            container = new VBox(shortcutLabel, descriptionLabel);
            ((VBox)container).setAlignment(Pos.BASELINE_LEFT);
        } else {
            container = new HBox(shortcutLabel, descriptionLabel);
            ((HBox)container).setSpacing(2);
            ((HBox)container).setAlignment(Pos.BASELINE_LEFT);
        }
        container.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-padding: 4px;");

        container.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(container, true);

        return container;
    }

    private void updateFilteredShortcuts(List<Shortcut> shortcuts, String filter) {
        List<Shortcut> filteredShortcuts = shortcuts.stream()
                .filter(shortcut -> shortcut.description().toLowerCase().contains(filter.toLowerCase()))
                .collect(Collectors.toList());

        updateShortcutsGrid(filteredShortcuts);

        if(filteredShortcuts.isEmpty()){
            Label emptyLabel = new Label (bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.WARNING_NO_RESULTS));
            emptyLabel.getStyleClass().addAll(Styles.TEXT, Styles.TEXT_ITALIC, Styles.WARNING, Styles.TEXT_CAPTION);
            shortcutsGrid.add(emptyLabel, 0 ,0 );
        }
    }
}