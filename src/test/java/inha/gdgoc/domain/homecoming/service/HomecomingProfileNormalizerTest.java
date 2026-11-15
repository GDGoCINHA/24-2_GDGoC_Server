package inha.gdgoc.domain.homecoming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.homecoming.enums.AttendType;
import inha.gdgoc.domain.homecoming.enums.HomecomingMemberRole;
import inha.gdgoc.domain.homecoming.enums.HomecomingTeam;
import inha.gdgoc.domain.homecoming.exception.HomecomingErrorCode;
import org.junit.jupiter.api.Test;

class HomecomingProfileNormalizerTest {

  private final HomecomingProfileNormalizer n = new HomecomingProfileNormalizer();

  @Test
  void organizerNeedsTeam() {
    assertThatThrownBy(
            () -> n.normalize(AttendType.MAIN, "a", "01012345678", 2022, 2023, HomecomingMemberRole.ORGANIZER, null, false, null, null))
        .extracting("errorCode")
        .isEqualTo(HomecomingErrorCode.TEAM_REQUIRED);
  }

  @Test
  void yearOrderAndRange() {
    assertThatThrownBy(
            () -> n.normalize(AttendType.MAYBE, "a", "01012345678", 2024, 2022, HomecomingMemberRole.MEMBER, null, null, null, null))
        .extracting("errorCode")
        .isEqualTo(HomecomingErrorCode.YEAR_ORDER_INVALID);
    assertThatThrownBy(
            () -> n.normalize(AttendType.MAYBE, "a", "01012345678", 2020, 2022, HomecomingMemberRole.MEMBER, null, null, null, null))
        .extracting("errorCode")
        .isEqualTo(HomecomingErrorCode.YEAR_OUT_OF_RANGE);
  }

  @Test
  void lateNeedsListedTime() {
    assertThatThrownBy(
            () -> n.normalize(AttendType.BOTH, "a", "01012345678", 2022, 2022, HomecomingMemberRole.MEMBER, null, true, "14:30", null))
        .extracting("errorCode")
        .isEqualTo(HomecomingErrorCode.ARRIVAL_TIME_INVALID);
  }

  @Test
  void memberDropsTeamAndBlankMessage() {
    HomecomingProfile p =
        n.normalize(AttendType.MAIN, "a", "010-1234-5678", 2022, 2022, HomecomingMemberRole.MEMBER, HomecomingTeam.HR, false, "16:00", "   ");
    assertThat(p.team()).isNull();
    assertThat(p.arrivalTime()).as("늦참이 아니면 도착 시간은 버린다").isNull();
    assertThat(p.message()).isNull();
  }

  @Test
  void blankNameRejected() {
    assertThatThrownBy(() -> n.normalize(AttendType.NO, "   ", "01012345678", null, null, null, null, null, null, null))
        .extracting("errorCode")
        .isEqualTo(HomecomingErrorCode.NAME_REQUIRED);
  }
}
