CREATE TABLE IF NOT EXISTS recruit_member_memo (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    privacy_agreement BOOLEAN NOT NULL,
    freshman_memo_agreement BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recruit_member_memo_phone_number ON recruit_member_memo(phone_number);
CREATE INDEX IF NOT EXISTS idx_recruit_member_memo_email_lower ON recruit_member_memo((lower(email)));
CREATE INDEX IF NOT EXISTS idx_recruit_member_memo_created_at ON recruit_member_memo(created_at DESC);
