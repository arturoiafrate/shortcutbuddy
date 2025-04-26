package it.arturoiafrate.shortcutbuddy.model.bean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Shortcut {
    private long id;
    private long appId;
    private String description;
    private List<String> keys;
    private String category;
    private List<String> defaultKeys;
    private boolean starred;

    public Shortcut(long id, long appId, String description, List<String> keys, String category){
        this.id = id;
        this.appId = appId;
        this.description = description;
        this.keys = keys;
        this.category = category;
        this.starred = false;
    }

    public Shortcut(long id, long appId, String description, List<String> keys, String category, List<String> defaultKeys){
        this.id = id;
        this.appId = appId;
        this.description = description;
        this.keys = keys;
        this.category = category;
        this.defaultKeys = defaultKeys;
        this.starred = false;
    }

    public Shortcut(long id, long appId, String description, List<String> keys, String category, List<String> defaultKeys, boolean starred){
        this.id = id;
        this.appId = appId;
        this.description = description;
        this.keys = keys;
        this.category = category;
        this.defaultKeys = defaultKeys;
        this.starred = starred;
    }
}
