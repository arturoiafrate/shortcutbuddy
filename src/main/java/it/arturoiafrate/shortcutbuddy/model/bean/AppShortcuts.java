package it.arturoiafrate.shortcutbuddy.model.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppShortcuts {
    private Long appId;
    private String appName;
    private String appDescription;
    private List<Shortcut> shortcuts;
    private Long usageCount;
    private boolean userDefined;
    private String appIconPath;

    public AppShortcuts(Long appId, String appName, String appDescription, List<Shortcut> shortcuts, Long usageCount, boolean userDefined) {
        this.appId = appId;
        this.appName = appName;
        this.appDescription = appDescription;
        this.shortcuts = shortcuts;
        this.usageCount = usageCount;
        this.userDefined = userDefined;
    }

    public AppShortcuts(String appName, String appDescription, List<Shortcut> shortcuts, Long usageCount, boolean userDefined) {
        this.appName = appName;
        this.appDescription = appDescription;
        this.shortcuts = shortcuts;
        this.usageCount = usageCount;
        this.userDefined = userDefined;
    }
}
