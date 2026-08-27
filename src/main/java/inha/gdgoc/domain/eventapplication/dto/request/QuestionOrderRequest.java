package inha.gdgoc.domain.eventapplication.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 질문 순서 일괄 변경. 폼의 살아 있는 질문 전체를 원하는 순서로 보낸다. */
public record QuestionOrderRequest(@NotEmpty List<Long> questionIds) {}
