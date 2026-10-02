package inha.gdgoc.domain.club.schedule.repository;

import inha.gdgoc.domain.club.schedule.entity.ClubScheduleCheckin;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubScheduleCheckinRepository extends JpaRepository<ClubScheduleCheckin, Long> {

  /** QR 로 체크인한 사람. 활동 기록 작성 화면에서 출석 체크의 기본값으로만 쓴다. */
  @Query("select c.user.id from ClubScheduleCheckin c where c.schedule.id = :scheduleId")
  List<Long> findUserIdsByScheduleId(@Param("scheduleId") Long scheduleId);
}
