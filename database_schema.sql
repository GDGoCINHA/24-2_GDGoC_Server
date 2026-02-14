-- GDGoC Inha Univ. Database Schema
-- DBMS: PostgreSQL

-- 1. 유저 (users)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    oauth_subject VARCHAR(255) NOT NULL UNIQUE,
    major VARCHAR(255) NOT NULL,
    student_id VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    user_role VARCHAR(50) NOT NULL DEFAULT 'GUEST',
    team VARCHAR(50),
    membership_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    image TEXT,
    socials JSONB,
    careers JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_student_id ON users(student_id);
CREATE INDEX IF NOT EXISTS idx_users_phone_number ON users(phone_number);
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users((lower(email)));

-- 2. 리크루팅 멤버 (recruit_member)
CREATE TABLE IF NOT EXISTS recruit_member (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    grade VARCHAR(50) NOT NULL,
    student_id VARCHAR(255) NOT NULL UNIQUE,
    enrolled_classification VARCHAR(50) NOT NULL,
    phone_number VARCHAR(255) NOT NULL UNIQUE,
    nationality VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    birth DATE NOT NULL,
    major VARCHAR(255) NOT NULL,
    is_payed BOOLEAN NOT NULL DEFAULT FALSE,
    admission_semester VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_recruit_member_email_lower ON recruit_member((lower(email)));
CREATE INDEX IF NOT EXISTS idx_recruit_member_created_at ON recruit_member(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_recruit_member_name_lower ON recruit_member((lower(name)));

-- 3. 답변 (answer) - recruit_member와 1:N
CREATE TABLE IF NOT EXISTS answer (
    id BIGSERIAL PRIMARY KEY,
    recruit_member BIGINT REFERENCES recruit_member(id) ON DELETE CASCADE,
    survey_type VARCHAR(50) NOT NULL,
    input_type VARCHAR(50) NOT NULL,
    response_value JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_answer_recruit_member_survey_type
    ON answer(recruit_member, survey_type);

-- 4. 코어 멤버 지원 (core_recruit_applications)
CREATE TABLE IF NOT EXISTS core_recruit_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    session VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    student_id VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    major VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    team VARCHAR(255) NOT NULL,
    motivation TEXT NOT NULL,
    wish TEXT NOT NULL,
    strengths TEXT NOT NULL,
    pledge TEXT NOT NULL,
    file_urls JSONB NOT NULL DEFAULT '[]',
    result_status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    reviewed_at TIMESTAMPTZ,
    reviewed_by BIGINT,
    result_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_core_recruit_user_session
    ON core_recruit_applications(user_id, session);
CREATE INDEX IF NOT EXISTS idx_core_recruit_session_status_team_created
    ON core_recruit_applications(session, result_status, team, created_at DESC);

-- 5. 스터디 (study)
CREATE TABLE IF NOT EXISTS study (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    simple_introduce VARCHAR(512),
    activity_introduce TEXT,
    image_path VARCHAR(256),
    creator_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expected_time VARCHAR(100),
    expected_place VARCHAR(100),
    recruit_start_date TIMESTAMP,
    recruit_end_date TIMESTAMP,
    activity_start_date TIMESTAMP,
    activity_end_date TIMESTAMP,
    user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. 스터디 참여자 (study_attendee)
CREATE TABLE IF NOT EXISTS study_attendee (
    id BIGSERIAL PRIMARY KEY,
    study_id BIGINT REFERENCES study(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    introduce TEXT,
    activity_time VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. 정기 회의 (meetings)
CREATE TABLE IF NOT EXISTS meetings (
    id BIGSERIAL PRIMARY KEY,
    meeting_date DATE NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_meeting_date ON meetings(meeting_date);

-- 8. 출석 기록 (attendance_records)
CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    present BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id),
    UNIQUE (meeting_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_attendance_user_meeting ON attendance_records(user_id, meeting_id);

-- 9. 인증 코드 (auth_code)
CREATE TABLE IF NOT EXISTS auth_code (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. 리프레시 토큰 (refresh_token)
CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    expiry_date TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. 마니또 세션 (manito_sessions)
CREATE TABLE IF NOT EXISTS manito_sessions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_manito_sessions_created_at ON manito_sessions(created_at DESC);

-- 12. 마니또 배정 (manito_assignments)
CREATE TABLE IF NOT EXISTS manito_assignments (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES manito_sessions(id),
    student_id VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    encrypted_manitto TEXT,
    pin_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (session_id, student_id)
);
CREATE INDEX IF NOT EXISTS idx_manito_assignments_session ON manito_assignments(session_id);
CREATE INDEX IF NOT EXISTS idx_manito_assignments_student ON manito_assignments(student_id);

-- 13. 게임 문제 (game_question)
CREATE TABLE IF NOT EXISTS game_question (
    id BIGSERIAL PRIMARY KEY,
    language VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    result VARCHAR(255) NOT NULL
);

-- 14. 게임 유저 (game_user)
CREATE TABLE IF NOT EXISTS game_user (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    major VARCHAR(255) NOT NULL,
    student_id VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    typing_speed FLOAT8 NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 15. 방명록 (guestbook_entry)
CREATE TABLE IF NOT EXISTS guestbook_entry (
    id BIGSERIAL PRIMARY KEY,
    wristband_serial VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    won_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_guestbook_created_at ON guestbook_entry(created_at);

-- 16. 신입생 지원 알림 신청 (recruit_member_memo)
CREATE TABLE IF NOT EXISTS recruit_member_memo (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    privacy_agreement BOOLEAN NOT NULL,
    freshman_memo_agreement BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_recruit_member_memo_phone_number
    ON recruit_member_memo(phone_number);
CREATE INDEX IF NOT EXISTS idx_recruit_member_memo_email_lower
    ON recruit_member_memo((lower(email)));
CREATE INDEX IF NOT EXISTS idx_recruit_member_memo_created_at
    ON recruit_member_memo(created_at DESC);
