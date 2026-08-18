-- 부원 지원을 학기마다 다시 받을 수 있게 한다.
--
-- recruit_member 는 생성 때부터 student_id·phone_number 에 전역 UNIQUE 를 달고 있었다.
-- 그래서 한 사람은 평생 한 번만 지원할 수 있었다 — 학기가 바뀌어 다시 지원하면
-- `duplicate key value violates unique constraint "recruit_member_student_id_key"` 로
-- 500 이 났다(2026-08-18 dev 실측). 매 학기 회비를 받는 운영과 어긋난다.
--
-- 제약을 (값, 학기) 복합으로 바꾼다. 같은 학기 안의 중복은 그대로 막히고 학기가 다르면 통과한다.
--
-- 제약 이름은 database_schema.sql 이 컬럼 정의에 인라인 UNIQUE 를 써서 Postgres 가
-- 자동으로 붙인 것이다(`{테이블}_{컬럼}_key`). dev 의 오류 메시지가 이 이름을 그대로 찍었다.
-- 혹시 이름이 다르면 DROP 이 조용히 아무것도 안 하고 옛 제약이 남는다 — 그 경우
-- 재지원이 여전히 500 이므로 배포 후 실제 제출로 확인한다.
ALTER TABLE recruit_member DROP CONSTRAINT IF EXISTS recruit_member_student_id_key;
ALTER TABLE recruit_member DROP CONSTRAINT IF EXISTS recruit_member_phone_number_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_recruit_member_student_id_semester
    ON recruit_member (student_id, admission_semester);

CREATE UNIQUE INDEX IF NOT EXISTS uq_recruit_member_phone_number_semester
    ON recruit_member (phone_number, admission_semester);

-- 이메일에는 UNIQUE 를 걸지 않는다. 지금까지 제약이 없어 (email, admission_semester)
-- 중복 행이 이미 쌓여 있을 수 있고, 그러면 이 마이그레이션이 실패해 앱이 뜨지 못한다.
-- 학기당 1회 규칙은 RecruitMemberService 가 저장 전에 검사한다.
