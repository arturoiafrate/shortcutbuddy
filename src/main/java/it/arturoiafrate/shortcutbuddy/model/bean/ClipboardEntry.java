package it.arturoiafrate.shortcutbuddy.model.bean;

import it.arturoiafrate.shortcutbuddy.model.enumerator.ClipboardContentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClipboardEntry {
    private Long id;
    private ClipboardContentType contentType; // Campo per il tipo
    private String content;
    private long timestamp;

    public ClipboardEntry(ClipboardContentType contentType, String content, long timestamp) {
        this.id = null;
        this.contentType = contentType;
        this.content = content;
        this.timestamp = timestamp;
    }
}
