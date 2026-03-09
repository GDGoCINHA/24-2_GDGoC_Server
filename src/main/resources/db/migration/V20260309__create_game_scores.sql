CREATE TABLE IF NOT EXISTS game_scores (
    id            BIGSERIAL    PRIMARY KEY,
    phone_number  VARCHAR(20)  NOT NULL UNIQUE,
    nickname      VARCHAR(20)  NOT NULL,
    score         INT          NOT NULL DEFAULT 0,
    stage_reached INT          NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_game_scores_score ON game_scores(score DESC);
