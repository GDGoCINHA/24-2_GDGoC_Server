package inha.gdgoc.domain.study.repository;

import inha.gdgoc.domain.study.entity.StudyAttendee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyAttendeeRepository extends JpaRepository<StudyAttendee, Long>, StudyAttendeeCustom {
}
