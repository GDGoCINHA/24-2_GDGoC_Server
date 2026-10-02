-- 소모임(모임 관리).
--
-- 시스템은 막지 않고 기록·계산해서 보여준다. 정원·완주 기준 같은 규칙은 제약으로 걸지 않고
-- 화면에서 경고로 보여준다. 여기 있는 제약은 데이터가 깨지는 경우에만 둔다.
--
-- 이끔이는 users.role 이 아니라 데이터다. 개설 권한은 club_leader_grant,
-- 특정 팀의 관리 권한은 club.leader_id 로 판단한다. 일반 부원도 이끔이가 될 수 있다.

-- 기수. 날짜는 두지 않는다 — 활동 기간은 소모임마다 다르다.
CREATE TABLE IF NOT EXISTS club_term (
    id               BIGSERIAL    PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    attendance_ratio NUMERIC(3,2) NOT NULL DEFAULT 0.50,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club (
    id              BIGSERIAL    PRIMARY KEY,
    term_id         BIGINT       NOT NULL REFERENCES club_term(id),
    leader_id       BIGINT       NOT NULL REFERENCES users(id),
    name            VARCHAR(100) NOT NULL,
    category        VARCHAR(16)  NOT NULL,
    summary         VARCHAR(200) NOT NULL,
    description     TEXT,
    activity_method TEXT,
    image_url       VARCHAR(500),
    kakao_link      VARCHAR(500),
    capacity        INT,
    start_date      DATE,
    end_date        DATE,
    recruit_status  VARCHAR(16)  NOT NULL DEFAULT 'RECRUITING',
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_club_term ON club(term_id);

-- 탈퇴·강퇴·거절해도 행을 지우지 않는다. 지난 회차 명단이 이 행의 joined_at/left_at 으로 계산된다.
-- 재신청은 새 행을 만든다. 그래서 중복 방지는 진행 중(PENDING·ACTIVE)인 행에만 건다.
CREATE TABLE IF NOT EXISTS club_member (
    id            BIGSERIAL    PRIMARY KEY,
    club_id       BIGINT       NOT NULL REFERENCES club(id),
    user_id       BIGINT       NOT NULL REFERENCES users(id),
    status        VARCHAR(16)  NOT NULL,
    apply_message VARCHAR(500),
    applied_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    joined_at     TIMESTAMPTZ,
    left_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_club_member_open
    ON club_member(club_id, user_id) WHERE status IN ('PENDING', 'ACTIVE');
CREATE INDEX IF NOT EXISTS idx_club_member_user ON club_member(user_id);

CREATE TABLE IF NOT EXISTS club_leader_grant (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    granted_by BIGINT,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_club_leader_grant_active
    ON club_leader_grant(user_id) WHERE revoked_at IS NULL;

CREATE TABLE IF NOT EXISTS club_open_request (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id),
    name          VARCHAR(100) NOT NULL,
    category      VARCHAR(16)  NOT NULL,
    summary       VARCHAR(200) NOT NULL,
    goal          TEXT,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500),
    reviewed_by   BIGINT,
    reviewed_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_schedule (
    id          BIGSERIAL    PRIMARY KEY,
    club_id     BIGINT       NOT NULL REFERENCES club(id),
    title       VARCHAR(200) NOT NULL,
    starts_at   TIMESTAMPTZ  NOT NULL,
    location    VARCHAR(200),
    online_link VARCHAR(500),
    description TEXT,
    created_by  BIGINT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_club_schedule_club ON club_schedule(club_id, starts_at);

CREATE TABLE IF NOT EXISTS club_schedule_rsvp (
    id          BIGSERIAL   PRIMARY KEY,
    schedule_id BIGINT      NOT NULL REFERENCES club_schedule(id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    response    VARCHAR(16) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_club_schedule_rsvp UNIQUE (schedule_id, user_id)
);

-- QR 출석. 활동 기록 작성 화면에서 출석 체크의 기본값으로만 쓴다.
CREATE TABLE IF NOT EXISTS club_schedule_checkin (
    id          BIGSERIAL   PRIMARY KEY,
    schedule_id BIGINT      NOT NULL REFERENCES club_schedule(id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    checked_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_club_schedule_checkin UNIQUE (schedule_id, user_id)
);

-- 활동 기록 = 활동 인증. 일정을 지우면 연결만 끊는다.
CREATE TABLE IF NOT EXISTS club_activity (
    id              BIGSERIAL     PRIMARY KEY,
    club_id         BIGINT        NOT NULL REFERENCES club(id),
    schedule_id     BIGINT        UNIQUE REFERENCES club_schedule(id) ON DELETE SET NULL,
    activity_date   DATE          NOT NULL,
    content         TEXT          NOT NULL,
    progress_note   TEXT,
    status          VARCHAR(24)   NOT NULL DEFAULT 'PENDING',
    revision_reason VARCHAR(1000),
    reviewed_by     BIGINT,
    reviewed_at     TIMESTAMPTZ,
    submitted_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_club_activity_club ON club_activity(club_id, activity_date);
CREATE INDEX IF NOT EXISTS idx_club_activity_status ON club_activity(status);

CREATE TABLE IF NOT EXISTS club_activity_photo (
    id          BIGSERIAL    PRIMARY KEY,
    activity_id BIGINT       NOT NULL REFERENCES club_activity(id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 행 = 활동일 당시 팀 명단. 행 수가 참석 비율의 분모다.
CREATE TABLE IF NOT EXISTS club_activity_attendance (
    id          BIGSERIAL   PRIMARY KEY,
    activity_id BIGINT      NOT NULL REFERENCES club_activity(id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    attended    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_club_activity_attendance UNIQUE (activity_id, user_id)
);

CREATE TABLE IF NOT EXISTS club_attendance_fix_request (
    id          BIGSERIAL     PRIMARY KEY,
    activity_id BIGINT        NOT NULL REFERENCES club_activity(id) ON DELETE CASCADE,
    user_id     BIGINT        NOT NULL REFERENCES users(id),
    reason      VARCHAR(1000),
    status      VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    handled_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_post (
    id         BIGSERIAL   PRIMARY KEY,
    club_id    BIGINT      NOT NULL REFERENCES club(id),
    author_id  BIGINT      NOT NULL REFERENCES users(id),
    category   VARCHAR(16) NOT NULL,
    content    TEXT        NOT NULL,
    image_urls JSONB,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_club_post_club ON club_post(club_id, created_at);

-- 목표·완주. 소모임당 한 행이며 처음 쓸 때 만든다.
CREATE TABLE IF NOT EXISTS club_completion (
    id                 BIGSERIAL     PRIMARY KEY,
    club_id            BIGINT        NOT NULL UNIQUE REFERENCES club(id),
    goal               TEXT,
    goal_criteria      TEXT,
    goal_result        TEXT,
    goal_evidence_urls JSONB,
    goal_status        VARCHAR(16)   NOT NULL DEFAULT 'NOT_SUBMITTED',
    completion_status  VARCHAR(16)   NOT NULL DEFAULT 'IN_PROGRESS',
    completion_memo    VARCHAR(1000),
    confirmed_by       BIGINT,
    confirmed_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 쉬는 주. week_start 는 그 주의 월요일.
CREATE TABLE IF NOT EXISTS club_rest_week (
    id         BIGSERIAL   PRIMARY KEY,
    club_id    BIGINT      NOT NULL REFERENCES club(id),
    week_start DATE        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_club_rest_week UNIQUE (club_id, week_start)
);

-- 좋아요·댓글. 대상은 일반 게시글(POST) 또는 활동 기록(ACTIVITY).
CREATE TABLE IF NOT EXISTS club_like (
    id          BIGSERIAL   PRIMARY KEY,
    target_type VARCHAR(16) NOT NULL,
    target_id   BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_club_like UNIQUE (target_type, target_id, user_id)
);

CREATE TABLE IF NOT EXISTS club_comment (
    id          BIGSERIAL   PRIMARY KEY,
    target_type VARCHAR(16) NOT NULL,
    target_id   BIGINT      NOT NULL,
    author_id   BIGINT      NOT NULL REFERENCES users(id),
    content     TEXT        NOT NULL,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_club_comment_target ON club_comment(target_type, target_id);
