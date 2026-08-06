ALTER TABLE event_board_attachment
    ADD COLUMN kind       VARCHAR(16)  NOT NULL DEFAULT 'FILE',
    ADD COLUMN file_size  BIGINT,
    ADD COLUMN url        VARCHAR(2048),
    ADD COLUMN sort_order INT          NOT NULL DEFAULT 0;

ALTER TABLE event_board_attachment
    ALTER COLUMN file_key  DROP NOT NULL,
    ALTER COLUMN file_name DROP NOT NULL;

ALTER TABLE event_board_attachment
    ADD CONSTRAINT ck_event_board_attachment_kind CHECK (
        (kind = 'FILE' AND file_key IS NOT NULL AND file_name IS NOT NULL AND url IS NULL)
     OR (kind = 'LINK' AND url IS NOT NULL AND file_key IS NULL)
    );
