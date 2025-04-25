package it.arturoiafrate.shortcutbuddy.model.bean;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

@Getter
public class ShortcutEditLists {
    private ObservableList<Shortcut> added;
    private ObservableList<Shortcut> removed;
    private ObservableList<Shortcut> updated;

    public void addNew(Shortcut shortcut)  {
        if(added == null) added = FXCollections.observableArrayList();
        added.add(shortcut);
    }

    public void addRemoved(Shortcut shortcut)  {
        if(removed == null) removed = FXCollections.observableArrayList();
        removed.add(shortcut);
    }

    public void addModified(Shortcut shortcut)  {
        if(updated == null) updated = FXCollections.observableArrayList();
        updated.add(shortcut);
    }
}
