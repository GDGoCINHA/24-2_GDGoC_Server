package inha.gdgoc.domain.club.common.exception;

import inha.gdgoc.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 소모임 전체가 함께 쓰는 에러 코드.
 *
 * <p>담당(A·B·C)마다 고유한 코드는 각자 하위 패키지에 따로 둬도 된다. 여기는 권한·존재 확인처럼 여러 담당이 함께 부르는 것만 둔다.
 */
@Getter
@RequiredArgsConstructor
public enum ClubErrorCode implements ErrorCode {
  CLUB_NOT_FOUND(HttpStatus.NOT_FOUND, "소모임을 찾을 수 없습니다."),
  NOT_CLUB_MEMBER(HttpStatus.FORBIDDEN, "이 소모임의 멤버만 할 수 있습니다."),
  NOT_CLUB_LEADER(HttpStatus.FORBIDDEN, "이 소모임의 이끔이만 할 수 있습니다."),
  LEADER_GRANT_REQUIRED(HttpStatus.FORBIDDEN, "이끔이 권한이 있어야 소모임을 개설할 수 있습니다.");

  private final HttpStatus status;
  private final String message;
}
