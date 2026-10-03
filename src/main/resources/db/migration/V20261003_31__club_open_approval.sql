-- 소모임 개설 승인.
--
-- 개설 신청서 → 리더 권한 부여 → 개설 폼 재작성의 세 단계를 하나로 줄였다. 이제 개설 폼을 내면
-- 소모임이 PENDING 으로 바로 생기고, 운영진이 승인(ACTIVE)하거나 반려(REJECTED)한다.
-- status 는 VARCHAR 라 값만 늘었고, 반려 사유 칸만 더한다.
-- club_leader_grant·club_open_request 는 더 쓰지 않지만 기록으로 남겨 둔다.
ALTER TABLE club ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(500);
