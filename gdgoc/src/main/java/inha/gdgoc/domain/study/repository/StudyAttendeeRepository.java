package inha.gdgoc.domain.study.repository;

import inha.gdgoc.domain.study.entity.StudyAttendee;
import java.awt.print.Pageable;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyAttendeeRepository extends JpaRepository<StudyAttendee, Long>, StudyAttendeeCustom {
    Page<StudyAttendee> findAllByStudyId(Long studyId, Pageable pageable);

    Optional<StudyAttendee> findStudyAttendeeByStudyIdAndUserId(Long studyId, Long userId);
}
