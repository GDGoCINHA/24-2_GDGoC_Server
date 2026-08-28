package inha.gdgoc.domain.eventapplication.exception;

import inha.gdgoc.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EventApplicationErrorCode implements ErrorCode {
  EVENT_BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."),
  FORM_NOT_FOUND(HttpStatus.NOT_FOUND, "이 행사는 신청을 받고 있지 않습니다."),
  FORM_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 신청 폼이 있는 행사입니다."),
  FORM_HAS_APPLICATIONS(HttpStatus.BAD_REQUEST, "신청자가 있어 신청 폼을 삭제할 수 없습니다. 그만 받으려면 마감해 주세요."),
  EVENT_ENDED(HttpStatus.BAD_REQUEST, "이미 끝난 행사입니다."),
  QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),

  OPTIONS_REQUIRED(HttpStatus.BAD_REQUEST, "선택형 질문에는 선택지가 필요합니다."),
  OPTIONS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "이 유형의 질문에는 선택지를 둘 수 없습니다."),
  OPTION_VALUE_DUPLICATED(HttpStatus.BAD_REQUEST, "선택지 값이 중복됩니다."),
  OPTION_VALUE_BLANK(HttpStatus.BAD_REQUEST, "선택지 값과 라벨은 비워둘 수 없습니다."),

  QUESTION_SHAPE_LOCKED(HttpStatus.BAD_REQUEST, "신청자가 있어 질문 유형과 선택지를 바꿀 수 없습니다."),
  QUESTION_REQUIRED_LOCKED(HttpStatus.BAD_REQUEST, "신청자가 있어 선택 질문을 필수로 바꿀 수 없습니다."),
  QUESTION_MUST_BE_OPTIONAL(HttpStatus.BAD_REQUEST, "신청자가 있으면 선택 질문만 추가할 수 있습니다."),
  OPTION_VALUE_LOCKED(HttpStatus.BAD_REQUEST, "신청자가 있어 선택지를 지울 수 없습니다. 라벨은 고칠 수 있습니다."),

  CONDITION_QUESTION_NOT_FOUND(HttpStatus.BAD_REQUEST, "조건의 기준 질문을 찾을 수 없습니다."),
  CONDITION_QUESTION_NOT_BEFORE(HttpStatus.BAD_REQUEST, "조건의 기준은 앞 순서 질문이어야 합니다."),
  CONDITION_QUESTION_TYPE_INVALID(HttpStatus.BAD_REQUEST, "선택형 질문만 조건의 기준이 될 수 있습니다."),
  CONDITION_VALUE_INVALID(HttpStatus.BAD_REQUEST, "조건 값이 기준 질문의 선택지에 없습니다."),
  CONDITION_VALUES_EMPTY(HttpStatus.BAD_REQUEST, "조건 값을 하나 이상 지정해야 합니다."),
  CONDITION_REFERENCED(HttpStatus.BAD_REQUEST, "이 질문을 기준으로 삼는 조건이 있습니다. 조건을 먼저 푸세요."),
  CONDITION_OPTION_REFERENCED(
      HttpStatus.BAD_REQUEST, "다른 질문이 조건으로 삼는 선택지는 지울 수 없습니다. 조건을 먼저 푸세요."),
  CONDITION_ORDER_BROKEN(HttpStatus.BAD_REQUEST, "이 순서로 바꾸면 조건이 뒤 질문을 가리키게 됩니다."),

  CAPACITY_BELOW_APPLICANTS(HttpStatus.BAD_REQUEST, "현재 신청자 수보다 적은 정원으로는 줄일 수 없습니다."),
  PERIOD_INVALID(HttpStatus.BAD_REQUEST, "신청 시작이 마감보다 늦을 수 없습니다."),
  SORT_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "순서를 바꿀 질문 목록이 현재 질문과 일치하지 않습니다."),

  NOT_OPEN_YET(HttpStatus.BAD_REQUEST, "아직 신청 기간이 아닙니다."),
  ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "신청이 마감되었습니다."),
  FORM_CLOSED(HttpStatus.BAD_REQUEST, "지금은 신청을 받지 않습니다."),
  NOT_ELIGIBLE(HttpStatus.FORBIDDEN, "이 행사에 신청할 수 있는 권한이 아닙니다."),
  CAPACITY_FULL(HttpStatus.BAD_REQUEST, "정원이 찼습니다."),
  ALREADY_APPLIED(HttpStatus.CONFLICT, "이미 신청한 행사입니다."),
  APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "신청 내역이 없습니다."),

  ANSWER_REQUIRED(HttpStatus.BAD_REQUEST, "필수 질문에 답하지 않았습니다."),
  AGREEMENT_REQUIRED(HttpStatus.BAD_REQUEST, "필수 동의 항목에 동의해야 신청할 수 있습니다."),
  ANSWER_TYPE_INVALID(HttpStatus.BAD_REQUEST, "답변 형태가 질문 유형과 맞지 않습니다."),
  ANSWER_VALUE_INVALID(HttpStatus.BAD_REQUEST, "선택지에 없는 값입니다."),
  ANSWER_QUESTION_UNKNOWN(HttpStatus.BAD_REQUEST, "이 폼에 없는 질문에 답했습니다."),
  ANSWER_SERIALIZE_FAILED(HttpStatus.BAD_REQUEST, "답변을 저장할 수 없는 형태입니다."),

  CHECKIN_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "QR 이 만료되었습니다. 화면의 QR 을 다시 찍어주세요."),
  CHECKIN_NOT_IN_PERIOD(HttpStatus.BAD_REQUEST, "행사 기간에만 체크인할 수 있습니다."),
  CHECKIN_NOT_APPLIED(HttpStatus.BAD_REQUEST, "신청 내역이 없습니다. 먼저 신청해주세요.");

  private final HttpStatus status;
  private final String message;
}
