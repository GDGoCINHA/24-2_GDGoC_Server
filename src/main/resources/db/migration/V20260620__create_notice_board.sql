CREATE TABLE IF NOT EXISTS notice_board (
    article_id       UUID            NOT NULL DEFAULT gen_random_uuid(),
    article_number   BIGSERIAL       NOT NULL,
    category         VARCHAR(32)              DEFAULT NULL,
    is_pinned        BOOLEAN         NOT NULL DEFAULT FALSE,
    status           VARCHAR(32)     NOT NULL DEFAULT 'PUBLISHED',
    title            VARCHAR(255)    NOT NULL,
    content          TEXT            NOT NULL,
    view_count       INTEGER         NOT NULL DEFAULT 0,
    posted_by        BIGINT          NOT NULL REFERENCES users(id),
    posted_by_name   VARCHAR(100)    NOT NULL,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMPTZ              DEFAULT NULL,

    CONSTRAINT pk_notice_board PRIMARY KEY (article_id)
);

CREATE TABLE IF NOT EXISTS notice_board_attachment (
    attachment_id    UUID            NOT NULL DEFAULT gen_random_uuid(),
    article_id       UUID            NOT NULL REFERENCES notice_board(article_id) ON DELETE CASCADE,
    attachment_type  VARCHAR(32)     NOT NULL,
    original_name    VARCHAR(255)             DEFAULT NULL,  -- FILE 전용
    stored_name      VARCHAR(255)             DEFAULT NULL,  -- FILE 전용 (S3 UUID 기반)
    file_url         TEXT                     DEFAULT NULL,  -- FILE 전용 (S3 접근 경로)
    file_size        BIGINT                   DEFAULT NULL,  -- FILE 전용 (바이트 단위)
    mime_type        VARCHAR(100)             DEFAULT NULL,  -- FILE 전용
    link_url         TEXT                     DEFAULT NULL,  -- URL 전용
    display_order    INTEGER         NOT NULL,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_notice_board_attachment PRIMARY KEY (attachment_id)
);

CREATE INDEX IF NOT EXISTS idx_articles_number ON notice_board (article_number);
CREATE INDEX IF NOT EXISTS idx_category ON notice_board (category) WHERE category IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_articles_deleted ON notice_board (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_articles_pinned ON notice_board (is_pinned) WHERE is_pinned = TRUE;
CREATE INDEX IF NOT EXISTS idx_articles_status ON notice_board (status) WHERE status = 'PUBLISHED';
CREATE INDEX IF NOT EXISTS idx_articles_prev_next ON notice_board (category, status, article_number) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_notice_board_attachment_article_id ON notice_board_attachment (article_id);
