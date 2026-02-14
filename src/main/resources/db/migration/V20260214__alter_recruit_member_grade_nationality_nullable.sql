ALTER TABLE IF EXISTS recruit_member
    DROP COLUMN IF EXISTS grade,
    DROP COLUMN IF EXISTS nationality;

ALTER TABLE IF EXISTS answer
    DROP COLUMN IF EXISTS gdg_user_motive,
    DROP COLUMN IF EXISTS gdg_user_story,
    DROP COLUMN IF EXISTS gdg_period,
    DROP COLUMN IF EXISTS gdg_route,
    DROP COLUMN IF EXISTS gdg_expect;
