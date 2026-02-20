CREATE TABLE IF NOT EXISTS admin_credentials (
    id BIGSERIAL PRIMARY KEY,
    login_id VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_credentials_login_id
    ON admin_credentials(login_id);

DO $$
DECLARE
    constraint_name text;
BEGIN
    IF to_regclass('public.admin_credentials') IS NULL THEN
        RETURN;
    END IF;

    FOR constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'public'
          AND t.relname = 'admin_credentials'
          AND c.conkey IS NOT NULL
          AND EXISTS (
              SELECT 1
              FROM unnest(c.conkey) AS colnum(attnum)
              JOIN pg_attribute a
                ON a.attrelid = t.oid
               AND a.attnum = colnum.attnum
              WHERE a.attname = 'user_id'
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE public.admin_credentials DROP CONSTRAINT IF EXISTS %I',
            constraint_name
        );
    END LOOP;
END $$;

ALTER TABLE IF EXISTS public.admin_credentials
    DROP COLUMN IF EXISTS user_id;
