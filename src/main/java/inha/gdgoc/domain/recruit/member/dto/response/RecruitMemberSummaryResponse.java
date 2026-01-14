package inha.gdgoc.domain.recruit.member.dto.response;

import inha.gdgoc.domain.recruit.member.entity.RecruitMember;

public record RecruitMemberSummaryResponse(
        Long id,
        String name,
        String phoneNumber,
        String major,
        String studentId,
        String admissionSemester,
        Boolean isPayed
) {

    public static RecruitMemberSummaryResponse from(RecruitMember recruitMember) {
        String semester = null;
        if (recruitMember.getAdmissionSemester() != null) {
            String enumName = recruitMember.getAdmissionSemester().name();
            semester = enumName.substring(1).replace('_', '-');
        }

        return new RecruitMemberSummaryResponse(
                recruitMember.getId(),
                recruitMember.getName(),
                recruitMember.getPhoneNumber(),
                recruitMember.getMajor(),
                recruitMember.getStudentId(),
                semester,
                recruitMember.getIsPayed()
        );
    }
}
