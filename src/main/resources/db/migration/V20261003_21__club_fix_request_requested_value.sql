-- 출석 수정 요청 (B).
--
-- 1) 요청한 출석 값을 저장한다. 수락할 때 출석을 "반전" 하지 않고 요청한 값으로 맞춘다.
--    요청 뒤 리더가 기록을 직접 고쳤다면 수락해도 다시 뒤집히지 않는다.
--    이 테이블을 쓰는 API 가 배포된 적이 없어 행이 없지만, 혹시 있어도 실패하지 않게
--    기본값을 붙여 추가한 뒤 기본값을 뗀다(새 행은 반드시 값을 넣게).
ALTER TABLE club_attendance_fix_request
    ADD COLUMN IF NOT EXISTS requested_attended BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE club_attendance_fix_request
    ALTER COLUMN requested_attended DROP DEFAULT;

-- 2) 같은 기록·같은 사람의 처리 대기 요청은 하나만. 처리된 요청(ACCEPTED·REJECTED)은 몇 개든 남는다.
CREATE UNIQUE INDEX IF NOT EXISTS uq_club_fix_request_pending
    ON club_attendance_fix_request (activity_id, user_id)
    WHERE status = 'PENDING';
