-- 1) 컬럼 추가 (처음엔 NULL 허용)
ALTER TABLE recruit_member
    ADD COLUMN admission_semester VARCHAR(10);

-- 2) 기존 데이터 백필
-- 학기 규칙: 2~7월 = YYY_1, 8~12월 = YYY_2, 1월 = (전년도) YYY_2
UPDATE recruit_member
SET admission_semester = CASE
                             WHEN EXTRACT(MONTH FROM created_at) BETWEEN 8 AND 12
                                 THEN 'Y' || to_char(created_at, 'YY') || '_2'
                             WHEN EXTRACT(MONTH FROM created_at) BETWEEN 2 AND 7
                                 THEN 'Y' || to_char(created_at, 'YY') || '_1'
                             WHEN EXTRACT(MONTH FROM created_at) = 1
                                 THEN 'Y' || to_char(created_at - INTERVAL '1 year', 'YY') || '_2'
    END
WHERE admission_semester IS NULL;

-- 3) NOT NULL 전환 (형식 제약은 생략 가능)
ALTER TABLE recruit_member
    ALTER COLUMN admission_semester SET NOT NULL;

-- (선택) 인덱스
CREATE INDEX idx_recruit_member_admission_semester
    ON recruit_member (admission_semester);
