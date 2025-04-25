package it.arturoiafrate.shortcutbuddy.controller;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
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

    @Inject
    public AppShortcutEditorController() {}

    @FXML
    private void initialize() {
        shortcutEditLists = new ShortcutEditLists();
        saveButton.disableProperty().bind(hasUnsavedChanges.not());
        appTitleLabel.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_BOLD, Styles.TITLE_3);
        addShortcutButton.setGraphic(new FontIcon(Feather.PLUS));
        addShortcutButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.ACCENT);
        Tooltip.install(addShortcutButton, new Tooltip(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_ADD_BUTTON_TOOLTIP)));
        saveButton.setGraphic(new FontIcon(Feather.SAVE));
        saveButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.SUCCESS);
        Tooltip.install(saveButton, new Tooltip(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_SAVE_BUTTON_TOOLTIP)));
        shortcutsTable.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        shortcutsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    }

    public void initData(AppShortcuts appInfo, Image appIcon) {
        this.currentApp = appInfo;
        this.headerAppIconView.setImage(appIcon);
        Image shortcutBuddyIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png")));

        Stage stage = getStage();
        stage.setTitle(MessageFormat.format(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_WINDOW_TITLE), appInfo.getAppDescription()));
        stage.getIcons().add(shortcutBuddyIcon);
        appTitleLabel.setText(MessageFormat.format(resources.getString(it.arturoiafrate.shortcutbuddy.model.constant.Label.SHORTCUT_EDITOR_WINDOW_LABEL), appInfo.getAppDescription()));

        setupTableColumns();

        List<Shortcut> shortcuts = shortcutManager.getShortcutsForApp(appInfo.getAppName());
        this.shortcutList = FXCollections.observableArrayList(shortcuts);

        this.shortcutList.addListener((ListChangeListener<Shortcut>) c -> markUnsavedChanges(true));

        shortcutsTable.setItems(this.shortcutList);
        hasUnsavedChanges.set(false);

        stage.setOnCloseRequest(event -> {
            AtomicReference<Boolean> saveChanges = new AtomicReference<>(false);
            if (hasUnsavedChanges.get()) {
                String caption = MessageFormat.format(resources.getString("dialog.edit.shortcut.alert.title"), AppInfo.getName());
                var textArea = new javafx.scene.control.TextArea();
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setText(resources.getString("dialog.edit.shortcut.alert.message"));
                DialogUtils.showActionDialog(caption, textArea, Optional.ofNullable(this.getApplicationIcon()), Optional.of(getStage()), e -> saveChanges.set(true));
                if(saveChanges.get()){
                    handleSaveButtonAction();
                }
            }
        });
    }

    private javafx.scene.image.Image getApplicationIcon() {
        try {
            return new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png")));
        } catch (Exception e) {
            log.error("Impossibile caricare l'icona dell'applicazione /images/logo_128.png", e);
        }
        return null;
    }

    private void setupTableColumns() {
        keysColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getKeys()));
        keysColumn.setCellFactory(column -> new ShortcutKeysTableCell());
        keysColumn.setSortable(false);
        keysColumn.setStyle(InlineCSS.FONT_MONOSPACE);
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setStyle(InlineCSS.FONT_MONOSPACE);
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColumn.setStyle(InlineCSS.FONT_MONOSPACE);
        actionsColumn.setCellFactory(column -> new ActionTableCell());
        actionsColumn.setStyle(InlineCSS.FONT_MONOSPACE);
        actionsColumn.setSortable(false);
    }


    @FXML
    void handleAddShortcutAction() {
        Shortcut newShortcut = new Shortcut();
        newShortcut.setAppId(currentApp.getAppId());

        Optional<Shortcut> result = showShortcutEditDialog(newShortcut, "dialog.addShortcut.title");

        result.ifPresent(addedShortcut -> {
            shortcutList.add(addedShortcut);
            shortcutEditLists.addNew(addedShortcut);
        });
    }

    private void handleEditShortcutAction(Shortcut shortcutToEdit) {
        Shortcut copyForEditing = copyShortcut(shortcutToEdit);

        Optional<Shortcut> result = showShortcutEditDialog(copyForEditing, "dialog.editShortcut.title");

        result.ifPresent(editedShortcut -> {
            int index = shortcutList.indexOf(shortcutToEdit);
            if (index != -1) {
                shortcutList.set(index, editedShortcut);
                shortcutEditLists.addModified(editedShortcut);
            }
        });
    }

    private void handleDeleteShortcutAction(Shortcut shortcutToDelete) {
        AtomicReference<Boolean> deleteConfirmed = new AtomicReference<>(false);
        var textArea = new javafx.scene.control.TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setText(MessageFormat.format(resources.getString("alert.delete.confirm.message"), shortcutToDelete.getDescription()));
        DialogUtils.showActionDialog(resources.getString("alert.delete.confirm.title"), textArea, Optional.ofNullable(this.getApplicationIcon()), Optional.of(getStage()), e -> deleteConfirmed.set(true));
        if(deleteConfirmed.get()){
            shortcutList.remove(shortcutToDelete);
            shortcutEditLists.addRemoved(shortcutToDelete);
        }
    }

    @FXML
    void handleSaveButtonAction() {
        if (currentApp == null) return;
        boolean saved = shortcutManager.batchModifyShortcuts(currentApp.getAppName(), shortcutEditLists);
        if(saved) markUnsavedChanges(false);
        String caption = MessageFormat.format(resources.getString("dialog.save.shortcut.alert.title"), AppInfo.getName());
        var textArea = new javafx.scene.control.TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setText(saved ?
                resources.getString("dialog.save.shortcut.alert.message.ok") :
                resources.getString("dialog.save.shortcut.alert.message.error"));
        DialogUtils.showInfoDialog(caption, textArea, Optional.ofNullable(this.getApplicationIcon()), Optional.of(getStage()));
    }


    private void markUnsavedChanges(boolean changed) {
        this.hasUnsavedChanges.set(changed);
    }

    private Stage getStage() {
        return (Stage) shortcutsTable.getScene().getWindow();
    }

    private Optional<Shortcut> showShortcutEditDialog(Shortcut shortcutData, String titleKey) {
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
            editorDialogController.initModalWindow(currentApp, headerAppIconView.getImage(), shortcutData, shortcut -> {
                if (shortcut != null) {
                    shortcutRef.set(shortcut);
                }
            });
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
        private final HBox pane = new HBox(5, btnEdit, btnDelete);

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

    private Shortcut copyShortcut(Shortcut original) {
        return new Shortcut(
                original.getId(),
                original.getAppId(),
                original.getDescription(),
                new ArrayList<>(original.getKeys()), // Copia la lista
                original.getCategory()
        );
    }
}