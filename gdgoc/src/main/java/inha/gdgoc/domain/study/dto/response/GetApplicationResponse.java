package inha.gdgoc.domain.study.dto.response;

import inha.gdgoc.domain.study.entity.StudyAttendee;

public record GetApplicationResponse(String name, String phone, String major, String studentId, String introduce,
                                     String activityTime) {
    public static GetApplicationResponse from(StudyAttendee studyAttendee) {
        return new GetApplicationResponse(studyAttendee.getUser().getName(),
                studyAttendee.getUser().getPhoneNumber(), studyAttendee.getUser().getMajor(),
                studyAttendee.getUser().getStudentId(), studyAttendee.getIntroduce(), studyAttendee.getActivityTime());
    }
}
