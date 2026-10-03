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
  SCHEDULE_ALREADY_RECORDED(HttpStatus.CONFLICT, "이 일정에는 이미 활동 기록이 있습니다."),
  NOT_IN_ROSTER(HttpStatus.FORBIDDEN, "이 활동 기록의 출석 명단에 없어 수정 요청을 할 수 없습니다."),
  FIX_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "출석 수정 요청을 찾을 수 없습니다."),
  FIX_REQUEST_ALREADY_PENDING(HttpStatus.CONFLICT, "이 기록에 이미 처리 대기 중인 수정 요청이 있습니다."),
  FIX_REQUEST_ALREADY_HANDLED(HttpStatus.CONFLICT, "이미 처리된 수정 요청입니다."),
  FIX_REQUEST_NOT_IN_ROSTER(
      HttpStatus.CONFLICT, "요청한 사람이 지금 이 기록의 명단에 없습니다. 활동일이 바뀌었다면 거절해 주세요.");

  private final HttpStatus status;
  private final String message;
}
