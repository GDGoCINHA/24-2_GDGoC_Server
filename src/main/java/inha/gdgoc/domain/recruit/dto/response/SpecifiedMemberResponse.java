package inha.gdgoc.domain.recruit.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.recruit.entity.Answer;
import inha.gdgoc.domain.recruit.entity.RecruitMember;
import java.util.List;

public record SpecifiedMemberResponse(
        String name,
        String major,
        String studentId,
        boolean isPayed,
        AnswersResponse answers
) {

    public static SpecifiedMemberResponse from(
            RecruitMember member,
            List<Answer> answers,
            ObjectMapper objectMapper
    ) {
        return new SpecifiedMemberResponse(
                member.getName(),
                member.getMajor(),
                member.getStudentId(),
                Boolean.TRUE.equals(member.getIsPayed()),
                AnswersResponse.from(answers, objectMapper)
        );
    }
}
