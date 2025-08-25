DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN
    WITH target_cols AS (
      SELECT
          n.nspname  AS schema_name,
          c.relname  AS table_name,
          a.attname  AS column_name
      FROM pg_catalog.pg_attribute a
      JOIN pg_catalog.pg_class     c ON c.oid = a.attrelid
      JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
      WHERE a.attnum > 0
        AND NOT a.attisdropped
        AND c.relkind IN ('r','p')
        AND n.nspname NOT IN ('pg_catalog','information_schema')
        AND a.attname IN ('created_at','updated_at')
        AND pg_catalog.format_type(a.atttypid, a.atttypmod) ILIKE 'timestamp% without time zone%'
    )
    SELECT
      format(
        'ALTER TABLE %I.%I ALTER COLUMN %I TYPE timestamptz(6) USING (%I AT TIME ZONE %L);',
         schema_name, table_name, column_name, column_name, 'Asia/Seoul'
      ) AS alter_sql
    FROM target_cols
    ORDER BY schema_name, table_name, column_name
  LOOP
    RAISE NOTICE 'Executing: %', r.alter_sql;
    EXECUTE r.alter_sql;
  END LOOP;
END $$;
