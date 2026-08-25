-- 모집 안내 일정. 지원 창구를 여닫는 open_at/close_at 과 달리 **화면에 보여주는 값**이다.
--
-- 서류 발표·면접·최종 발표 날짜가 웹 상수(recruitSchedule.ts)에만 있어서 바꾸려면 배포가
-- 필요했다. 랜딩과 지원 안내 두 화면이 같이 쓰는 값이라 랜딩 콘텐츠 문서가 아니라
-- 모집 쪽에 둔다.
--
-- 전부 NULL 을 허용한다. NULL 이면 웹이 번들에 든 기본값을 그대로 쓴다 — 그래서 이
-- 마이그레이션만 올라가고 아무도 저장하지 않아도 지금 화면이 그대로 유지된다.
--
-- 종류마다 쓰는 칸이 다르다. CORE 는 서류·면접·최종을, MEMBER 는 집중 모집 기간만 쓴다.
-- 한쪽만 쓰는 칸을 이름 없이 공유하면 나중에 어느 쪽 값인지 알 수 없어 따로 둔다.
ALTER TABLE recruit_period_override
    ADD COLUMN IF NOT EXISTS document_result_at TIMESTAMPTZ,   -- CORE: 서류 발표
    ADD COLUMN IF NOT EXISTS interview_open_at  TIMESTAMPTZ,   -- CORE: 면접 시작
    ADD COLUMN IF NOT EXISTS interview_close_at TIMESTAMPTZ,   -- CORE: 면접 마감
    ADD COLUMN IF NOT EXISTS final_result_at    TIMESTAMPTZ,   -- CORE: 최종 발표
    ADD COLUMN IF NOT EXISTS interview_note     VARCHAR(300),  -- CORE: 면접 안내 문구
    ADD COLUMN IF NOT EXISTS meeting_note       VARCHAR(300),  -- CORE: 상견례 안내 문구
    ADD COLUMN IF NOT EXISTS intensive_open_at  TIMESTAMPTZ,   -- MEMBER: 집중 모집 시작
    ADD COLUMN IF NOT EXISTS intensive_close_at TIMESTAMPTZ;   -- MEMBER: 집중 모집 마감

-- 기간을 거꾸로 저장하는 사고를 막는다. 한쪽만 채운 상태는 허용한다 —
-- 시작만 정해두고 마감을 나중에 정하는 경우가 있다.
ALTER TABLE recruit_period_override
    ADD CONSTRAINT chk_recruit_interview_order
        CHECK (interview_open_at IS NULL OR interview_close_at IS NULL
               OR interview_open_at < interview_close_at),
    ADD CONSTRAINT chk_recruit_intensive_order
        CHECK (intensive_open_at IS NULL OR intensive_close_at IS NULL
               OR intensive_open_at < intensive_close_at);
