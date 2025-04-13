package it.arturoiafrate.shortcutbuddy.controller;

import atlantafx.base.theme.Styles;
import it.arturoiafrate.shortcutbuddy.controller.dialog.AppShortcutDialog;
import it.arturoiafrate.shortcutbuddy.model.bean.AppShortcuts;
import it.arturoiafrate.shortcutbuddy.model.manager.shortcut.ShortcutManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;
import java.util.ResourceBundle;

public class UserShortcutsController {

    @FXML private ResourceBundle resources;

    @FXML private Button addButton;

    private ObservableList<AppShortcuts> userDefinedShortcuts;

    @FXML
    private void initialize(){
        userDefinedShortcuts = FXCollections.observableArrayList(ShortcutManager.getInstance().loadUserDefinedShortcuts());
        addButton.setGraphic(new FontIcon(Feather.PLUS));
        addButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.ACCENT);
    }

    @FXML
    void handleAddButtonAction(ActionEvent event) {
        Optional<AppShortcuts> newShortcut = new AppShortcutDialog().showAndWait();
        if (newShortcut.isPresent()) {
            //todo
        }
    }
}
