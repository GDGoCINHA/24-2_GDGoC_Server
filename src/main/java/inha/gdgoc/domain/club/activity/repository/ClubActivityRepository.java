package inha.gdgoc.domain.club.activity.repository;

import inha.gdgoc.domain.club.activity.dto.ApprovedActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubActivityRepository extends JpaRepository<ClubActivity, Long> {

  /** 상태를 바꾸기 전에 잠가서 읽는다. 운영진 검토와 이끔이의 출석 수정 수락이 같은 기록을 동시에 바꾸지 못하게 한다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from ClubActivity a where a.id = :id")
  Optional<ClubActivity> findByIdForUpdate(@Param("id") Long id);

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
