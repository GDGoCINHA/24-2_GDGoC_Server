package inha.gdgoc.domain.recruit.core.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * 운영진 리크루팅 회차(예: 2026-1)를 현재 날짜 기준으로 계산한다.
 * 1~6월은 1학기, 7~12월은 2학기로 본다.
 */
@Component
public class RecruitCoreSessionResolver {

    private final Clock clock;

    public RecruitCoreSessionResolver() {
        this(Clock.system(ZoneId.of("Asia/Seoul")));
    }

    public RecruitCoreSessionResolver(Clock clock) {
        this.clock = clock;
    }

    public String currentSession() {
        LocalDate today = LocalDate.now(clock);
        int semester = (today.getMonthValue() <= 6) ? 1 : 2;
        return today.getYear() + "-" + semester;
    }
}
