-- Ensure users table contains membership_status column for new enum responses
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS membership_status VARCHAR(32);

ALTER TABLE users
    ALTER COLUMN membership_status SET DEFAULT 'PENDING';

UPDATE users
SET membership_status = 'PENDING'
WHERE membership_status IS NULL;

ALTER TABLE users
    ALTER COLUMN membership_status SET NOT NULL;
