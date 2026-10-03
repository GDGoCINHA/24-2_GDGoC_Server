package inha.gdgoc.domain.club.activity.repository;

import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubActivityAttendanceRepository
    extends JpaRepository<ClubActivityAttendance, Long> {

  @Query(
      "select a from ClubActivityAttendance a join fetch a.user "
          + "where a.activity.id = :activityId order by a.user.name asc, a.user.id asc")
  List<ClubActivityAttendance> findAllWithUser(@Param("activityId") Long activityId);

  /** 그 기록의 명단에 이 사람이 있는가 — 출석 수정 요청의 자격이자 수락 대상. */
  @Query(
      "select a from ClubActivityAttendance a join fetch a.user "
          + "where a.activity.id = :activityId and a.user.id = :userId")
  Optional<ClubActivityAttendance> findByActivityIdAndUserId(
      @Param("activityId") Long activityId, @Param("userId") Long userId);

  /** 피드 카드의 인원 수. [activityId, 명단 수, 출석 수]. 명단 행이 없는 기록은 빠진다(0/0 으로 본다). */
  @Query(
      "select a.activity.id, count(a), sum(case when a.attended = true then 1L else 0L end) "
          + "from ClubActivityAttendance a where a.activity.id in :activityIds group by a.activity.id")
  List<Object[]> countByActivityIds(@Param("activityIds") Collection<Long> activityIds);

  /** 한 소모임에서 내가 명단에 든 회차. 최근 활동일부터. */
  @Query(
      "select a from ClubActivityAttendance a join fetch a.activity act left join fetch act.schedule "
          + "where act.club.id = :clubId and a.user.id = :userId "
          + "order by act.activityDate desc, act.id desc")
  List<ClubActivityAttendance> findMine(
      @Param("clubId") Long clubId, @Param("userId") Long userId);

  /** 명단을 다시 만들기 전에 지운다. 같은 (활동, 사용자) 를 다시 넣으므로 먼저 DB 에서 지워져 있어야 한다. */
  @Modifying(flushAutomatically = true)
  @Query("delete from ClubActivityAttendance a where a.activity.id = :activityId")
  void deleteByActivityId(@Param("activityId") Long activityId);
}
