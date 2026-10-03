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
  NOT_CLUB_LEADER(HttpStatus.FORBIDDEN, "이 소모임의 리더만 할 수 있습니다."),
  TERM_NOT_FOUND(HttpStatus.BAD_REQUEST, "기수가 없습니다. 운영진에게 기수를 먼저 만들어 달라고 요청해 주세요."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  NOT_RECRUITING(HttpStatus.BAD_REQUEST, "지금은 참여 신청을 받지 않는 소모임입니다."),
  ALREADY_APPLIED(HttpStatus.CONFLICT, "이미 신청했거나 참여 중인 소모임입니다."),
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버 정보를 찾을 수 없습니다."),
  INVALID_MEMBER_STATE(HttpStatus.CONFLICT, "이미 처리된 신청이거나 지금 상태에서는 할 수 없습니다."),
  LEADER_CANNOT_LEAVE(HttpStatus.CONFLICT, "리더는 다른 멤버에게 리더를 넘긴 뒤 탈퇴할 수 있습니다."),
  CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "자기 자신은 내보낼 수 없습니다."),
  TARGET_NOT_ACTIVE_MEMBER(HttpStatus.BAD_REQUEST, "이 소모임의 멤버에게만 리더를 넘길 수 있습니다."),
  CLUB_NOT_PENDING(HttpStatus.CONFLICT, "승인 대기 중인 소모임만 승인하거나 반려할 수 있습니다."),
  CLUB_DELETE_NOT_ALLOWED(
      HttpStatus.CONFLICT, "공개된 소모임은 리더가 삭제할 수 없습니다. 운영진에게 요청해 주세요.");

  private final HttpStatus status;
  private final String message;
}
