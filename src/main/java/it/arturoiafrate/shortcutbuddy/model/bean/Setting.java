package it.arturoiafrate.shortcutbuddy.model.bean;

import org.apache.commons.lang3.StringUtils;

public record Setting(String key, String value, Boolean readonly, String[] options) {
    public Setting {
        if (StringUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        if(readonly == null) readonly = false;
    }
}
