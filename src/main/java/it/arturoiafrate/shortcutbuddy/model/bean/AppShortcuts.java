package it.arturoiafrate.shortcutbuddy.model.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AppShortcuts {
    private String appName;
    private String appDescription;
    private List<Shortcut> shortcuts;
    private Long usageCount;
    private boolean userDefined;
}
