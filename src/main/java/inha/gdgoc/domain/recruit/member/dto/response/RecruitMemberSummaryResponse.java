package inha.gdgoc.domain.recruit.member.dto.response;

import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import java.time.Instant;

public record RecruitMemberSummaryResponse(
        Long id,
        String name,
        String phoneNumber,
        String major,
        String studentId,
        // 목록에서 학기를 걸러 볼 수 있어야 필터 결과를 확인할 수 있다.
        // 웹은 전부터 이 필드를 읽고 있었으나 서버가 내려주지 않아 항상 비어 있었다.
        AdmissionSemester admissionSemester,
        Boolean isPayed,
        // 목록의 기본 정렬이 createdAt DESC 인데 정작 값을 안 내려줘서, 화면은 순서만 알고
        // 「언제 냈는지」는 지원자를 하나씩 열어야 알 수 있었다. 상세 응답에는 원래 있던 값이다.
        Instant createdAt
) {

    public static RecruitMemberSummaryResponse from(RecruitMember recruitMember) {
        return new RecruitMemberSummaryResponse(
                recruitMember.getId(),
                recruitMember.getName(),
                recruitMember.getPhoneNumber(),
                recruitMember.getMajor(),
                recruitMember.getStudentId(),
                recruitMember.getAdmissionSemester(),
                recruitMember.getIsPayed(),
                recruitMember.getCreatedAt()
        );
    }
}
