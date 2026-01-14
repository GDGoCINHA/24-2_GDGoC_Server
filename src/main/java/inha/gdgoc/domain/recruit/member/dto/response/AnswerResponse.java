package inha.gdgoc.domain.recruit.member.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.recruit.member.entity.Answer;
import inha.gdgoc.domain.recruit.member.enums.InputType;
import java.util.List;
import java.util.Map;

public record AnswerResponse(
        Long id,
        InputType inputType,
        Object responseValue
) {
    public static AnswerResponse from(Answer answer, ObjectMapper om) {
        return new AnswerResponse(
                answer.getId(),
                answer.getInputType(),
                toFriendlyValue(answer.getResponseValue(), om)
        );
    }

    private static Object toFriendlyValue(String json, ObjectMapper om) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = om.readTree(json);

            if (node.isTextual()) {
                return node.asText();
            }
            if (node.isArray()) {
                return om.convertValue(node, new TypeReference<List<Object>>() {});
            }
            if (node.isObject()) {
                return om.convertValue(node, new TypeReference<Map<String, Object>>() {});
            }
            if (node.isNumber()) {
                return node.numberValue();
            }
            if (node.isBoolean()) {
                return node.booleanValue();
            }
            return null;
        } catch (Exception e) {
            return json;
        }
    }
}