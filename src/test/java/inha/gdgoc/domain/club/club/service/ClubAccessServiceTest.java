package inha.gdgoc.domain.club.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.repository.ClubRepository;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.leader.repository.ClubLeaderGrantRepository;
import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
import inha.gdgoc.domain.club.member.repository.ClubMemberRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClubAccessServiceTest {

  private static final Long CLUB_ID = 1L;
  private static final Long LEADER_ID = 10L;
  private static final Long OTHER_ID = 20L;

  @Mock private ClubRepository clubRepository;
  @Mock private ClubMemberRepository clubMemberRepository;
  @Mock private ClubLeaderGrantRepository clubLeaderGrantRepository;

  private ClubAccessService service;

  @BeforeEach
  void setUp() {
    service = new ClubAccessService(clubRepository, clubMemberRepository, clubLeaderGrantRepository);
  }

  private void givenClub() {
    User leader = User.builder().build();
    ReflectionTestUtils.setField(leader, "id", LEADER_ID);
    Club club = BeanUtils.instantiateClass(Club.class);
    ReflectionTestUtils.setField(club, "leader", leader);
    given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
  }

  private static void assertError(Runnable call, ClubErrorCode code) {
    assertThatThrownBy(call::run)
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(code);
  }

  @Test
  @DisplayName("소모임이 없으면 404 가 먼저 난다")
  void missingClubIsNotFound() {
    given(clubRepository.findById(CLUB_ID)).willReturn(Optional.empty());
    assertError(() -> service.requireLeader(CLUB_ID, LEADER_ID), ClubErrorCode.CLUB_NOT_FOUND);
  }

  @Test
  @DisplayName("이끔이는 club.leader_id 로 판정한다")
  void leaderPasses() {
    givenClub();
    assertThat(service.requireLeader(CLUB_ID, LEADER_ID)).isNotNull();
  }

  @Test
  @DisplayName("이끔이가 아니면 403 — 운영진 역할이어도 마찬가지다")
  void nonLeaderIsForbidden() {
    givenClub();
    assertError(() -> service.requireLeader(CLUB_ID, OTHER_ID), ClubErrorCode.NOT_CLUB_LEADER);
  }

  @Test
  @DisplayName("ACTIVE 가 아니면 멤버가 아니다")
  void nonActiveMemberIsForbidden() {
    givenClub();
    given(clubMemberRepository.existsByClubIdAndUserIdAndStatus(CLUB_ID, OTHER_ID, ClubMemberStatus.ACTIVE))
        .willReturn(false);
    assertError(() -> service.requireActiveMember(CLUB_ID, OTHER_ID), ClubErrorCode.NOT_CLUB_MEMBER);
  }

  @Test
  @DisplayName("회수된 이끔이 권한으로는 개설할 수 없다")
  void revokedGrantCannotOpen() {
    given(clubLeaderGrantRepository.existsByUserIdAndRevokedAtIsNull(OTHER_ID)).willReturn(false);
    assertError(() -> service.requireLeaderGrant(OTHER_ID), ClubErrorCode.LEADER_GRANT_REQUIRED);
  }
}
