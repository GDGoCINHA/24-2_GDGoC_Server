package inha.gdgoc.domain.recruit.member.dto.response;

import inha.gdgoc.domain.recruit.member.entity.RecruitMember;

public record RecruitMemberSummaryResponse(
        Long id,
        String name,
        String phoneNumber,
        String major,
        String studentId,
        Boolean isPayed
) {

    public static RecruitMemberSummaryResponse from(RecruitMember recruitMember) {
        return new RecruitMemberSummaryResponse(
                recruitMember.getId(),
                recruitMember.getName(),
                recruitMember.getPhoneNumber(),
                recruitMember.getMajor(),
                recruitMember.getStudentId(),
                recruitMember.getIsPayed()
        );
    }
}
