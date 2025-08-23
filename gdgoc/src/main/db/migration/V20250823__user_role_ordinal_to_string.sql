DO $$
DECLARE r record;
BEGIN
  FOR r IN
    SELECT conname
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    WHERE t.relname = 'users'
      AND n.nspname = 'public'
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) ILIKE '%user_role%'  -- user_role 관련 CHECK
  LOOP
    EXECUTE format('ALTER TABLE public.users DROP CONSTRAINT %I', r.conname);
  END LOOP;
END $$;

ALTER TABLE public.users ALTER COLUMN user_role DROP DEFAULT;

ALTER TABLE public.users
    ALTER COLUMN user_role TYPE varchar(32)
  USING user_role::text;


UPDATE public.users
SET user_role = CASE user_role
                    WHEN '0' THEN 'GUEST'
                    WHEN '1' THEN 'MEMBER'
                    WHEN '2' THEN 'ADMIN'
                    ELSE user_role
    END;

-- 5) 새 디폴트/체크 제약조건 설정
ALTER TABLE public.users ALTER COLUMN user_role SET DEFAULT 'GUEST';

ALTER TABLE public.users
    ADD CONSTRAINT users_user_role_check
        CHECK (user_role IN ('GUEST','MEMBER','ADMIN'));
