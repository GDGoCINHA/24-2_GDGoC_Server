CREATE TABLE IF NOT EXISTS free_board_comment (
    id             BIGSERIAL     PRIMARY KEY,
    free_board_id  BIGINT        NOT NULL REFERENCES free_board(id) ON DELETE CASCADE,
    parent_id      BIGINT                 REFERENCES free_board_comment(id) ON DELETE CASCADE,
    content        TEXT          NOT NULL,
    author_id      BIGINT        NOT NULL REFERENCES users(id),
    author_name    VARCHAR(100)  NOT NULL,
    deleted_at     TIMESTAMPTZ            DEFAULT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 상세 화면이 글 하나의 댓글을 통째로 읽고 작성순으로 세운다.
CREATE INDEX IF NOT EXISTS idx_free_board_comment_board_id
    ON free_board_comment(free_board_id, created_at);

CREATE INDEX IF NOT EXISTS idx_free_board_comment_parent_id
    ON free_board_comment(parent_id) WHERE parent_id IS NOT NULL;
