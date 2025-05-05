package it.arturoiafrate.shortcutbuddy.controller;

import atlantafx.base.theme.Styles;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import it.arturoiafrate.shortcutbuddy.ShortcutBuddyApp;
import it.arturoiafrate.shortcutbuddy.controller.dialog.InlineCSS;
import it.arturoiafrate.shortcutbuddy.model.bean.ClipboardEntry;
import it.arturoiafrate.shortcutbuddy.model.enumerator.ClipboardContentType;
import it.arturoiafrate.shortcutbuddy.model.interceptor.foreground.ForegroundAppInterceptor;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.IKeyObserver;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyOperation;
import it.arturoiafrate.shortcutbuddy.model.keyemulator.KeyEmulator;
import it.arturoiafrate.shortcutbuddy.model.manager.clipboard.ClipboardHistoryManager;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.MouseInfo;
import java.awt.Point;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class ClipboardSnippetController implements IKeyObserver {

    @FXML
    private TextField searchBox;

    @FXML
    private Label exitSearchModeLabel;


    @FXML
    private GridPane snippetsGrid;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox snippetsBox;

    @FXML
    private HBox snippetsLabelBox;

    private final ClipboardHistoryManager clipboardHistoryManager;
    private final KeyEmulator keyEmulator;
    private final ForegroundAppInterceptor foregroundAppInterceptor;

    private Stage stage;

    private ResourceBundle bundle;
    private List<ClipboardEntry> clipboardEntries;
    private List<ClipboardEntry> filteredEntries;
    private final AtomicBoolean isSearchMode = new AtomicBoolean(false);
    private int selectedIndex = -1;
    private boolean isSnippetsLabelSelected = false;

    @Inject
    public ClipboardSnippetController(ClipboardHistoryManager clipboardHistoryManager, 
                                     KeyEmulator keyEmulator,
                                     ForegroundAppInterceptor foregroundAppInterceptor) {
        this.clipboardHistoryManager = clipboardHistoryManager;
        this.keyEmulator = keyEmulator;
        this.foregroundAppInterceptor = foregroundAppInterceptor;
    }

    @FXML
    public void initialize() {
        exitSearchModeLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.ACCENT);
        searchBox.clear();
        searchBox.requestFocus();
        searchBox.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilteredEntries(clipboardEntries, newValue);
        });
    }

    public void setStage(Stage stage){
        this.stage = stage;
        loadAppIcon();
        this.stage.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if(!isFocused){
                Platform.runLater(() -> manageEscKey(KeyOperation.KEY_PRESS));
            }
        });
    }

    private void loadAppIcon(){
        log.debug("Loading application icon");
        try{
            Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png")));
            this.stage.getIcons().add(appIcon);
            log.debug("Application icon loaded successfully");
        } catch (Exception e) {
            log.error("Error loading application icon", e);
        }
    }

    @Override
    public void update(int keyCode, KeyOperation mode, NativeKeyEvent nativeKeyEvent) {
        if (stage == null || !stage.isShowing()) {
            return;
        }

        if (keyCode == NativeKeyEvent.VC_ESCAPE && mode == KeyOperation.KEY_PRESS) {
            manageEscKey(mode);
        } else if (keyCode == NativeKeyEvent.VC_ENTER && mode == KeyOperation.KEY_PRESS) {
            manageEnterKey(mode);
        } else if ((keyCode == NativeKeyEvent.VC_UP || keyCode == NativeKeyEvent.VC_DOWN) && mode == KeyOperation.KEY_PRESS) {
            manageNavigation(keyCode, mode);
        }
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        if (exitSearchModeLabel != null) {
            exitSearchModeLabel.setText(this.bundle.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.APP_CLIPBOARD_TOOLTIP));
        }
    }

    private void manageEnterKey(KeyOperation mode) {
        if (mode != KeyOperation.KEY_PRESS) {
            return;
        }

        // Only process Enter key when a grid item is selected (not when snippetsLabelBox is selected)
        if (!isSnippetsLabelSelected && selectedIndex >= 0 && selectedIndex < filteredEntries.size()) {
            ClipboardEntry selectedEntry = filteredEntries.get(selectedIndex);
            copyToClipboard(selectedEntry.getContent());

            // Simulate paste action (Ctrl+V) after copying to clipboard
            List<String> pasteKeys = List.of("CTRL", "V");
            keyEmulator.emulateKeysAsync(pasteKeys, 100); // Small delay to ensure clipboard content is ready

            hideStage();
        }
    }

    private void copyToClipboard(String content) {
        try {
            StringSelection selection = new StringSelection(content);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            log.debug("Content copied to clipboard: {}", content);
        } catch (Exception e) {
            log.error("Error copying to clipboard", e);
        }
    }

    private void manageNavigation(int keyCode, KeyOperation mode) {
        if (mode != KeyOperation.KEY_PRESS) {
            return;
        }

        if (filteredEntries == null || filteredEntries.isEmpty()) {
            return;
        }

        if (keyCode == NativeKeyEvent.VC_DOWN) {
            if (isSnippetsLabelSelected) {
                // If snippetsLabel is selected and DOWN is pressed, move to the first item in the grid
                isSnippetsLabelSelected = false;
                selectedIndex = 0;
            } else if (selectedIndex == filteredEntries.size() - 1) {
                // If at the last item in the grid and DOWN is pressed, move to snippetsLabel
                isSnippetsLabelSelected = true;
                selectedIndex = -1;
            } else {
                // Normal navigation within the grid
                selectedIndex = Math.min(selectedIndex + 1, filteredEntries.size() - 1);
            }
        } else if (keyCode == NativeKeyEvent.VC_UP) {
            if (isSnippetsLabelSelected) {
                // If snippetsLabel is selected and UP is pressed, move to the last item in the grid
                isSnippetsLabelSelected = false;
                selectedIndex = filteredEntries.size() - 1;
            } else if (selectedIndex == 0) {
                // If at the first item in the grid and UP is pressed, move to snippetsLabel
                isSnippetsLabelSelected = true;
                selectedIndex = -1;
            } else {
                // Normal navigation within the grid
                selectedIndex = Math.max(selectedIndex - 1, 0);
            }
        }

        updateSelection();
    }

    private void updateSelection() {
        // Update snippetsLabelBox selection
        if (isSnippetsLabelSelected) {
            snippetsLabelBox.setStyle(InlineCSS.SELECTED_SHORTCUT_BORDER);
        } else {
            snippetsLabelBox.setStyle(InlineCSS.SHORTCUT_CONTAINER_BORDER);
        }

        // Update snippetsGrid selection
        for (int i = 0; i < snippetsGrid.getChildren().size(); i++) {
            Node node = snippetsGrid.getChildren().get(i);
            if (!isSnippetsLabelSelected && i == selectedIndex) {
                // Apply the selected border style to highlight the selected item
                node.setStyle(InlineCSS.SELECTED_SHORTCUT_BORDER);
            } else {
                // Apply the default container border style to non-selected items
                node.setStyle(InlineCSS.SHORTCUT_CONTAINER_BORDER);
            }
        }

        // Ensure the selected item is visible (only if a grid item is selected)
        if (!isSnippetsLabelSelected && selectedIndex >= 0 && selectedIndex < snippetsGrid.getChildren().size()) {
            Node selectedNode = snippetsGrid.getChildren().get(selectedIndex);

            // Get the bounds of the selected node relative to the scroll pane's content
            double nodeMinY = selectedNode.getBoundsInParent().getMinY();
            double nodeMaxY = selectedNode.getBoundsInParent().getMaxY();

            // Get the current viewport bounds
            double viewportMinY = scrollPane.getVvalue() * snippetsGrid.getHeight();
            double viewportMaxY = viewportMinY + scrollPane.getViewportBounds().getHeight();

            // Check if the node is fully visible in the viewport
            if (nodeMinY < viewportMinY) {
                double additionalOffset = selectedNode.getBoundsInParent().getHeight() * 0.5; // 50% of row height as additional scroll
                double newVvalue = (nodeMinY - additionalOffset) / snippetsGrid.getHeight();
                scrollPane.setVvalue(Math.max(0, Math.min(1, newVvalue)));
            } else if (nodeMaxY > viewportMaxY) {
                double additionalOffset = selectedNode.getBoundsInParent().getHeight() * 0.5; // 50% of row height as additional scroll
                double newVvalue = (nodeMaxY + additionalOffset - scrollPane.getViewportBounds().getHeight()) / snippetsGrid.getHeight();
                scrollPane.setVvalue(Math.max(0, Math.min(1, newVvalue)));
            }
            // If the node is already visible, no scrolling needed
        }

        // If snippetsLabelBox is selected, ensure it's visible by scrolling to the bottom
        if (isSnippetsLabelSelected) {
            scrollPane.setVvalue(1.0); // Scroll to the bottom
        }
    }

    private void manageEscKey(KeyOperation mode) {
        if (mode != KeyOperation.KEY_PRESS) {
            return;
        }
        hideStage();
        searchBox.clear();
        updateFilteredEntries(clipboardEntries, "");
    }

    public void setClipboardEntries(List<ClipboardEntry> entries) {
        this.clipboardEntries = entries;
        updateFilteredEntries(entries, "");
    }

    private void updateFilteredEntries(List<ClipboardEntry> entries, String filter) {
        if (entries == null) {
            return;
        }

        if (StringUtils.isBlank(filter)) {
            filteredEntries = entries;
        } else {
            filteredEntries = entries.stream()
                    .filter(entry -> entry.getContent().toLowerCase().contains(filter.toLowerCase()))
                    .collect(Collectors.toList());
        }

        updateSnippetsGrid(filteredEntries);
    }

    private void updateSnippetsGrid(List<ClipboardEntry> entries) {
        Platform.runLater(() -> {
            snippetsGrid.getChildren().clear();
            snippetsGrid.getRowConstraints().clear();

            if (entries.isEmpty()) {
                return;
            }

            // Reset selection to the first item in the grid
            selectedIndex = 0;
            isSnippetsLabelSelected = false;

            int row = 0;
            for (ClipboardEntry entry : entries) {
                Node entryNode = createSnippetEntryNode(entry);
                snippetsGrid.add(entryNode, 0, row);
                row++;
            }

            updateSelection();
        });
    }

    private Node createSnippetEntryNode(ClipboardEntry entry) {
        VBox entryBox = new VBox();
        entryBox.setAlignment(Pos.CENTER_LEFT);
        entryBox.setPadding(new Insets(5, 10, 5, 10));
        entryBox.setSpacing(5);

        // Create an HBox to hold the icon and content
        HBox contentBox = new HBox();
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setSpacing(5);

        // Add icon for TEXT type entries
        if (entry.getContentType() == ClipboardContentType.TEXT) {
            Image iconImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/edit_note.png")));
            ImageView iconView = new ImageView(iconImage);
            iconView.setFitHeight(16);
            iconView.setFitWidth(16);
            contentBox.getChildren().add(iconView);
        }

        Label contentLabel = new Label(entry.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(Double.MAX_VALUE);

        // Truncate long content
        String content = entry.getContent();
        if (content.length() > 30) {
            content = content.substring(0, 29) + "...";
        }
        contentLabel.setText(content);

        contentBox.getChildren().add(contentLabel);
        entryBox.getChildren().add(contentBox);
        return entryBox;
    }

    private void hideStage() {
        if (stage != null) {
            Platform.runLater(() -> {
                stage.hide();
                isSearchMode.set(false);
                searchBox.clear();
            });
        }
    }

    public void showStage() {
        if (stage != null && !stage.isShowing() && Window.getWindows().isEmpty()) {
            Platform.runLater(() -> {
                // Load the latest clipboard entries
                setClipboardEntries(clipboardHistoryManager.getHistory());

                // Try to get the caret position from the foreground application
                Point2D caretPosition = foregroundAppInterceptor.getCaretPosition();

                // Get position coordinates (either from caret or mouse)
                double posX, posY;

                if (caretPosition != null) {
                    // Use caret position if available
                    log.debug("Using caret position: {}", caretPosition);
                    posX = caretPosition.getX();
                    posY = caretPosition.getY();
                } else {
                    // Fall back to mouse position if caret position is not available
                    log.debug("Falling back to mouse position");
                    Point mousePosition = MouseInfo.getPointerInfo().getLocation();
                    posX = mousePosition.getX();
                    posY = mousePosition.getY();
                }

                // Adjust position to ensure the stage is fully visible on screen
                // Get screen dimensions
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                double screenWidth = screenSize.getWidth();
                double screenHeight = screenSize.getHeight();

                // Get stage dimensions
                double stageWidth = stage.getWidth();
                double stageHeight = stage.getHeight();

                // Calculate position to ensure stage is fully visible
                double xPos = Math.min(posX, screenWidth - stageWidth);
                double yPos = Math.min(posY, screenHeight - stageHeight);

                // Set stage position
                stage.setX(xPos);
                stage.setY(yPos);

                stage.show();
                stage.toFront();
                stage.requestFocus();
                foregroundAppInterceptor.forceFocus();
                ShortcutBuddyApp.getInstance().registerStage(stage);
            });
        }
    }
}
