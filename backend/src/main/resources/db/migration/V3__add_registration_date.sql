-- Add registration_date column to app_user table
ALTER TABLE app_user ADD COLUMN registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Update existing records to have a registration date
UPDATE app_user SET registration_date = CURRENT_TIMESTAMP WHERE registration_date IS NULL;