package inha.gdgoc.domain.board.notice.exception;

import inha.gdgoc.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NoticeErrorCode implements ErrorCode {
  PINNED_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "상단 고정은 최대 3개까지 가능합니다."),
  PINNED_NOT_ELIGIBLE(HttpStatus.BAD_REQUEST, "고정할 수 없는 공지가 포함되어 있습니다.");

  private final HttpStatus status;
  private final String message;
}
