package inha.gdgoc.domain.club.schedule.repository;

import inha.gdgoc.domain.club.schedule.entity.ClubSchedule;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubScheduleRepository extends JpaRepository<ClubSchedule, Long> {

  /** 다가오는 일정. 가까운 것부터. */
  List<ClubSchedule> findAllByClubIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(
      Long clubId, Instant from);

  /** 지난 일정. 최근 것부터. */
  List<ClubSchedule> findAllByClubIdAndStartsAtLessThanOrderByStartsAtDesc(
      Long clubId, Instant before);
}
