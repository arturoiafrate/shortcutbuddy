package it.arturoiafrate.shortcutbuddy.controller.dialog;

import it.arturoiafrate.shortcutbuddy.model.bean.AppShortcuts;
import javafx.scene.control.Dialog;

public class AppShortcutDialog extends Dialog<AppShortcuts> {
    public AppShortcutDialog() {
        setTitle("Add Shortcut");
        setHeaderText("Add a new shortcut to the application");
        this.setResultConverter(buttonType -> null);
    }
}
