package inha.gdgoc.domain.core.attendance.repository;

import inha.gdgoc.domain.core.attendance.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Optional<Meeting> findByMeetingDate(LocalDate meetingDate);
}