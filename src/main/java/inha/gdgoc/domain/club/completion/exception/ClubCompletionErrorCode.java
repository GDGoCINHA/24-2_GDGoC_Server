package inha.gdgoc.domain.club.completion.exception;

import inha.gdgoc.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 목표·완주 에러 코드. 소모임 공통 코드는 {@code ClubErrorCode} 에 있다. */
@Getter
@RequiredArgsConstructor
public enum ClubCompletionErrorCode implements ErrorCode {
  GOAL_JUDGEMENT_INVALID(HttpStatus.BAD_REQUEST, "목표 판정은 ACHIEVED 또는 NOT_ACHIEVED 만 가능합니다."),
  COMPLETION_DECISION_INVALID(HttpStatus.BAD_REQUEST, "완주 확정은 COMPLETED 또는 FAILED 만 가능합니다."),
  NOT_LEADER_OR_STAFF(HttpStatus.FORBIDDEN, "이 소모임의 리더나 운영진만 할 수 있습니다.");

  private final HttpStatus status;
  private final String message;
}
