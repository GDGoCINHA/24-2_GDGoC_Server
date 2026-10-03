package inha.gdgoc.domain.club.activity.repository;

import inha.gdgoc.domain.club.activity.dto.ApprovedActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubActivityRepository extends JpaRepository<ClubActivity, Long> {

  /** 상태를 바꾸기 전에 잠가서 읽는다. 운영진 검토와 리더의 출석 수정 수락이 같은 기록을 동시에 바꾸지 못하게 한다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from ClubActivity a where a.id = :id")
  Optional<ClubActivity> findByIdForUpdate(@Param("id") Long id);

  /** 일정 하나에 기록은 하나다. 운영 DB 는 유니크 제약으로도 막지만 테스트 DB(H2)에는 그 제약이 없다. */
  boolean existsByScheduleId(Long scheduleId);

  boolean existsByScheduleIdAndIdNot(Long scheduleId, Long activityId);

  /** 피드 한 페이지의 기록. 필요 참석 인원을 계산하려고 기수까지 함께 읽는다. 순서는 부르는 쪽이 맞춘다. */
  @Query("select a from ClubActivity a join fetch a.club c join fetch c.term where a.id in :ids")
  List<ClubActivity> findAllWithClub(@Param("ids") Collection<Long> ids);

  /** 전체 활동 피드. 숨김 소모임은 빼고 올라온 순서대로. 상태는 가리지 않는다(카드에 상태 배지를 단다). */
  @Query(
      value =
          "select a from ClubActivity a join fetch a.club c join fetch c.term "
              + "where c.status <> inha.gdgoc.domain.club.club.enums.ClubStatus.HIDDEN "
              + "order by a.createdAt desc, a.id desc",
      countQuery =
          "select count(a) from ClubActivity a "
              + "where a.club.status <> inha.gdgoc.domain.club.club.enums.ClubStatus.HIDDEN")
  Page<ClubActivity> findGlobalFeed(Pageable pageable);

  /** 일정마다 연결된 기록 id. [scheduleId, activityId]. 일정 목록이 「기록 작성」 대신 「기록 보기」 를 띄우는 데 쓴다. */
  @Query("select a.schedule.id, a.id from ClubActivity a where a.schedule.id in :scheduleIds")
  List<Object[]> findScheduleLinks(@Param("scheduleIds") Collection<Long> scheduleIds);

  /**
   * 일정을 지울 때 연결만 끊는다. 기록은 일정 없이 진행한 활동으로 남는다.
   *
   * <p>운영 DB 는 {@code ON DELETE SET NULL} 로도 끊지만 테스트 DB(H2)에는 그 제약이 없다. 인증 완료된 기록도 끊는다 — 일정 연결은 완주
   * 계산에 쓰이지 않는다.
   */
  @Modifying(flushAutomatically = true)
  @Query("update ClubActivity a set a.schedule = null where a.schedule.id = :scheduleId")
  void detachSchedule(@Param("scheduleId") Long scheduleId);

  /** 기간 안의 기록마다 명단 수와 출석 수. 명단 행이 하나도 없는 기록도 0/0 으로 나온다. */
  @Query(
      """
      select new inha.gdgoc.domain.club.activity.dto.ApprovedActivity(
          a.id,
          a.activityDate,
          count(att.id),
          coalesce(sum(case when att.attended = true then 1L else 0L end), 0L))
      from ClubActivity a
      left join ClubActivityAttendance att on att.activity = a
      where a.club.id = :clubId
        and a.status = :status
        and a.activityDate between :from and :to
      group by a.id, a.activityDate
      order by a.activityDate, a.id
      """)
  List<ApprovedActivity> summarizeByStatus(
      @Param("clubId") Long clubId,
      @Param("status") ClubActivityStatus status,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);
}
