package inha.gdgoc.domain.club.reaction.exception;

import inha.gdgoc.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 좋아요·댓글 에러 코드. */
@Getter
@RequiredArgsConstructor
public enum ClubReactionErrorCode implements ErrorCode {
  TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글이나 활동 기록을 찾을 수 없습니다."),
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
  COMMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "작성자, 팀 리더, 운영진만 댓글을 지울 수 있습니다.");

  private final HttpStatus status;
  private final String message;
}
