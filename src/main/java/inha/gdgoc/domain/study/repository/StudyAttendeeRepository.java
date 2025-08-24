package inha.gdgoc.domain.study.repository;

import inha.gdgoc.domain.study.entity.StudyAttendee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyAttendeeRepository extends JpaRepository<StudyAttendee, Long>, StudyAttendeeCustom {
    Optional<StudyAttendee> findStudyAttendeeByStudyIdAndUserId(Long studyId, Long userId);

    boolean existsByStudyIdAndUserId(Long studyId, Long userId);
}
