CREATE TABLE IF NOT EXISTS pinned_notice (
    id               BIGSERIAL   PRIMARY KEY,
    notice_board_id  BIGINT      NOT NULL UNIQUE REFERENCES notice_board(id) ON DELETE CASCADE,
    display_order    INT         NOT NULL UNIQUE,
    pinned_by        BIGINT      NOT NULL REFERENCES users(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pinned_notice_order CHECK (display_order BETWEEN 1 AND 3)
);
