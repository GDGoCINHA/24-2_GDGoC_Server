package inha.gdgoc.domain.eventapplication.dto.request;

import inha.gdgoc.domain.user.enums.UserRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import java.time.Instant;

/**
 * 신청 폼의 설정. 생성과 수정에 함께 쓴다.
 *
 * <p>capacity 가 null 이면 정원 무제한이다. 수정에서 정원을 다시 무제한으로 되돌리려면 {@code clearCapacity} 를 true 로 보낸다 — null
 * 만으로는 "안 바꿈"과 구분되지 않기 때문이다.
 *
 * <p>allowAnonymous 도 null 이면 안 바꾼다. 이 값을 모르는 옛 화면이 저장해도 켜 둔 설정이 꺼지지 않게 하려는 것이다.
 */
public record EventFormSaveRequest(
    Instant opensAt,
    Instant closesAt,
    @Min(1) Integer capacity,
    boolean clearCapacity,
    UserRole minRole,
    Boolean isOpen,
    Boolean allowAnonymous) {

  @AssertTrue(message = "신청 시작이 마감보다 늦을 수 없습니다.")
  private boolean isPeriodValid() {
    return opensAt == null || closesAt == null || opensAt.isBefore(closesAt);
  }
}
