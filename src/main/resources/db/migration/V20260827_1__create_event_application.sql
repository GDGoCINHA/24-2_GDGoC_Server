-- 행사 신청 폼 빌더.
--
-- 행사마다 구글폼을 새로 만드는 대신 운영진이 홈페이지에서 신청 폼을 만든다.
-- 행사가 열릴 때마다 컬럼을 추가하지 않도록 질문·신청·답변을 데이터로 저장한다.
--
-- 행사 자체는 event_board 를 그대로 쓴다. 다만 FK 로 묶지 않고 id 만 들고,
-- 표시에 필요한 값(행사명·기간)은 이 표에 복사해 둔다. 게시글이 휴지통에 들어가도
-- 신청 데이터와 마이페이지 이력이 그대로 살아야 하기 때문이다.
-- 복사본은 게시글을 수정할 때 함께 갱신한다.

CREATE TABLE IF NOT EXISTS event_application_form (
    id               BIGSERIAL    PRIMARY KEY,
    event_board_id   BIGINT       NOT NULL UNIQUE,
    event_title      VARCHAR(255) NOT NULL,
    event_start_date DATE         NOT NULL,
    event_end_date   DATE         NOT NULL,
    opens_at         TIMESTAMPTZ,
    closes_at        TIMESTAMPTZ,
    capacity         INT,
    min_role         VARCHAR(16)  NOT NULL DEFAULT 'MEMBER',
    is_open          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_event_form_period   CHECK (opens_at IS NULL OR closes_at IS NULL OR opens_at < closes_at),
    CONSTRAINT chk_event_form_capacity CHECK (capacity IS NULL OR capacity > 0)
);

-- visible_when_* 는 조건부 표시다. 기준 질문의 답이 visible_when_values 중 하나일 때만 보인다.
-- 기준 질문은 반드시 자기보다 sort_order 가 작아야 한다 — 이 규칙 하나로 순환 참조가
-- 구조적으로 불가능해진다. 순서 검사는 애플리케이션이 한다.
CREATE TABLE IF NOT EXISTS event_form_question (
    id                       BIGSERIAL    PRIMARY KEY,
    form_id                  BIGINT       NOT NULL REFERENCES event_application_form(id) ON DELETE CASCADE,
    type                     VARCHAR(24)  NOT NULL,
    label                    VARCHAR(255) NOT NULL,
    help_text                VARCHAR(500),
    is_required              BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order               INT          NOT NULL,
    options                  JSONB,
    visible_when_question_id BIGINT       REFERENCES event_form_question(id),
    visible_when_values      JSONB,
    is_deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_event_question_condition
        CHECK ((visible_when_question_id IS NULL) = (visible_when_values IS NULL)),
    CONSTRAINT chk_event_question_not_self
        CHECK (visible_when_question_id IS NULL OR visible_when_question_id <> id)
);

-- 취소는 행을 지우지 않고 status 만 바꾼다. UNIQUE(form_id, user_id) 로 중복 신청을 막는데,
-- 취소를 삭제로 처리하면 재신청 때 이 제약과 충돌하기 때문이다. 재신청은 같은 행을 되살린다.
CREATE TABLE IF NOT EXISTS event_application (
    id                BIGSERIAL    PRIMARY KEY,
    form_id           BIGINT       NOT NULL REFERENCES event_application_form(id) ON DELETE CASCADE,
    user_id           BIGINT       NOT NULL REFERENCES users(id),
    status            VARCHAR(16)  NOT NULL DEFAULT 'APPLIED',
    attendance_status VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    checked_in_at     TIMESTAMPTZ,
    applied_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_event_application_form_user UNIQUE (form_id, user_id)
);

CREATE TABLE IF NOT EXISTS event_application_answer (
    id             BIGSERIAL   PRIMARY KEY,
    application_id BIGINT      NOT NULL REFERENCES event_application(id) ON DELETE CASCADE,
    question_id    BIGINT      NOT NULL REFERENCES event_form_question(id),
    value          JSONB       NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_event_answer_application_question UNIQUE (application_id, question_id)
);

CREATE INDEX IF NOT EXISTS idx_event_question_form_order   ON event_form_question(form_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_event_question_visible_when ON event_form_question(visible_when_question_id)
    WHERE visible_when_question_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_event_application_form      ON event_application(form_id, status);
CREATE INDEX IF NOT EXISTS idx_event_application_user      ON event_application(user_id);
CREATE INDEX IF NOT EXISTS idx_event_answer_application    ON event_application_answer(application_id);
