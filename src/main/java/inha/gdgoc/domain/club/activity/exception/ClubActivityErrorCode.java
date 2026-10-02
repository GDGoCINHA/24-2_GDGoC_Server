package inha.gdgoc.domain.club.activity.exception;

import inha.gdgoc.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 활동 기록(인증)·출석 에러 코드. 소모임 공통 코드는 {@code ClubErrorCode} 에 있다. */
@Getter
@RequiredArgsConstructor
public enum ClubActivityErrorCode implements ErrorCode {
  ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "활동 기록을 찾을 수 없습니다."),
  ACTIVITY_LOCKED(HttpStatus.CONFLICT, "인증 완료된 활동 기록은 수정할 수 없습니다."),
  SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "이 소모임의 일정을 찾을 수 없습니다."),
  SCHEDULE_ALREADY_RECORDED(HttpStatus.CONFLICT, "이 일정에는 이미 활동 기록이 있습니다.");

  private final HttpStatus status;
  private final String message;
}
