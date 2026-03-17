ALTER TABLE attendance_records
    ADD COLUMN IF NOT EXISTS status VARCHAR(32);

UPDATE attendance_records
SET status = CASE
    WHEN status IS NOT NULL THEN status
    WHEN present THEN 'PRESENT'
    ELSE 'ABSENT'
END;

ALTER TABLE attendance_records
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE attendance_records
    ALTER COLUMN status SET DEFAULT 'ABSENT';

ALTER TABLE attendance_records
    DROP COLUMN IF EXISTS present;
