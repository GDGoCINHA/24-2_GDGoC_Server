-- recruit/core, recruit/member, login, signup API hot-path indexes
-- Safe additions only (no destructive DDL).

-- auth/login, signup duplicate checks
CREATE INDEX IF NOT EXISTS idx_users_student_id ON users(student_id);
CREATE INDEX IF NOT EXISTS idx_users_phone_number ON users(phone_number);
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users((lower(email)));

-- recruit/member duplicate checks + admin list/search
CREATE INDEX IF NOT EXISTS idx_recruit_member_email_lower ON recruit_member((lower(email)));
CREATE INDEX IF NOT EXISTS idx_recruit_member_created_at ON recruit_member(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_recruit_member_name_lower ON recruit_member((lower(name)));

-- recruit/member detail answers lookup
CREATE INDEX IF NOT EXISTS idx_answer_recruit_member_survey_type
    ON answer(recruit_member, survey_type);

-- recruit/core user/session lookup + admin filtering
CREATE INDEX IF NOT EXISTS idx_core_recruit_user_session
    ON core_recruit_applications(user_id, session);
CREATE INDEX IF NOT EXISTS idx_core_recruit_session_status_team_created
    ON core_recruit_applications(session, result_status, team, created_at DESC);
