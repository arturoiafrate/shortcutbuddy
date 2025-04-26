-- Add default_value column to shortcuts table
ALTER TABLE shortcuts ADD COLUMN default_value TEXT;

-- Update existing shortcuts to set default_value equal to keys_storage
UPDATE shortcuts SET default_value = keys_storage;

-- Add starred column to shortcuts table with default value of false
ALTER TABLE shortcuts ADD COLUMN starred BOOLEAN DEFAULT false;

-- Update existing shortcuts to set starred to false
UPDATE shortcuts SET starred = false;