package inha.gdgoc.domain.core.attendance.repository;

import inha.gdgoc.domain.core.attendance.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface MeetingRepository extends JpaRepository<Meeting, LocalDate> {
}