CREATE TABLE IF NOT EXISTS free_board_attachment (
    id             BIGSERIAL     PRIMARY KEY,
    free_board_id  BIGINT        NOT NULL REFERENCES free_board(id) ON DELETE CASCADE,
    kind           VARCHAR(16)   NOT NULL,
    file_key       VARCHAR(512),
    file_name      VARCHAR(255),
    file_size      BIGINT,
    url            VARCHAR(2048),
    sort_order     INT           NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_free_board_attachment_kind CHECK (
        (kind = 'FILE' AND file_key IS NOT NULL AND file_name IS NOT NULL AND url IS NULL)
     OR (kind = 'LINK' AND url IS NOT NULL AND file_key IS NULL AND file_name IS NULL AND file_size IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_free_board_attachment_board_id
    ON free_board_attachment(free_board_id);
