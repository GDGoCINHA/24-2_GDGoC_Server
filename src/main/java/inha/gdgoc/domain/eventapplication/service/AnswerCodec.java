package inha.gdgoc.domain.eventapplication.service;

import static inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode.ANSWER_SERIALIZE_FAILED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationAnswer;
import inha.gdgoc.global.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 답변 값과 jsonb 문자열 사이를 오간다.
 *
 * <p>컬럼이 jsonb 라 Hibernate 는 String 을 JSON 텍스트 그대로 바인딩한다. 즉 여기서 만든 문자열이 DB 에 그대로 들어가며, 문자열 답변이 이중으로
 * 인용되는 일은 없다.
 */
@Component
@RequiredArgsConstructor
public class AnswerCodec {

  private final ObjectMapper objectMapper;

  public String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new BusinessException(ANSWER_SERIALIZE_FAILED);
    }
  }

  public Object read(String raw) {
    try {
      return objectMapper.readValue(raw, Object.class);
    } catch (JsonProcessingException e) {
      // 저장할 때 직렬화한 값이라 깨질 일은 없지만, 한 건 때문에 목록 전체를 막지는 않는다.
      return null;
    }
  }

  public Map<Long, Object> readAll(EventApplication application) {
    Map<Long, Object> answers = new LinkedHashMap<>();
    for (EventApplicationAnswer answer : application.getAnswers()) {
      answers.put(answer.getQuestionId(), read(answer.getValue()));
    }
    return answers;
  }
}
