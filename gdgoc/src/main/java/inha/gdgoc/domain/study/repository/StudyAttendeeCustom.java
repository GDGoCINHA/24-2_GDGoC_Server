package inha.gdgoc.domain.study.repository;

import inha.gdgoc.domain.study.entity.StudyAttendee;

import java.util.List;

public interface StudyAttendeeCustom {

    List<StudyAttendee> pageAllByStudyId(Long studyId, Long limit, Long offset);

    List<StudyAttendee> findAllByIdsAndStudyId(List<Long> ids, Long studyId);

    List<StudyAttendee> findAllByUserId(Long userId);

    Long findAllByStudyIdStudyAttendeeCount(Long studyId);

}
