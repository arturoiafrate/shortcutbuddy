package it.arturoiafrate.shortcutbuddy.model.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Setting {
    private final String key;
    private final String value;
    private boolean readonly;
    private final String[] options;
    private boolean isHide;
    private final int order;
}
