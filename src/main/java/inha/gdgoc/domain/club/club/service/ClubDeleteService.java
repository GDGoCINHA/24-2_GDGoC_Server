package inha.gdgoc.domain.club.club.service;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소모임 삭제. 딸린 기록(일정·활동 기록·게시글·반응·멤버·완주)까지 지운다 — 되돌릴 수 없다.
 *
 * <p>리더는 승인 전(PENDING·REJECTED) 소모임만 지운다. 공개된 소모임은 팀원과 기록이 있어 운영진만 지운다.
 * 좋아요·댓글은 FK 없이 대상 id 로만 걸려 있어 따로 지운다.
 */
@Service
@RequiredArgsConstructor
public class ClubDeleteService {

  private static final List<ClubStatus> LEADER_DELETABLE =
      List.of(ClubStatus.PENDING, ClubStatus.REJECTED);

  /** 자식부터 지운다. 순서를 바꾸면 FK 에 걸린다. */
  private static final List<String> STATEMENTS =
      List.of(
          "delete from club_comment where (target_type = 'POST' and target_id in"
              + " (select id from club_post where club_id = :id)) or (target_type = 'ACTIVITY'"
              + " and target_id in (select id from club_activity where club_id = :id))",
          "delete from club_like where (target_type = 'POST' and target_id in"
              + " (select id from club_post where club_id = :id)) or (target_type = 'ACTIVITY'"
              + " and target_id in (select id from club_activity where club_id = :id))",
          "delete from club_activity_attendance where activity_id in"
              + " (select id from club_activity where club_id = :id)",
          "delete from club_activity_photo where activity_id in"
              + " (select id from club_activity where club_id = :id)",
          "delete from club_attendance_fix_request where activity_id in"
              + " (select id from club_activity where club_id = :id)",
          "delete from club_activity where club_id = :id",
          "delete from club_schedule_checkin where schedule_id in"
              + " (select id from club_schedule where club_id = :id)",
          "delete from club_schedule_rsvp where schedule_id in"
              + " (select id from club_schedule where club_id = :id)",
          "delete from club_schedule where club_id = :id",
          "delete from club_post where club_id = :id",
          "delete from club_completion where club_id = :id",
          "delete from club_rest_week where club_id = :id",
          "delete from club_member where club_id = :id",
          "delete from club where id = :id");

  private final ClubAccessService clubAccessService;
  private final EntityManager entityManager;

  @Transactional
  public void deleteByLeader(Long clubId, Long userId) {
    Club club = clubAccessService.requireLeader(clubId, userId);
    if (!LEADER_DELETABLE.contains(club.getStatus())) {
      throw new BusinessException(ClubErrorCode.CLUB_DELETE_NOT_ALLOWED);
    }
    delete(clubId);
  }

  @Transactional
  public void deleteByStaff(Long clubId) {
    clubAccessService.getClub(clubId);
    delete(clubId);
  }

  private void delete(Long clubId) {
    // 영속성 컨텍스트에 올라온 Club 이 네이티브 삭제 뒤 다시 flush 되지 않게 비운다.
    entityManager.flush();
    entityManager.clear();
    for (String sql : STATEMENTS) {
      entityManager.createNativeQuery(sql).setParameter("id", clubId).executeUpdate();
    }
  }
}
