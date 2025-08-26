package inha.gdgoc.domain.recruit.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AnswerCreateRequest {
    private String surveyType;
    private String inputType;
    private JsonNode responseValue;
}
