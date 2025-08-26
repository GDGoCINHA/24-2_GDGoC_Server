package inha.gdgoc.domain.recruit.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.recruit.entity.Answer;
import inha.gdgoc.domain.recruit.enums.InputType;
import java.util.List;
import java.util.Map;

public record AnswerResponse(
        Long id,
        InputType inputType,
        Object responseValue
) {
    /**
     * Answer 도메인 객체를 AnswerResponse DTO로 변환하여 생성합니다.
     *
     * 응답값(responseValue)은 내부적으로 JSON 문자열을 가능한 네이티브 Java 타입(문자열, 숫자, 불리언,
     * List, Map 등)으로 변환하여 설정합니다. JSON이 null 또는 빈 문자열이면 responseValue는 null이 됩니다;
     * 파싱/변환 오류가 발생하면 원본 JSON 문자열이 그대로 사용됩니다.
     *
     * @param answer DTO로 변환할 도메인 Answer 객체
     * @return 변환된 AnswerResponse 인스턴스
     */
    public static AnswerResponse from(Answer answer, ObjectMapper om) {
        return new AnswerResponse(
                answer.getId(),
                answer.getInputType(),
                toFriendlyValue(answer.getResponseValue(), om)
        );
    }

    /**
     * JSON 문자열을 가능한 한 친숙한 Java 값(문자열, 숫자, 불리언, List, Map 등)으로 변환한다.
     *
     * 입력이 null 또는 빈 문자열이면 null을 반환한다. 파싱에 실패하면 원본 JSON 문자열을 그대로 반환한다.
     *
     * @param json 변환할 JSON 문자열(Answer에 저장된 responseValue)
     * @return 변환된 값(String, Number, Boolean, List<Object>, Map<String,Object>) 또는
     *         입력이 null/빈 문자열이면 null, 파싱 실패 시 원본 json 문자열
     */
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