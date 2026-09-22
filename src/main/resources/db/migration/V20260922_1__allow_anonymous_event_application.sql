-- 행사 신청을 로그인 없이도 받는다.
--
-- 링크를 타고 들어온 사람이 가입·로그인 없이 바로 신청하게 한다. 폼마다 켠다 —
-- 부원 전용 행사는 그대로 막을 수 있어야 한다.
--
-- 기존 행의 값은 바꾸지 않는다. 이미 들어와 있는 신청은 전부 user_id 가 있고,
-- 여기서는 NOT NULL 을 풀고 빈 컬럼을 더할 뿐이다.

ALTER TABLE event_application_form
    ADD COLUMN IF NOT EXISTS allow_anonymous BOOLEAN NOT NULL DEFAULT FALSE;

-- 계정이 없는 신청은 신원을 이 행에 직접 적는다. 계정 신청은 네 칸이 비어 있고 users 에서 읽는다.
ALTER TABLE event_application ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE event_application
    ADD COLUMN IF NOT EXISTS applicant_name       VARCHAR(50),
    ADD COLUMN IF NOT EXISTS applicant_student_id VARCHAR(20),
    ADD COLUMN IF NOT EXISTS applicant_major      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS applicant_phone      VARCHAR(20);

-- 계정도 신원도 없는 신청은 누구인지 알 수 없다. 기존 행은 전부 user_id 가 있어 통과한다.
ALTER TABLE event_application ADD CONSTRAINT chk_event_application_identity
    CHECK (user_id IS NOT NULL
        OR (applicant_name IS NOT NULL
            AND applicant_student_id IS NOT NULL
            AND applicant_major IS NOT NULL
            AND applicant_phone IS NOT NULL));

-- UNIQUE(form_id, user_id) 는 NULL 끼리 겹치지 않아 로그인 없이 낸 중복 신청을 못 막는다.
-- 학번으로 막는다. 계정 신청과의 중복은 애플리케이션이 폼 행을 잠근 채 확인한다.
CREATE UNIQUE INDEX IF NOT EXISTS uq_event_application_form_anonymous_student
    ON event_application (form_id, applicant_student_id)
    WHERE user_id IS NULL;
