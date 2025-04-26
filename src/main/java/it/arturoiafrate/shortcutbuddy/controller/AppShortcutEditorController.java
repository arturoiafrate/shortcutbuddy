package it.arturoiafrate.shortcutbuddy.controller;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import it.arturoiafrate.shortcutbuddy.ShortcutBuddyApp;
import it.arturoiafrate.shortcutbuddy.config.ApplicationComponent;
import it.arturoiafrate.shortcutbuddy.controller.dialog.DialogUtils;
import it.arturoiafrate.shortcutbuddy.controller.dialog.InlineCSS;
import it.arturoiafrate.shortcutbuddy.model.bean.AppShortcuts;
import it.arturoiafrate.shortcutbuddy.model.bean.Shortcut;
import it.arturoiafrate.shortcutbuddy.model.bean.ShortcutEditLists;
import it.arturoiafrate.shortcutbuddy.model.manager.shortcut.ShortcutManager;
import it.arturoiafrate.shortcutbuddy.utility.AppInfo;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controller for the application shortcut editor view.
 * This controller manages the UI for viewing, adding, editing, and deleting shortcuts for a specific application.
 */
@Slf4j
public class AppShortcutEditorController {

    @FXML private ResourceBundle resources;
    @FXML private Label appTitleLabel;
    @FXML private ImageView headerAppIconView;
    @FXML private Button addShortcutButton;
    @FXML private TableView<Shortcut> shortcutsTable;
    @FXML private TableColumn<Shortcut, List<String>> keysColumn;
    @FXML private TableColumn<Shortcut, String> descriptionColumn;
    @FXML private TableColumn<Shortcut, String> categoryColumn;
    @FXML private TableColumn<Shortcut, Void> actionsColumn;
    @FXML private Button saveButton;

    @Inject ShortcutManager shortcutManager;
    @Inject ApplicationComponent appComponent;

    private AppShortcuts currentApp;
    private ObservableList<Shortcut> shortcutList;
    private final BooleanProperty hasUnsavedChanges = new SimpleBooleanProperty(false);
    private ShortcutEditLists shortcutEditLists;

    /**
     * Constructor for dependency injection.
     */
    @Inject
    public AppShortcutEditorController() {}

    /**
     * Initializes the controller after FXML fields are injected.
     * Sets up UI components, styles, and bindings.
     */
    @FXML
    private void initialize() {
        log.debug("Initializing AppShortcutEditorController");
        shortcutEditLists = new ShortcutEditLists();
        saveButton.disableProperty().bind(hasUnsavedChanges.not());

        // Set up styles and icons
        appTitleLabel.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_BOLD, Styles.TITLE_3);

