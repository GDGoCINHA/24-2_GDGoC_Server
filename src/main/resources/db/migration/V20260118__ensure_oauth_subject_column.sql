DO $$
DECLARE
    has_snake BOOLEAN;
    has_lower BOOLEAN;
    has_camel BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'users'
          AND column_name = 'oauth_subject'
    )
    INTO has_snake;

    IF NOT has_snake THEN
        SELECT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'users'
              AND column_name = 'oauthsubject'
        )
        INTO has_lower;

        SELECT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'users'
              AND column_name = 'oauthSubject'
        )
        INTO has_camel;

        IF has_lower THEN
            EXECUTE 'ALTER TABLE users RENAME COLUMN oauthsubject TO oauth_subject';
        ELSIF has_camel THEN
            EXECUTE 'ALTER TABLE users RENAME COLUMN "oauthSubject" TO oauth_subject';
        ELSE
            ALTER TABLE users
                ADD COLUMN oauth_subject VARCHAR(255);
        END IF;
    END IF;

    -- Fallback for any NULL values (legacy data without OAuth subject)
    UPDATE users
    SET oauth_subject = CONCAT('legacy-', id)
    WHERE oauth_subject IS NULL;

    ALTER TABLE users
        ALTER COLUMN oauth_subject SET NOT NULL;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'public'
          AND tablename = 'users'
          AND indexname = 'uk_users_oauth_subject'
    ) THEN
        EXECUTE 'ALTER TABLE users ADD CONSTRAINT uk_users_oauth_subject UNIQUE (oauth_subject)';
    END IF;
END;
$$;
