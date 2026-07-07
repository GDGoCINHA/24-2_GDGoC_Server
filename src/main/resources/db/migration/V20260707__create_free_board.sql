CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS free_board (
    id             BIGSERIAL    PRIMARY KEY,
    post_number    BIGSERIAL    UNIQUE,
    title          VARCHAR(255) NOT NULL,
    content        TEXT         NOT NULL,
    status         VARCHAR(32)  NOT NULL DEFAULT 'PUBLISHED',
    is_public      BOOLEAN      NOT NULL DEFAULT true,

    view_count     INT          NOT NULL DEFAULT 0,

    posted_by      BIGINT       NOT NULL REFERENCES users(id),
    posted_by_name VARCHAR(100) NOT NULL,

    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS free_board_attachment (
    id              BIGSERIAL    PRIMARY KEY,
    free_board_id   BIGINT       NOT NULL REFERENCES free_board(id) ON DELETE CASCADE,
    attachment_type VARCHAR(32)  NOT NULL,
    file_category   VARCHAR(20),
    original_name   VARCHAR(255),
    stored_name     VARCHAR(255),
    file_url        TEXT,
    file_size       BIGINT,
    mime_type       VARCHAR(127),
    link_url        TEXT,
    display_order   INT          NOT NULL DEFAULT 0,
    uploaded_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS free_board_like (
    free_board_id BIGINT      NOT NULL REFERENCES free_board(id) ON DELETE CASCADE,
    user_id       BIGINT      NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (free_board_id, user_id)
);

CREATE TABLE IF NOT EXISTS free_board_comment (
    id             BIGSERIAL     PRIMARY KEY,
    free_board_id  BIGINT        NOT NULL REFERENCES free_board(id) ON DELETE CASCADE,
    posted_by      BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    posted_by_name VARCHAR(100)  NOT NULL,
    content        VARCHAR(1000) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_free_board_number     ON free_board(post_number);
CREATE INDEX IF NOT EXISTS idx_free_board_status     ON free_board(status)     WHERE status = 'PUBLISHED';
CREATE INDEX IF NOT EXISTS idx_free_board_deleted    ON free_board(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_free_board_public     ON free_board(is_public)  WHERE is_public = true;
CREATE INDEX IF NOT EXISTS idx_free_board_created_at ON free_board(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_free_board_title_trgm   ON free_board USING GIN (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_free_board_content_trgm ON free_board USING GIN (content gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_free_board_author_trgm  ON free_board USING GIN (posted_by_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_free_board_attachment_post ON free_board_attachment(free_board_id);
CREATE INDEX IF NOT EXISTS idx_free_board_comment_post    ON free_board_comment(free_board_id) WHERE deleted_at IS NULL;
