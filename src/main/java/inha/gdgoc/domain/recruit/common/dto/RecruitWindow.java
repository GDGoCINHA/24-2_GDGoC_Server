package inha.gdgoc.domain.recruit.common.dto;

import java.time.Instant;

/** 모집이 열려 있는 구간. 설정에서 오든 DB 에서 오든 이 모양으로 다룬다. */
public record RecruitWindow(Instant openAt, Instant closeAt) {

    public RecruitWindow {
        if (!openAt.isBefore(closeAt)) {
            throw new IllegalArgumentException(
                "open-at 은 close-at 보다 앞서야 한다: openAt=" + openAt + ", closeAt=" + closeAt);
        }
    }
}
