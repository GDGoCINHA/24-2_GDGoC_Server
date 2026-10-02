package inha.gdgoc.domain.club.post.exception;

import inha.gdgoc.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 팀 일반 게시글 에러 코드. 소모임 공통 코드는 {@code ClubErrorCode} 에 있다. */
@Getter
@RequiredArgsConstructor
public enum ClubPostErrorCode implements ErrorCode {
  POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
  POST_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "작성자만 고칠 수 있습니다."),
  POST_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "작성자와 리더, 운영진만 지울 수 있습니다.");

  private final HttpStatus status;
  private final String message;
}
