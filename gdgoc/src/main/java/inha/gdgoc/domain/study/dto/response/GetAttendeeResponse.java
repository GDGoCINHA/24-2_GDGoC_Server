package inha.gdgoc.domain.study.dto.response;

import inha.gdgoc.domain.study.entity.StudyAttendee;

public record GetAttendeeResponse(Long id, String name, String major, String studentId, String status) {
    public static GetAttendeeResponse from(StudyAttendee studyAttendee) {
        return new GetAttendeeResponse(studyAttendee.getId(), studyAttendee.getUser().getName(),
                studyAttendee.getUser().getMajor(), studyAttendee.getUser().getStudentId(),
                studyAttendee.getStatus().getValue());
    }
}
