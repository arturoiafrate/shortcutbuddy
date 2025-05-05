ALTER TABLE settings ADD COLUMN dev_mode BOOLEAN DEFAULT false NOT NULL;
ALTER TABLE settings ADD COLUMN conditional_enabling TEXT;

CREATE TABLE clipboard_history (
                                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                                   content_type TEXT NOT NULL,
                                   content TEXT NOT NULL,
                                   timestamp INTEGER NOT NULL
);

CREATE INDEX idx_clipboard_history_timestamp ON clipboard_history (timestamp);
CREATE INDEX idx_clipboard_history_content_type ON clipboard_history (content_type);

INSERT INTO settings (key, value, is_readonly, is_hidden, value_type, allowed_options, setting_order, group_name, dev_mode, conditional_enabling) VALUES
    ('enableClipboardManager', 'n', 0, 0, 'BOOLEAN_YN', '["y","n"]', 10, 'clipboardManager', true, null),
    ('clipboardHistorySize', '25', 0, 0, 'CHOICE', '["10","25","50","100","500"]', 11, 'clipboardManager', true, 'enableClipboardManager'),
    ('enableSnippetManager', 'n', 0, 0, 'BOOLEAN_YN', '["y","n"]', 12, 'clipboardManager', true, 'enableClipboardManager'),
    ('snippet_1', '', 0, 0, 'PICKER', null, 13, 'clipboardManager', true, 'enableSnippetManager'),
    ('snippet_2', '', 0, 0, 'PICKER', null, 14, 'clipboardManager', true, 'enableSnippetManager'),
    ('snippet_3', '', 0, 0, 'PICKER', null, 15, 'clipboardManager', true, 'enableSnippetManager'),
    ('snippet_4', '', 0, 0, 'PICKER', null, 16, 'clipboardManager', true, 'enableSnippetManager'),
    ('snippet_5', '', 0, 0, 'PICKER', null, 17, 'clipboardManager', true, 'enableSnippetManager');