package it.arturoiafrate.shortcutbuddy.model.bean;

import java.util.List;

public record Shortcut(String shortcut, String description, List<String> keys) {
}
