package it.arturoiafrate.shortcutbuddy.model.bean;

import java.util.List;

public record ReleaseInfo(String version, String date, List<String> notes) {
}
