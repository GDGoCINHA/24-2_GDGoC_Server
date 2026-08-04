CREATE TABLE IF NOT EXISTS event_board (
    id                BIGSERIAL    PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    event_start_date  DATE         NOT NULL,
    event_end_date    DATE         NOT NULL,
    organizing_team   VARCHAR(32)  NOT NULL,
    thumbnail_key     VARCHAR(512),
    content           TEXT         NOT NULL,
    is_published      BOOLEAN      NOT NULL DEFAULT FALSE,
    author_id         BIGINT       NOT NULL REFERENCES users(id),
    author_name       VARCHAR(100) NOT NULL DEFAULT '',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS event_board_attachment (
    id               BIGSERIAL    PRIMARY KEY,
    event_board_id   BIGINT       NOT NULL REFERENCES event_board(id) ON DELETE CASCADE,
    file_key         VARCHAR(512) NOT NULL,
    file_name        VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_event_board_is_published        ON event_board(is_published);
CREATE INDEX IF NOT EXISTS idx_event_board_author_id           ON event_board(author_id);
CREATE INDEX IF NOT EXISTS idx_event_board_attachment_board_id ON event_board_attachment(event_board_id);
CREATE INDEX IF NOT EXISTS idx_event_board_deleted_at ON event_board(deleted_at) WHERE deleted_at IS NOT NULL;
