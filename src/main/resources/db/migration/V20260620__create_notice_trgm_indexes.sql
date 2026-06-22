CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_title_trgm ON notice_board USING GIN (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_content_trgm ON notice_board USING GIN (content gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_title_content_trgm ON notice_board USING GIN ((title || ' ' || content) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_articles_posted_by_name ON notice_board USING GIN (posted_by_name gin_trgm_ops);
