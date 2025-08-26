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

    /**
     * RecruitMember와 관련 응답 데이터를 이용해 SpecifiedMemberResponse를 생성한다.
     *
     * RecruitMember에서 이름, 전공, 학번을 추출하고 isPayed는 null 안전하게 Boolean.TRUE.equals로 판정한다.
     * answers는 AnswersResponse.from으로 변환되어 응답에 포함된다.
     *
     * @param member  변환할 원본 RecruitMember
     * @param answers RecruitMember에 대한 Answer 목록 (AnswersResponse로 변환됨)
     * @return 생성된 SpecifiedMemberResponse 인스턴스
     */
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
