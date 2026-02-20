CREATE TABLE IF NOT EXISTS recruit_member_memo_notification (
    id BIGSERIAL PRIMARY KEY,
    semester VARCHAR(16) NOT NULL,
    email VARCHAR(255) NOT NULL,
    subject VARCHAR(200),
    body TEXT,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_recruit_member_memo_notification_semester_email
    ON recruit_member_memo_notification (semester, email);

CREATE INDEX IF NOT EXISTS idx_recruit_member_memo_notification_status_next_attempt
    ON recruit_member_memo_notification (status, next_attempt_at);

ALTER TABLE recruit_member_memo_notification
    ADD COLUMN IF NOT EXISTS subject VARCHAR(200),
    ADD COLUMN IF NOT EXISTS body TEXT;

UPDATE recruit_member_memo_notification
SET
    subject = COALESCE(subject, '[GDGoC INHA] 2026학년도 신입생 정식 지원 안내'),
    body = COALESCE(
        body,
        $$안녕하세요, GDGoC INHA입니다.

먼저 인하대학교 입학을 진심으로 축하드립니다!
그동안 학번이 나오지 않아 지원을 기다려 주셨던 신입생분들께 반가운 소식을 전해드립니다.

어제부로 신입생 학번이 발급됨에 따라, 이제 정식으로 GDGoC INHA 지원이 가능해졌습니다. 알림 신청을 통해 보여주신 여러분의 열정을 지원서에 가득 담아주세요!

지원 링크: https://gdgocinha.com/recruit/member

기술을 통해 함께 성장하고, 더 나은 가치를 만들어갈 여러분의 지원을 기다리겠습니다.

여러분과 함께 성장해나가는 커뮤니티, GDGoC INHA 운영진 드림
$$
    )
WHERE subject IS NULL
   OR body IS NULL;

ALTER TABLE recruit_member_memo_notification
    ALTER COLUMN subject SET NOT NULL,
    ALTER COLUMN body SET NOT NULL;
