package inha.gdgoc.domain.club.schedule.repository;

import inha.gdgoc.domain.club.schedule.entity.ClubScheduleRsvp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubScheduleRsvpRepository extends JpaRepository<ClubScheduleRsvp, Long> {

  Optional<ClubScheduleRsvp> findByScheduleIdAndUserId(Long scheduleId, Long userId);

  /** 일정 목록의 응답 집계를 한 번에 읽는다. */
  @Query(
      "select r from ClubScheduleRsvp r join fetch r.schedule s "
          + "where s.id in :scheduleIds")
  List<ClubScheduleRsvp> findAllByScheduleIds(@Param("scheduleIds") Collection<Long> scheduleIds);

  @Modifying(flushAutomatically = true)
  @Query("delete from ClubScheduleRsvp r where r.schedule.id = :scheduleId")
  void deleteByScheduleId(@Param("scheduleId") Long scheduleId);
}
