CREATE TABLE applications (
  app_id INTEGER PRIMARY KEY AUTOINCREMENT,
  app_name TEXT NOT NULL UNIQUE,
  app_description TEXT,
  usage_count INTEGER DEFAULT 0,
  user_defined NUMBER(1) DEFAULT 0
);

create INDEX idx_app_name ON applications (app_name);

CREATE TABLE shortcuts (
   shortcut_id INTEGER PRIMARY KEY AUTOINCREMENT,
   app_id INTEGER NOT NULL,
   keys_storage TEXT NOT NULL,
   description TEXT NOT NULL,
   category TEXT,
   user_defined NUMBER(1) DEFAULT 0,
   FOREIGN KEY (app_id) REFERENCES applications (app_id) ON DELETE CASCADE
);

CREATE INDEX idx_shortcuts_app_id ON shortcuts (app_id);