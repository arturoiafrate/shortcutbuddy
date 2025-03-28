package it.arturoiafrate.shortcutbuddy.model.enumerator;

import java.util.Arrays;
import java.util.Locale;

public enum Languages {
    english("english");

    private final String language;

    Languages(String language) {
        this.language = language;
    }

    public boolean equals(String language) {
        return this.language.equalsIgnoreCase(language);
    }

    public String getLanguage() {
        return language;
    }

    private static Languages fromString(String language) {
        return Arrays.stream(Languages.values())
                .filter(l -> l.equals(language))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No enum constant for language: " + language));
    }

    public static Locale getLocale(String language) {
        return Arrays.stream(Languages.values())
                .filter(l -> l.equals(language))
                .findFirst()
                .map(l -> Locale.of(l.getLanguage()))
                .orElseThrow(() -> new IllegalArgumentException("No locale found for language: " + language));
    }
}
