package inha.gdgoc.domain.recruit.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.recruit.entity.Answer;
import java.util.List;

public record AnswersResponse(
        List<AnswerResponse> answers
) {
    /**
     * Answer 엔티티 목록을 AnswerResponse 목록으로 변환해 포함한 AnswersResponse를 생성한다.
     *
     * entities의 각 Answer는 {@link AnswerResponse#from(Answer, com.fasterxml.jackson.databind.ObjectMapper)} 를 통해 변환된다.
     *
     * @param entities 변환할 Answer 엔티티 리스트
     * @return 변환된 AnswerResponse 리스트를 담은 새 AnswersResponse 인스턴스
     */
    public static AnswersResponse from(List<Answer> entities, ObjectMapper objectMapper) {
        return new AnswersResponse(
                entities.stream()
                        .map(a -> AnswerResponse.from(a, objectMapper))
                        .toList()
        );
    }
}