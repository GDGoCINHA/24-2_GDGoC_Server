-- 신청 폼에 '작성 중' 상태를 준다.
--
-- 폼을 만들자마자 부원에게 보이면 질문을 다 넣기 전 반쯤 만들어진 폼이 노출된다.
-- published_at 이 NULL 인 동안은 공개 조회가 404 를 주고, 운영진 화면에서만 보인다.
--
-- is_open 과는 다른 축이다.
--   published_at IS NULL  : 아직 만드는 중 — 부원에게 아무것도 안 보인다
--   published_at 있음 + is_open : 신청 받는 중
--   published_at 있음 + !is_open: 마감 — 폼은 보이되 신청 버튼이 잠긴다
--
-- 발행은 되돌리지 않는다. 이미 신청한 사람의 화면이 통째로 사라지면 안 되기 때문이고,
-- 그만 받고 싶을 때는 is_open 을 내리면 된다.
ALTER TABLE event_application_form
    ADD COLUMN published_at TIMESTAMPTZ NULL;

-- 이 컬럼이 생기기 전에 만들어진 폼은 이미 부원에게 보이고 있었다.
-- NULL 로 두면 그 폼들이 갑자기 사라지므로 발행된 것으로 본다.
UPDATE event_application_form
SET published_at = created_at
WHERE published_at IS NULL;
