package inha.gdgoc.domain.eventapplication.dto.response;

import java.time.Instant;

/**
 * 체크인 결과.
 *
 * <p>이미 처리된 경우를 오류로 다루지 않는다. 두 번 찍는 것은 흔한 일이고, 빨간 화면을 보여줄 이유가 없다.
 */
public record CheckinResponse(boolean alreadyCheckedIn, Instant checkedInAt, String eventTitle) {}
