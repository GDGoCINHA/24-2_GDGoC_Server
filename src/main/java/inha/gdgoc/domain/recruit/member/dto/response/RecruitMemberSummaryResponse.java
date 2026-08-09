package inha.gdgoc.domain.recruit.member.dto.response;

import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;

public record RecruitMemberSummaryResponse(
        Long id,
        String name,
        String phoneNumber,
        String major,
        String studentId,
        // 목록에서 학기를 걸러 볼 수 있어야 필터 결과를 확인할 수 있다.
        // 웹은 전부터 이 필드를 읽고 있었으나 서버가 내려주지 않아 항상 비어 있었다.
        AdmissionSemester admissionSemester,
        Boolean isPayed
) {

    public static RecruitMemberSummaryResponse from(RecruitMember recruitMember) {
        return new RecruitMemberSummaryResponse(
                recruitMember.getId(),
                recruitMember.getName(),
                recruitMember.getPhoneNumber(),
                recruitMember.getMajor(),
                recruitMember.getStudentId(),
                recruitMember.getAdmissionSemester(),
                recruitMember.getIsPayed()
        );
    }
}
