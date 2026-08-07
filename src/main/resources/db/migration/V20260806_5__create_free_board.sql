CREATE TABLE IF NOT EXISTS free_board (
    id           BIGSERIAL     PRIMARY KEY,
    title        VARCHAR(255)  NOT NULL,
    content      TEXT          NOT NULL,
    author_id    BIGINT        NOT NULL REFERENCES users(id),
    author_name  VARCHAR(100)  NOT NULL,
    view_count   INT           NOT NULL DEFAULT 0,
    deleted_at   TIMESTAMPTZ            DEFAULT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_free_board_author_id  ON free_board(author_id);
CREATE INDEX IF NOT EXISTS idx_free_board_deleted_at ON free_board(deleted_at) WHERE deleted_at IS NOT NULL;
