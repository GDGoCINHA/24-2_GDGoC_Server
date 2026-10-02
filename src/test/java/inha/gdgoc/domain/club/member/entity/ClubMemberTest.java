package inha.gdgoc.domain.club.member.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 출석 명단의 날짜 경계. 틀리면 참석 비율의 분모가 틀려 완주 판정이 전부 어긋난다. */
class ClubMemberTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final LocalDate DAY = LocalDate.of(2026, 10, 7);

  private static Instant kst(int day, int hour, int minute) {
    return ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, KST).toInstant();
  }

  private static ClubMember joinedAt(Instant joined) {
    ClubMember member = ClubMember.apply(null, null, null, joined.minusSeconds(3600));
    member.approve(joined);
    return member;
  }

  @Test
  @DisplayName("활동일 밤에 합류한 사람은 그날 명단에 든다")
  void joinedLateOnTheDayIsIncluded() {
    assertThat(joinedAt(kst(7, 23, 59)).wasActiveOn(DAY, KST)).isTrue();
  }

  @Test
  @DisplayName("다음 날 0시에 합류한 사람은 그날 명단에 없다")
  void joinedNextMidnightIsExcluded() {
    assertThat(joinedAt(kst(8, 0, 0)).wasActiveOn(DAY, KST)).isFalse();
  }

  @Test
  @DisplayName("활동일에 탈퇴한 사람은 그날 명단에서 빠진다")
  void leftOnTheDayIsExcluded() {
    ClubMember member = joinedAt(kst(1, 12, 0));
    member.leave(kst(7, 10, 0));
    assertThat(member.wasActiveOn(DAY, KST)).isFalse();
  }

  @Test
  @DisplayName("다음 날 0시에 강퇴돼도 활동일 명단에는 남는다")
  void kickedNextMidnightStaysOnTheDay() {
    ClubMember member = joinedAt(kst(1, 12, 0));
    member.kick(kst(8, 0, 0));
    assertThat(member.wasActiveOn(DAY, KST)).isTrue();
  }

  @Test
  @DisplayName("지난 회차 명단은 이후 탈퇴와 무관하다")
  void pastRosterSurvivesLaterLeave() {
    ClubMember member = joinedAt(kst(1, 12, 0));
    member.leave(kst(20, 9, 0));
    assertThat(member.wasActiveOn(DAY, KST)).isTrue();
  }

  @Test
  @DisplayName("승인 전 신청자는 명단에 없다")
  void pendingIsExcluded() {
    ClubMember member = ClubMember.apply(null, null, "같이 해요", kst(1, 12, 0));
    assertThat(member.wasActiveOn(DAY, KST)).isFalse();
  }
}
