-- 2026 홈커밍데이 OB 참석 신청.
--
-- OB 는 대부분 홈페이지 계정이 없어 카카오톡 링크로 로그인 없이 신청한다. 그래서 신원(이름·전화번호)을
-- 이 행에 직접 적고, 중복 판정도 전화번호로 한다. 행사 신청 폼 빌더(event_application)를 쓰지 않는 이유는
-- 참석 유형별 단계 분기·명찰 내보내기·목적별 장기 동의처럼 이 행사에만 있는 규칙이 많아서다.
--
-- 신청자 본인 수정·취소는 없다. 회장·HR 이 관리 API 로 고치거나 취소한다. 취소는 행을 지우지 않고
-- status 만 바꾼다 — 집계·명찰에서 빠지면서도 이력은 남아야 한다.

CREATE TABLE IF NOT EXISTS homecoming_application (
    id               BIGSERIAL     PRIMARY KEY,
    -- 클라이언트가 신청 시트를 열 때 만든 UUID. 저장 후 응답이 유실돼 재시도해도 같은 행을 돌려준다.
    request_id       VARCHAR(64)   NOT NULL,
    attend_type      VARCHAR(10)   NOT NULL,
    name             VARCHAR(50)   NOT NULL,
    phone            VARCHAR(13)   NOT NULL,
    -- 불참(NO)은 활동 정보를 받지 않는다.
    year_start       INT,
    year_end         INT,
    member_role      VARCHAR(16),
    -- 운영진(ORGANIZER)일 때만 있다. 사용자가 고른 값 그대로이며 과거 팀명으로 바꾸지 않는다.
    team             VARCHAR(16),
    -- 본 행사(MAIN·BOTH)만 늦참·요청사항을 받는다.
    is_late          BOOLEAN       NOT NULL DEFAULT FALSE,
    arrival_time     VARCHAR(5),
    message          VARCHAR(1000),
    privacy_agreed   BOOLEAN       NOT NULL,
    status           VARCHAR(16)   NOT NULL,
    submitted_at     TIMESTAMPTZ   NOT NULL,
    canceled_at      TIMESTAMPTZ,
    cancel_reason    VARCHAR(200),
    -- 마지막으로 고치거나 취소한 관리자 users.id. 신청자가 낸 그대로면 비어 있다.
    last_modified_by BIGINT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_homecoming_application_request UNIQUE (request_id),
    CONSTRAINT chk_homecoming_year_order
        CHECK (year_start IS NULL OR year_end IS NULL OR year_end >= year_start),
    CONSTRAINT chk_homecoming_late_arrival
        CHECK (is_late = FALSE OR arrival_time IS NOT NULL),
    CONSTRAINT chk_homecoming_privacy CHECK (privacy_agreed = TRUE)
);

-- 같은 번호로 두 번 신청하지 못하게 한다. 취소된 신청은 빼야 운영진이 취소한 사람이 다시 낼 수 있다.
-- 애플리케이션도 먼저 확인하지만, 동시에 들어온 두 요청은 이 인덱스만 막는다.
CREATE UNIQUE INDEX IF NOT EXISTS uq_homecoming_application_active_phone
    ON homecoming_application (phone)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_homecoming_application_submitted
    ON homecoming_application (submitted_at);

-- 목적별 동의 기록. 신청 한 건마다 세 목적(PRIVACY·EVENT_INVITE·TALENT_POOL) 행이 생기고, 거부도 agreed=false 로 남긴다.
-- 선택 동의는 신청과 독립이다 — 신청을 취소해도 동의는 남고, 동의만 철회할 수도 있다.
-- 장기 이용(EVENT_INVITE·TALENT_POOL)은 이름·전화번호·활동 기간·활동 구분만 쓴다. 팀·늦참·요청사항은 쓰지 않는다.
CREATE TABLE IF NOT EXISTS homecoming_consent (
    id             BIGSERIAL    PRIMARY KEY,
    application_id BIGINT       NOT NULL REFERENCES homecoming_application(id),
    purpose        VARCHAR(20)  NOT NULL,
    agreed         BOOLEAN      NOT NULL,
    notice_version VARCHAR(40)  NOT NULL,
    decided_at     TIMESTAMPTZ  NOT NULL,
    -- 선택 동의는 2027-12-31 KST 까지. 필수(PRIVACY) 동의의 파기 기한은 운영진 확정 전이라 비어 있다.
    expires_at     TIMESTAMPTZ,
    withdrawn_at   TIMESTAMPTZ,
    withdrawn_by   BIGINT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_homecoming_consent_purpose UNIQUE (application_id, purpose)
);
