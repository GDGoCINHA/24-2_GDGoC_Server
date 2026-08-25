-- 온보딩 콘텐츠. 관리자 화면에서 고치고 발행한다.
--
-- 문서 하나를 통째로 저장한다. 이 페이지는 항상 전체가 함께 편집·발행되고,
-- 조회도 전체를 한 번에 하므로 표를 쪼갤 이유가 없다.
--
-- 행이 없으면 웹이 번들에 든 기본값을 그대로 보여준다. 그래서 이 표가 비어 있어도
-- 지금 화면이 그대로 유지된다.
CREATE TABLE IF NOT EXISTS landing_content (
    id         BIGSERIAL    PRIMARY KEY,
    status     VARCHAR(16)  NOT NULL UNIQUE,
    content    TEXT         NOT NULL,
    updated_by BIGINT       NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
