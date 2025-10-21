-- 1) users.user_role 관련 기존 CHECK 제약 전부 제거
DO $$ DECLARE r record;
BEGIN FOR r IN
SELECT conname
FROM pg_constraint c
         JOIN pg_class t ON t.oid = c.conrelid
         JOIN pg_namespace n ON n.oid = t.relnamespace
WHERE t.relname = 'users'
  AND n.nspname = 'public'
  AND c.contype = 'c'
  AND pg_get_constraintdef(c.oid) ILIKE '%user_role%'
  LOOP
EXECUTE format('ALTER TABLE public.users DROP CONSTRAINT %I', r.conname);
END LOOP;
END $$;

-- 2) user_role 기본값 제거 후 타입을 VARCHAR(32)로 통일
ALTER TABLE public.users
    ALTER COLUMN user_role DROP DEFAULT;
ALTER TABLE public.users
    ALTER COLUMN user_role TYPE varchar (32)
    USING user_role:: text;

-- 3) user_role 기본값 및 새로운 CHECK 제약 생성
ALTER TABLE public.users
    ALTER COLUMN user_role SET DEFAULT 'GUEST';

DO $$
BEGIN IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'users_user_role_check'
      AND conrelid = 'public.users'::regclass
  ) THEN
ALTER TABLE public.users
    ADD CONSTRAINT users_user_role_check
        CHECK (user_role IN ('GUEST', 'MEMBER', 'CORE', 'LEAD', 'ORGANIZER', 'ADMIN'));
END IF;
END $$;

-- 4) 팀 컬럼 추가(널 허용). 값은 enum name 저장: 'HR','PR_DESIGN','TECH','BD'
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS team varchar (32);

-- 5) 팀 CHECK 제약 (NULL 허용)
DO $$
BEGIN IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'users_team_check'
      AND conrelid = 'public.users'::regclass
  ) THEN
ALTER TABLE public.users
    ADD CONSTRAINT users_team_check
        CHECK (
            team IS NULL
                OR team IN ('HR', 'PR_DESIGN', 'TECH', 'BD')
            );
END IF;
END $$;