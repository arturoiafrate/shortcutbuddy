UPDATE shortcuts SET keys_storage = '["Alt", "Down"]' WHERE keys_storage = '["Alt", "Down Arrow"]';

INSERT INTO applications (app_name, app_description) VALUES ('whatsapp.exe', 'WhatsApp');

INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Ctrl", "N"]', 'Start a new chat', 'chat management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Ctrl", "E"]', 'Archive the current chat', 'chat management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Ctrl", "Shift", "M"]', 'Mute the current chat', 'chat management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Ctrl", "Shift", "U"]', 'Mark current chat as unread', 'chat management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Ctrl", "Backspace"]', 'Delete the current chat', 'chat management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Ctrl", "Shift", "N"]', 'Create a new group', 'group management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Ctrl", "P"]', 'Open your profile/status', 'navigation');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Ctrl", "F"]', 'Search within the current chat', 'search');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Ctrl", "Tab"]', 'Go to the next chat', 'navigation');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Ctrl", "Shift", "Tab"]', 'Go to the previous chat', 'navigation');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Esc"]', 'Close current view', 'navigation');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Alt", "F4"]', 'Close WhatsApp', 'application control');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'whatsapp.exe'), '["Shift", "Enter"]', 'Insert a new line', 'message composition');

INSERT INTO applications (app_name, app_description) VALUES ('notepad.exe', 'Notepad');

INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "N"]', 'New window', 'file management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "O"]', 'Open file', 'file management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "S"]', 'Save', 'file management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "Shift", "S"]', 'Save As', 'file management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "P"]', 'Print', 'file management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "Shift", "N"]', 'Open a new tab', 'tab management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "W"]', 'Close the current tab', 'tab management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "Tab"]', 'Switch to the next tab', 'tab management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "Shift", "Tab"]', 'Switch to the previous tab', 'tab management');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "Z"]', 'Undo the last action', 'editing');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "X"]', 'Cut selected text', 'editing');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "C"]', 'Copy selected text', 'editing');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "V"]', 'Paste text from clipboard', 'editing');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Del"]', 'Delete selected text', 'editing');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "A"]', 'Select all text in the document', 'editing');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["F5"]', 'Insert the current time and date', 'text insertion');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "F"]', 'Open Find dialog', 'search');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["F3"]', 'Find next', 'search');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Shift", "F3"]', 'Find previous', 'search');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "H"]', 'Open Replace dialog', 'search');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "+"]', 'Zoom in', 'view adjustment');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "-"]', 'Zoom out', 'view adjustment');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Ctrl", "0"]', 'Restore default zoom level', 'view adjustment');
INSERT INTO shortcuts (app_id, keys_storage, description, category) VALUES ((SELECT app_id from applications where app_name = 'notepad.exe'), '["Alt", "F4"]', 'Close the Notepad window', 'application control');