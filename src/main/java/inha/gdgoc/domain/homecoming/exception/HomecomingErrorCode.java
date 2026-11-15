package inha.gdgoc.domain.homecoming.exception;

import inha.gdgoc.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 신청 화면은 HTTP 상태로 분기한다 — 409 중복, 403 마감, 400 입력 오류. 상태를 바꾸면 초대장 화면({@code api.js})도 함께 고친다.
 */
@Getter
@RequiredArgsConstructor
public enum HomecomingErrorCode implements ErrorCode {
  CLOSED(HttpStatus.FORBIDDEN, "신청이 마감되었습니다. 개별적으로 연락드린 HR팀원에게 답장해 주세요."),
  DUPLICATE_PHONE(HttpStatus.CONFLICT, "이미 신청하셨어요. 변경은 개별적으로 연락드린 HR팀원에게 답장 주세요."),
  PRIVACY_REQUIRED(HttpStatus.BAD_REQUEST, "개인정보 수집·이용에 동의해야 신청할 수 있습니다."),
  PHONE_INVALID(HttpStatus.BAD_REQUEST, "전화번호 11자리를 입력해 주세요."),
  NAME_REQUIRED(HttpStatus.BAD_REQUEST, "이름을 입력해 주세요."),
  ACTIVITY_REQUIRED(HttpStatus.BAD_REQUEST, "활동 기간과 활동 구분을 선택해 주세요."),
  YEAR_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "활동 연도는 2021년부터 2026년 사이여야 합니다."),
  YEAR_ORDER_INVALID(HttpStatus.BAD_REQUEST, "마지막 연도는 시작 연도와 같거나 이후여야 해요."),
  TEAM_REQUIRED(HttpStatus.BAD_REQUEST, "운영진은 활동 팀을 선택해 주세요."),
  ARRIVAL_TIME_INVALID(HttpStatus.BAD_REQUEST, "늦게 도착한다면 예상 도착 시간을 목록에서 선택해 주세요."),
  APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."),
  APPLICATION_CANCELED(HttpStatus.BAD_REQUEST, "취소된 신청은 수정할 수 없습니다."),
  CONSENT_NOT_AGREED(HttpStatus.BAD_REQUEST, "동의하지 않은 항목은 철회할 수 없습니다."),
  CONSENT_NOT_LONG_TERM(HttpStatus.BAD_REQUEST, "행사 운영 동의는 철회 대상이 아닙니다. 신청 취소로 처리해 주세요.");

  private final HttpStatus status;
  private final String message;
}
