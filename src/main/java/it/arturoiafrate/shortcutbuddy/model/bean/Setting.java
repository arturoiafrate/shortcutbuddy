package it.arturoiafrate.shortcutbuddy.model.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Setting {
    private final String key;
    private String value;
    private String valueType;
    private final boolean readonly;
    private final String[] options;
    private final boolean isHide;
    private final int order;
    private String groupName;
}
