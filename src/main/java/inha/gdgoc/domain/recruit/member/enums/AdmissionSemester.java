package inha.gdgoc.domain.recruit.member.enums;

/**
 * 지원 시점의 학기. {@link inha.gdgoc.global.util.SemesterCalculator} 가 현재 날짜로 계산해
 * 이 enum 에서 상수를 찾는다.
 *
 * <p><b>상수가 없으면 지원서 제출과 메모 발송이 런타임에 실패한다.</b> 실제로 Y26_2 가 빠져 있어
 * 2026-08 부터 그 상태였다. 미래 학기를 미리 채워 두는 이유다 — 해가 바뀌기 전에 이어서 추가한다.
 */
public enum AdmissionSemester {
    Y21_2, Y22_1, Y22_2, Y23_1, Y23_2, Y24_1, Y24_2, Y25_1, Y25_2, Y26_1,
    Y26_2, Y27_1, Y27_2, Y28_1, Y28_2, Y29_1, Y29_2
}
