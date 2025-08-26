package inha.gdgoc.domain.recruit.dto.response;

import inha.gdgoc.domain.recruit.entity.RecruitMember;

public record SpecifiedMemberResponse(
        String name,
        String major,
        String studentId,
        boolean isPayed
) {

    public static SpecifiedMemberResponse from(RecruitMember member) {
        return new SpecifiedMemberResponse(
                member.getName(),
                member.getMajor(),
                member.getStudentId(),
                member.getIsPayed()
        );
    }
}
