-- 모집 기간을 관리자 화면에서 바꾸기 위한 표.
--
-- 지금까지 기간은 설정(app.recruit.*.open-at)에만 있었다. 바꾸려면 배포가 필요했고,
-- 마감일이 지난 줄 모르고 5개월간 API 가 막혀 있던 적이 있다.
--
-- 행이 없으면 설정값을 그대로 쓴다. 그래서 이 마이그레이션만 올라가고 아무도
-- 저장하지 않아도 지금 동작이 그대로 유지된다.
CREATE TABLE IF NOT EXISTS recruit_period_override (
    recruit_type VARCHAR(16)  PRIMARY KEY,
    open_at      TIMESTAMPTZ  NOT NULL,
    close_at     TIMESTAMPTZ  NOT NULL,
    updated_by   BIGINT       NOT NULL REFERENCES users(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_recruit_period_override_order CHECK (open_at < close_at)
);
