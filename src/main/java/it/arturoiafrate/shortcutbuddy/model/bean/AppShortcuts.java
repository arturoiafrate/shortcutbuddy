package it.arturoiafrate.shortcutbuddy.model.bean;

import java.util.List;

public record AppShortcuts(String appName, String appDescription, List<Shortcut> shortcuts) {
}
