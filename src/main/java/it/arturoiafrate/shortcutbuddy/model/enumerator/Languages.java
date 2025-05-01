package it.arturoiafrate.shortcutbuddy.model.enumerator;

import java.util.Arrays;
import java.util.Locale;

public enum Languages {
    english("english"),
    italiano("italiano");

    private final String language;
    private final String code;

    Languages(String language) {

        this.language = language;
        if(language.equalsIgnoreCase("english")) {;
            this.code = "en";
        } else if(language.equalsIgnoreCase("italiano")) {
            this.code = "it";
        } else {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    public boolean equals(String language) {
        return this.language.equalsIgnoreCase(language);
    }

    public String getLanguage() {
        return language;
    }

    public String getCode() {return code;}

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
                .map(l -> Locale.of(l.getCode()))
                .orElseThrow(() -> new IllegalArgumentException("No locale found for language: " + language));
    }
}
