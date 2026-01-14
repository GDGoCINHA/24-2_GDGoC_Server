package inha.gdgoc.domain.recruit.member.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.recruit.member.entity.Answer;
import java.util.List;

public record AnswersResponse(
        List<AnswerResponse> answers
) {
    public static AnswersResponse from(List<Answer> entities, ObjectMapper objectMapper) {
        return new AnswersResponse(
                entities.stream()
                        .map(a -> AnswerResponse.from(a, objectMapper))
                        .toList()
        );
    }
}