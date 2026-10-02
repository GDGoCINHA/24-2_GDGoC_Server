package inha.gdgoc.domain.club.schedule.exception;

import inha.gdgoc.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 일정·참석 응답·QR 체크인 에러 코드. 소모임 공통 코드는 {@code ClubErrorCode} 에 있다. */
@Getter
@RequiredArgsConstructor
public enum ClubScheduleErrorCode implements ErrorCode {
  SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
  CHECKIN_TOKEN_INVALID(
      HttpStatus.BAD_REQUEST, "QR 이 만료됐거나 올바르지 않습니다. 화면에 띄운 QR 을 다시 찍어 주세요.");

  private final HttpStatus status;
  private final String message;
}
