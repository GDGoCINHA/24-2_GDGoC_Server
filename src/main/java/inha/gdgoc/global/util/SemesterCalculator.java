package inha.gdgoc.global.util;

import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * 현재 날짜를 기반으로 학기를 계산하는 컴포넌트.
 * env 값 대신 서버 시간이 기준이 되도록 고정.
 */
@Component
public class SemesterCalculator {

    private final Clock clock;

    public SemesterCalculator() {
        this(Clock.system(ZoneId.of("Asia/Seoul")));
    }

    public SemesterCalculator(Clock clock) {
        this.clock = clock;
    }

    public AdmissionSemester currentSemester() {
        return of(LocalDate.now(clock));
    }

    public AdmissionSemester of(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        int yy;
        int term;

        if (month == 1) {
            yy = (year - 1) % 100;
            term = 2;
        } else if (month <= 7) {
            yy = year % 100;
            term = 1;
        } else {
            yy = year % 100;
            term = 2;
        }

        String enumName = String.format("Y%02d_%d", yy, term);
        try {
            return AdmissionSemester.valueOf(enumName);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "AdmissionSemester enum에 상수 " + enumName + " 이(가) 정의되어 있지 않습니다. " +
                            "해당 연도/학기 상수를 추가하세요.", ex
            );
        }
    }
}
