
ALTER TABLE shortcuts ADD COLUMN usage_count INTEGER DEFAULT 0;

CREATE TABLE settings (
    key     TEXT PRIMARY KEY NOT NULL,
    value   TEXT,
    is_readonly     BOOLEAN DEFAULT false NOT NULL,
    is_hidden       BOOLEAN DEFAULT false NOT NULL,
    value_type      TEXT DEFAULT 'TEXT' NOT NULL,
    allowed_options TEXT NULL
);

INSERT INTO settings (key, value, is_readonly, is_hidden, value_type, allowed_options) VALUES
 ('width', '640', 0, 0, 'NUMBER_INT', NULL),
 ('height', '480', 0, 0, 'NUMBER_INT', NULL),
 ('theme', 'light', 0, 0, 'CHOICE', '["light","dark"]'),
 ('language', 'english', 0, 0, 'CHOICE', '["english"]'),
 ('searchKey', '.', 0, 0, 'CHOICE', '[".","space","-","P"]'),
 ('enableNotification', 'y', 0, 0, 'BOOLEAN_YN', '["y","n"]'),
 ('checkForUpdates', 'n', 0, 0, 'BOOLEAN_YN', '["y","n"]'),
 ('cacheSize', '20', 0, 0, 'CHOICE', '["10","20","30","50"]'),
 ('preloadAppsNumber', '5', 0, 0, 'CHOICE', '["disabled","5","10","20"]'),
 ('app.internal.lastVersion', '0.8.0', 1, 1, 'TEXT', NULL);