        addShortcutButton.setGraphic(new FontIcon(Feather.PLUS));
        addShortcutButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.ACCENT);
        Tooltip.install(addShortcutButton, new Tooltip(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_ADD_BUTTON_TOOLTIP)));

        saveButton.setGraphic(new FontIcon(Feather.SAVE));
        saveButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.SUCCESS);
        Tooltip.install(saveButton, new Tooltip(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_SAVE_BUTTON_TOOLTIP)));

        shortcutsTable.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        shortcutsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        log.debug("AppShortcutEditorController initialization complete");
    }

    /**
     * Initializes the view with application data.
     * Sets up the table, loads shortcuts for the application, and configures the window.
     *
     * @param appInfo Information about the application whose shortcuts are being edited
     * @param appIcon The icon of the application
     */
    public void initData(AppShortcuts appInfo, Image appIcon) {
        log.debug("Initializing data for app: {}", appInfo.getAppName());
        this.currentApp = appInfo;
        this.headerAppIconView.setImage(appIcon);
        Image shortcutBuddyIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png")));

        Stage stage = getStage();
        stage.setTitle(MessageFormat.format(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_WINDOW_TITLE), appInfo.getAppDescription()));
        stage.getIcons().add(shortcutBuddyIcon);
        appTitleLabel.setText(MessageFormat.format(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_WINDOW_LABEL), appInfo.getAppDescription()));

        setupTableColumns();

        log.debug("Loading shortcuts for app: {}", appInfo.getAppName());
        List<Shortcut> shortcuts = shortcutManager.getShortcutsForApp(appInfo.getAppName());
        this.shortcutList = FXCollections.observableArrayList(shortcuts);
        log.debug("Loaded {} shortcuts", shortcuts.size());

        this.shortcutList.addListener((ListChangeListener<Shortcut>) c -> markUnsavedChanges(true));

        shortcutsTable.setItems(this.shortcutList);
        hasUnsavedChanges.set(false);

        // Set up close request handler to prompt for saving changes
        stage.setOnCloseRequest(event -> {
            AtomicReference<Boolean> saveChanges = new AtomicReference<>(false);
            if (hasUnsavedChanges.get()) {
                log.debug("Unsaved changes detected on window close");
                String caption = MessageFormat.format(resources.getString("dialog.edit.shortcut.alert.title"), AppInfo.getName());
                var textArea = new javafx.scene.control.TextArea();
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setText(resources.getString("dialog.edit.shortcut.alert.message"));
                DialogUtils.showActionDialog(caption, textArea, Optional.ofNullable(this.getApplicationIcon()), Optional.of(getStage()), e -> saveChanges.set(true));
                if(saveChanges.get()){
                    log.debug("User chose to save changes before closing");
                    handleSaveButtonAction();
                } else {
                    log.debug("User chose to discard changes");
                }
            }
        });

        log.debug("App data initialization complete");
    }

    /**
     * Gets the application icon for use in dialogs.
     * 
     * @return The application icon image, or null if it cannot be loaded
     */
    private javafx.scene.image.Image getApplicationIcon() {
        log.debug("Loading application icon");
        try {
            return new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png")));
        } catch (Exception e) {
            log.error("Unable to load application icon /images/logo_128.png", e);
        }
        return null;
    }

    /**
     * Sets up the table columns for the shortcuts table.
     * Configures cell factories, value factories, and styles for each column.
     */
    private void setupTableColumns() {
        log.debug("Setting up table columns");

        // Keys column
        keysColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getKeys()));
        keysColumn.setCellFactory(column -> new ShortcutKeysTableCell());
        keysColumn.setSortable(false);
        keysColumn.setStyle(InlineCSS.FONT_MONOSPACE);

        // Description column
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setStyle(InlineCSS.FONT_MONOSPACE);

        // Category column
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColumn.setStyle(InlineCSS.FONT_MONOSPACE);

        // Actions column
        actionsColumn.setCellFactory(column -> new ActionTableCell());
        actionsColumn.setStyle(InlineCSS.FONT_MONOSPACE);
        actionsColumn.setSortable(false);

        log.debug("Table columns setup complete");
    }


    /**
     * Handles the action when the add shortcut button is clicked.
     * Creates a new shortcut and shows the edit dialog.
     */
    @FXML
    void handleAddShortcutAction() {
        log.debug("Adding new shortcut for app: {}", currentApp.getAppName());
        Shortcut newShortcut = new Shortcut();
        newShortcut.setAppId(currentApp.getAppId());

        Optional<Shortcut> result = showShortcutEditDialog(newShortcut, "dialog.addShortcut.title");

        result.ifPresent(addedShortcut -> {
            log.debug("New shortcut added: {}", addedShortcut.getDescription());
            shortcutList.add(addedShortcut);
            shortcutEditLists.addNew(addedShortcut);
        });
    }

    /**
     * Handles the action when the edit button for a shortcut is clicked.
     * Creates a copy of the shortcut for editing and shows the edit dialog.
     * 
     * @param shortcutToEdit The shortcut to edit
     */
    private void handleEditShortcutAction(Shortcut shortcutToEdit) {
        log.debug("Editing shortcut: {}", shortcutToEdit.getDescription());
        Shortcut copyForEditing = copyShortcut(shortcutToEdit);

        Optional<Shortcut> result = showShortcutEditDialog(copyForEditing, "dialog.editShortcut.title");

        result.ifPresent(editedShortcut -> {
            int index = shortcutList.indexOf(shortcutToEdit);
            if (index != -1) {
                log.debug("Shortcut updated: {}", editedShortcut.getDescription());
                shortcutList.set(index, editedShortcut);
                shortcutEditLists.addModified(editedShortcut);
            }
        });
    }

    /**
     * Handles the action when the delete button for a shortcut is clicked.
     * Shows a confirmation dialog and removes the shortcut if confirmed.
     * 
     * @param shortcutToDelete The shortcut to delete
     */
    private void handleDeleteShortcutAction(Shortcut shortcutToDelete) {
        log.debug("Attempting to delete shortcut: {}", shortcutToDelete.getDescription());
        AtomicReference<Boolean> deleteConfirmed = new AtomicReference<>(false);
        var textArea = new javafx.scene.control.TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setText(MessageFormat.format(resources.getString("alert.delete.confirm.message"), shortcutToDelete.getDescription()));
        DialogUtils.showActionDialog(resources.getString("alert.delete.confirm.title"), textArea, Optional.ofNullable(this.getApplicationIcon()), Optional.of(getStage()), e -> deleteConfirmed.set(true));

        if(deleteConfirmed.get()){
            log.debug("Deletion confirmed for shortcut: {}", shortcutToDelete.getDescription());
            shortcutList.remove(shortcutToDelete);
            shortcutEditLists.addRemoved(shortcutToDelete);
        } else {
            log.debug("Deletion cancelled for shortcut: {}", shortcutToDelete.getDescription());
        }
    }

    /**
     * Handles the action when the restore default button for a shortcut is clicked.
     * Shows a confirmation dialog and restores the default keys if confirmed.
     * 
     * @param shortcutToRestore The shortcut to restore to default keys
     */
    private void handleRestoreDefaultShortcutAction(Shortcut shortcutToRestore) {
        log.debug("Attempting to restore default keys for shortcut: {}", shortcutToRestore.getDescription());
        AtomicReference<Boolean> restoreConfirmed = new AtomicReference<>(false);
        var textArea = new javafx.scene.control.TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setText(MessageFormat.format("Are you sure you want to restore the default keys for shortcut: {0}?", shortcutToRestore.getDescription()));
        DialogUtils.showActionDialog("Restore Default Keys", textArea, Optional.ofNullable(this.getApplicationIcon()), Optional.of(getStage()), e -> restoreConfirmed.set(true));

        if(restoreConfirmed.get()){
            log.debug("Restore confirmed for shortcut: {}", shortcutToRestore.getDescription());
            boolean restored = shortcutManager.restoreDefaultShortcut(currentApp.getAppName(), shortcutToRestore.getId());
            if(restored) {
                log.debug("Default keys successfully restored");
                // Refresh the shortcut list to show the updated keys
                List<Shortcut> shortcuts = shortcutManager.getShortcutsForApp(currentApp.getAppName());
                this.shortcutList.setAll(shortcuts);
                markUnsavedChanges(false);

                // Show success message
                String caption = MessageFormat.format("Default Keys Restored", AppInfo.getName());
                var successTextArea = new javafx.scene.control.TextArea();
                successTextArea.setEditable(false);
                successTextArea.setWrapText(true);
                successTextArea.setText("The default keys have been successfully restored.");
                DialogUtils.showInfoDialog(caption, successTextArea, Optional.ofNullable(this.getApplicationIcon()), Optional.of(getStage()));
            } else {
                log.error("Failed to restore default keys for shortcut: {}", shortcutToRestore.getDescription());
            }
        } else {
            log.debug("Restore cancelled for shortcut: {}", shortcutToRestore.getDescription());
        }
    }

    /**
     * Handles the action when the save button is clicked.
     * Saves all changes to shortcuts and shows a confirmation dialog.
     */
    @FXML
    void handleSaveButtonAction() {
        log.debug("Saving shortcuts for app: {}", currentApp != null ? currentApp.getAppName() : "null");
        if (currentApp == null) {
            log.error("Cannot save shortcuts: currentApp is null");
            return;
        }

        boolean saved = shortcutManager.batchModifyShortcuts(currentApp.getAppName(), shortcutEditLists);

        if(saved) {
            log.debug("Shortcuts saved successfully");
            markUnsavedChanges(false);
        } else {
            log.error("Failed to save shortcuts");
        }

        String caption = MessageFormat.format(resources.getString("dialog.save.shortcut.alert.title"), AppInfo.getName());
        var textArea = new javafx.scene.control.TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setText(saved ?
                resources.getString("dialog.save.shortcut.alert.message.ok") :
                resources.getString("dialog.save.shortcut.alert.message.error"));
        DialogUtils.showInfoDialog(caption, textArea, Optional.ofNullable(this.getApplicationIcon()), Optional.of(getStage()));
    }


    /**
     * Marks whether there are unsaved changes in the editor.
     * This affects the state of the save button and prompts when closing the window.
     * 
     * @param changed True if there are unsaved changes, false otherwise
     */
    private void markUnsavedChanges(boolean changed) {
        log.debug("Setting unsaved changes flag to: {}", changed);
        this.hasUnsavedChanges.set(changed);
    }

    /**
     * Gets the current stage (window) for this controller.
     * 
     * @return The JavaFX Stage object
     */
    private Stage getStage() {
        return (Stage) shortcutsTable.getScene().getWindow();
    }

    /**
     * Shows a dialog for editing a shortcut.
     * 
     * @param shortcutData The shortcut data to edit
     * @param titleKey The resource key for the dialog title
     * @return An Optional containing the edited shortcut if successful, or empty if cancelled
     */
    private Optional<Shortcut> showShortcutEditDialog(Shortcut shortcutData, String titleKey) {
        log.debug("Opening shortcut edit dialog for shortcut: {}", shortcutData.getDescription());
        AtomicReference<Shortcut> shortcutRef = new AtomicReference<>(shortcutData);
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/appShortcut-editor-dialog.fxml"), resources);
            loader.setControllerFactory(appComponent.getControllerFactory());
            Parent root = loader.load();
            AppShortcutEditorDialogController editorDialogController = loader.getController();

            Stage currentStage = getStage();
            Stage editorDialogStage = new Stage();
            editorDialogStage.setTitle(resources.getString(titleKey));
            editorDialogStage.initModality(Modality.WINDOW_MODAL);
            editorDialogStage.initOwner(currentStage);
            editorDialogStage.getIcons().add(currentStage.getIcons().getFirst());

            Scene editorDialogScene = new Scene(root);
            editorDialogStage.setScene(editorDialogScene);

            log.debug("Initializing shortcut editor dialog controller");
            editorDialogController.initModalWindow(currentApp, headerAppIconView.getImage(), shortcutData, shortcut -> {
                if (shortcut != null) {
                    log.debug("Shortcut edited: {}", shortcut.getDescription());
                    shortcutRef.set(shortcut);
                } else {
                    log.debug("Shortcut edit cancelled");
                }
            });

            // Register the dialog window with the application
            ShortcutBuddyApp.getInstance().registerStage(editorDialogStage);

            log.debug("Showing shortcut edit dialog");
            currentStage.setOpacity(0.5);
            editorDialogStage.showAndWait();
            currentStage.setOpacity(1);

        } catch (Exception e){
            log.error("Error during shortcut editor dialog loading", e);
            var textArea = new javafx.scene.control.TextArea();
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setText(resources.getString("dialog.error.shortcut.message"));
            DialogUtils.showInfoDialog(resources.getString("dialog.error.title"), textArea, Optional.ofNullable(this.getApplicationIcon()), Optional.of(getStage()));
        }
        return Optional.ofNullable(shortcutRef.get());
    }


    private class ActionTableCell extends TableCell<Shortcut, Void> {
        private final Button btnEdit = new Button("", new FontIcon(Feather.EDIT));
        private final Button btnDelete = new Button("", new FontIcon(Feather.TRASH_2));
        private final Button btnRestore = new Button("", new FontIcon(Feather.REFRESH_CW));
        private final HBox pane = new HBox(5, btnEdit, btnDelete, btnRestore);

        ActionTableCell() {
            btnEdit.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.ACCENT);
            Tooltip.install(btnEdit, new Tooltip(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_EDIT_BUTTON_TOOLTIP)));
            btnEdit.setOnAction(event -> {
                Shortcut shortcut = getTableView().getItems().get(getIndex());
                handleEditShortcutAction(shortcut);
            });

            btnDelete.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.DANGER);
            Tooltip.install(btnDelete, new Tooltip(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_DELETE_BUTTON_TOOLTIP)));
            btnDelete.setOnAction(event -> {
                Shortcut shortcut = getTableView().getItems().get(getIndex());
                handleDeleteShortcutAction(shortcut);
            });

            btnRestore.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.SUCCESS);
            Tooltip.install(btnRestore, new Tooltip("Restore Default"));
            btnRestore.setOnAction(event -> {
                Shortcut shortcut = getTableView().getItems().get(getIndex());
                handleRestoreDefaultShortcutAction(shortcut);
            });

            pane.setAlignment(Pos.CENTER);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : pane);
        }
    }

    private static class ShortcutKeysTableCell extends TableCell<Shortcut, List<String>> {
        private final TextField textField = new TextField();

        public ShortcutKeysTableCell() {
            textField.setOnKeyPressed(event -> {});
        }

        @Override
        protected void updateItem(List<String> item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                textField.setText(String.join(" + ", item));
                setGraphic(textField);
            }
        }
    }

    /**
     * Creates a deep copy of a shortcut.
     * This ensures that modifications to the copy don't affect the original.
     * 
     * @param original The original shortcut to copy
     * @return A new shortcut instance with the same data
     */
    private Shortcut copyShortcut(Shortcut original) {
        return new Shortcut(
                original.getId(),
                original.getAppId(),
                original.getDescription(),
                new ArrayList<>(original.getKeys()),
                original.getCategory()
        );
    }
}
