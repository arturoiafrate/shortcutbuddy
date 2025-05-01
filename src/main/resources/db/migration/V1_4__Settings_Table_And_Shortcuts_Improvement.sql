
ALTER TABLE shortcuts ADD COLUMN usage_count INTEGER DEFAULT 0;

CREATE TABLE settings (
    key     TEXT PRIMARY KEY NOT NULL,
    value   TEXT,
    is_readonly     BOOLEAN DEFAULT false NOT NULL,
    is_hidden       BOOLEAN DEFAULT false NOT NULL,
    value_type      TEXT DEFAULT 'TEXT' NOT NULL,
    allowed_options TEXT NULL,
    group_name      TEXT DEFAULT 'general' NOT NULL,
    setting_order           INTEGER DEFAULT 0 NOT NULL
);

INSERT INTO settings (key, value, is_readonly, is_hidden, value_type, allowed_options, setting_order) VALUES
 ('width', '720', 0, 0, 'NUMBER_INT', NULL, 1),
 ('height', '600', 0, 0, 'NUMBER_INT', NULL,2),
 ('theme', 'light', 0, 0, 'CHOICE', '["light","dark"]', 3),
 ('language', 'english', 0, 0, 'CHOICE', '["english","italiano"]', 4),
 ('searchKey', '.', 0, 0, 'CHOICE', '[".","space","-","P"]', 5),
 ('enableNotification', 'y', 0, 0, 'BOOLEAN_YN', '["y","n"]', 6),
 ('checkForUpdates', 'n', 0, 0, 'BOOLEAN_YN', '["y","n"]', 7),
 ('cacheSize', '20', 0, 0, 'CHOICE', '["10","20","30","50"]', 8),
 ('preloadAppsNumber', '5', 0, 0, 'CHOICE', '["disabled","5","10","20"]', 9),
 ('app.internal.lastVersion', '1.0.0', 1, 1, 'TEXT', NULL, 0);
