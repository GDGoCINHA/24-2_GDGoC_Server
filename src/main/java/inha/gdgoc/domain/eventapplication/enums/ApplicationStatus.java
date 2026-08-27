package inha.gdgoc.domain.eventapplication.enums;

/**
 * 신청 상태.
 *
 * <p>취소해도 행을 지우지 않는다. {@code UNIQUE(form_id, user_id)} 로 중복 신청을 막는데, 취소를 삭제로 처리하면 재신청 때 이 제약과 충돌하기
 * 때문이다. 재신청은 같은 행을 되살린다.
 */
public enum ApplicationStatus {
  APPLIED,
  CANCELED
}
