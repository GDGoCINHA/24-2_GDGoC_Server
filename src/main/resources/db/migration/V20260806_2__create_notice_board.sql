CREATE TABLE IF NOT EXISTS notice_board (
    id           BIGSERIAL     PRIMARY KEY,
    category     VARCHAR(32)   NOT NULL,
    title        VARCHAR(255)  NOT NULL,
    content      TEXT          NOT NULL,
    author_id    BIGINT        NOT NULL REFERENCES users(id),
    author_name  VARCHAR(100)  NOT NULL,
    view_count   INT           NOT NULL DEFAULT 0,
    is_published BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted_at   TIMESTAMPTZ            DEFAULT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notice_board_is_published ON notice_board(is_published);
CREATE INDEX IF NOT EXISTS idx_notice_board_author_id    ON notice_board(author_id);
CREATE INDEX IF NOT EXISTS idx_notice_board_deleted_at   ON notice_board(deleted_at) WHERE deleted_at IS NOT NULL;
