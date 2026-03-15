CREATE TABLE IF NOT EXISTS bingo_board (
    id          BIGSERIAL    PRIMARY KEY,
    team_number INT          NOT NULL UNIQUE,
    marks       VARCHAR(160) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bingo_interaction (
    id          BIGSERIAL    PRIMARY KEY,
    team_number INT          NOT NULL,
    cell_index  INT          NOT NULL,
    mark        VARCHAR(16)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bingo_interaction_team_number
    ON bingo_interaction (team_number);

CREATE INDEX IF NOT EXISTS idx_bingo_interaction_team_created_at
    ON bingo_interaction (team_number, created_at DESC);
