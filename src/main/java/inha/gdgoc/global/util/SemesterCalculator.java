package inha.gdgoc.global.util;

import inha.gdgoc.domain.recruit.enums.AdmissionSemester;

import java.time.LocalDate;
import java.time.ZoneId;

public final class SemesterCalculator {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private SemesterCalculator() {}

    public static AdmissionSemester currentSemester() {
        return of(LocalDate.now(KST));
    }

    public static AdmissionSemester of(LocalDate date) {
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
