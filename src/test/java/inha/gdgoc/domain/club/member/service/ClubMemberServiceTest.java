package inha.gdgoc.domain.club.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.enums.ClubRecruitStatus;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.dto.response.ClubMemberAddResponse;
import inha.gdgoc.domain.club.member.dto.response.ClubMemberCandidateResponse;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
import inha.gdgoc.domain.club.member.repository.ClubMemberRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClubMemberServiceTest {

  private static final Long CLUB_ID = 1L;
  private static final Long LEADER_ID = 10L;
  private static final Long USER_ID = 20L;

  @Mock private ClubAccessService clubAccessService;
  @Mock private ClubMemberRepository clubMemberRepository;
  @Mock private UserRepository userRepository;

  private ClubMemberService service;
  private User leader;
  private Club club;

  @BeforeEach
  void setUp() {
    service = new ClubMemberService(clubAccessService, clubMemberRepository, userRepository);
    leader = user(LEADER_ID);
    club = BeanUtils.instantiateClass(Club.class);
    ReflectionTestUtils.setField(club, "id", CLUB_ID);
    ReflectionTestUtils.setField(club, "leader", leader);
    ReflectionTestUtils.setField(club, "status", ClubStatus.ACTIVE);
    ReflectionTestUtils.setField(club, "recruitStatus", ClubRecruitStatus.RECRUITING);
  }

  private static User user(Long id) {
    User user = User.builder().build();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private ClubMember member(Long memberId, User user, boolean active) {
    ClubMember m = ClubMember.apply(club, user, null, Instant.now());
    if (active) {
      m.approve(Instant.now());
    }
    ReflectionTestUtils.setField(m, "id", memberId);
    return m;
  }

  private static void assertError(Runnable call, ClubErrorCode code) {
    assertThatThrownBy(call::run)
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(code);
  }

  @Test
  @DisplayName("모집 마감이면 신청할 수 없다")
  void cannotApplyWhenClosed() {
    ReflectionTestUtils.setField(club, "recruitStatus", ClubRecruitStatus.CLOSED);
    given(clubAccessService.getClub(CLUB_ID)).willReturn(club);
    assertError(() -> service.apply(CLUB_ID, USER_ID, null), ClubErrorCode.NOT_RECRUITING);
  }

  @Test
  @DisplayName("신청 중이거나 참여 중이면 다시 신청할 수 없다")
  void cannotApplyTwice() {
    given(clubAccessService.getClub(CLUB_ID)).willReturn(club);
    given(clubMemberRepository.existsByClubIdAndUserIdAndStatusIn(any(), any(), anyCollection()))
        .willReturn(true);
    assertError(() -> service.apply(CLUB_ID, USER_ID, null), ClubErrorCode.ALREADY_APPLIED);
    verify(clubMemberRepository, never()).save(any());
  }

  @Test
  @DisplayName("리더는 넘기기 전에 탈퇴할 수 없다")
  void leaderCannotLeave() {
    given(clubAccessService.getClub(CLUB_ID)).willReturn(club);
    given(clubMemberRepository.findFirstByClubIdAndUserIdAndStatusIn(any(), any(), anyCollection()))
        .willReturn(Optional.of(member(1L, leader, true)));
    assertError(() -> service.leave(CLUB_ID, LEADER_ID), ClubErrorCode.LEADER_CANNOT_LEAVE);
  }

  @Test
  @DisplayName("신청 중에 나가면 취소로 남는다 — leftAt 이 찍히지 않는다")
  void pendingLeaveIsCancel() {
    ClubMember pending = member(2L, user(USER_ID), false);
    given(clubAccessService.getClub(CLUB_ID)).willReturn(club);
    given(clubMemberRepository.findFirstByClubIdAndUserIdAndStatusIn(any(), any(), anyCollection()))
        .willReturn(Optional.of(pending));
    service.leave(CLUB_ID, USER_ID);
    assertThat(pending.getStatus()).isEqualTo(ClubMemberStatus.CANCELED);
    assertThat(pending.getLeftAt()).isNull();
  }

  @Test
  @DisplayName("리더는 자기 자신을 내보낼 수 없다")
  void cannotKickSelf() {
    given(clubMemberRepository.findById(1L)).willReturn(Optional.of(member(1L, leader, true)));
    assertError(() -> service.kick(CLUB_ID, 1L, LEADER_ID), ClubErrorCode.CANNOT_KICK_SELF);
  }

  @Test
  @DisplayName("정원을 넘어도 승인한다 — 막지 않고 경고만 한다")
  void approveIgnoresCapacity() {
    ReflectionTestUtils.setField(club, "capacity", 1);
    ClubMember pending = member(2L, user(USER_ID), false);
    given(clubMemberRepository.findById(2L)).willReturn(Optional.of(pending));
    service.approve(CLUB_ID, 2L, LEADER_ID);
    assertThat(pending.getStatus()).isEqualTo(ClubMemberStatus.ACTIVE);
  }

  @Test
  @DisplayName("다른 소모임의 멤버 id 로는 승인할 수 없다")
  void cannotTouchOtherClubMember() {
    Club other = BeanUtils.instantiateClass(Club.class);
    ReflectionTestUtils.setField(other, "id", 99L);
    ClubMember foreign = ClubMember.apply(other, user(USER_ID), null, Instant.now());
    given(clubMemberRepository.findById(2L)).willReturn(Optional.of(foreign));
    assertError(() -> service.approve(CLUB_ID, 2L, LEADER_ID), ClubErrorCode.MEMBER_NOT_FOUND);
  }

  @Test
  @DisplayName("이미 처리된 신청은 다시 승인할 수 없다")
  void cannotApproveTwice() {
    given(clubMemberRepository.findById(1L)).willReturn(Optional.of(member(1L, user(USER_ID), true)));
    assertError(() -> service.approve(CLUB_ID, 1L, LEADER_ID), ClubErrorCode.INVALID_MEMBER_STATE);
  }

  private static User userWithRole(Long id, UserRole role) {
    User user = User.builder().userRole(role).studentId("12241234").build();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  @Test
  @DisplayName("일괄 추가 — 새 사람은 바로 ACTIVE, 신청 중이면 승인, 이미 멤버면 건너뛴다. 모집 마감이어도 넣는다")
  void addMembersHandlesEachState() {
    ReflectionTestUtils.setField(club, "recruitStatus", ClubRecruitStatus.CLOSED);
    User fresh = userWithRole(21L, UserRole.MEMBER);
    User applicant = userWithRole(22L, UserRole.MEMBER);
    User already = userWithRole(23L, UserRole.CORE);
    ClubMember pending = member(2L, applicant, false);
    ClubMember active = member(3L, already, true);
    given(clubAccessService.requireLeaderOrStaff(CLUB_ID, LEADER_ID, false)).willReturn(club);
    given(userRepository.findAllById(List.of(21L, 22L, 23L)))
        .willReturn(List.of(fresh, applicant, already));
    given(clubMemberRepository.findAllByClubIdAndUserIdInAndStatusIn(any(), any(), anyCollection()))
        .willReturn(List.of(pending, active));
    ArgumentCaptor<ClubMember> saved = ArgumentCaptor.forClass(ClubMember.class);

    ClubMemberAddResponse result =
        service.addMembers(CLUB_ID, LEADER_ID, false, List.of(21L, 22L, 23L, 21L));

    assertThat(result).isEqualTo(new ClubMemberAddResponse(1, 1, 1));
    verify(clubMemberRepository).save(saved.capture());
    assertThat(saved.getValue().getUser()).isSameAs(fresh);
    assertThat(saved.getValue().getStatus()).isEqualTo(ClubMemberStatus.ACTIVE);
    assertThat(pending.getStatus()).isEqualTo(ClubMemberStatus.ACTIVE);
  }

  @Test
  @DisplayName("일괄 추가 — 부원이 아닌(GUEST) 사람이 섞여 있으면 아무도 넣지 않는다")
  void addMembersRejectsGuest() {
    given(clubAccessService.requireLeaderOrStaff(CLUB_ID, LEADER_ID, false)).willReturn(club);
    given(userRepository.findAllById(List.of(21L, 22L)))
        .willReturn(
            List.of(userWithRole(21L, UserRole.MEMBER), userWithRole(22L, UserRole.GUEST)));
    assertError(
        () -> service.addMembers(CLUB_ID, LEADER_ID, false, List.of(21L, 22L)),
        ClubErrorCode.TARGET_NOT_MEMBER_ROLE);
    verify(clubMemberRepository, never()).save(any());
  }

  @Test
  @DisplayName("일괄 추가 — 없는 사용자가 섞여 있으면 아무도 넣지 않는다")
  void addMembersRejectsUnknownUser() {
    given(clubAccessService.requireLeaderOrStaff(CLUB_ID, LEADER_ID, false)).willReturn(club);
    given(userRepository.findAllById(List.of(21L, 99L)))
        .willReturn(List.of(userWithRole(21L, UserRole.MEMBER)));
    assertError(
        () -> service.addMembers(CLUB_ID, LEADER_ID, false, List.of(21L, 99L)),
        ClubErrorCode.USER_NOT_FOUND);
    verify(clubMemberRepository, never()).save(any());
  }

  @Test
  @DisplayName("일괄 추가 — 리더도 운영진도 아니면 막힌다")
  void addMembersRequiresLeaderOrStaff() {
    given(clubAccessService.requireLeaderOrStaff(CLUB_ID, USER_ID, false))
        .willThrow(new BusinessException(ClubErrorCode.NOT_CLUB_LEADER));
    assertError(
        () -> service.addMembers(CLUB_ID, USER_ID, false, List.of(21L)),
        ClubErrorCode.NOT_CLUB_LEADER);
    verify(userRepository, never()).findAllById(any());
  }

  @Test
  @DisplayName("멤버 검색 — 검색어가 비면 아무도 내주지 않는다")
  void candidatesNeedQuery() {
    given(clubAccessService.requireLeaderOrStaff(CLUB_ID, LEADER_ID, false)).willReturn(club);
    assertThat(service.findCandidates(CLUB_ID, LEADER_ID, false, "  ")).isEmpty();
    verify(userRepository, never()).searchByName(any(), anyCollection(), any());
  }

  @Test
  @DisplayName("멤버 검색 — 학번은 앞 4자리만 주고, 이 소모임에서의 상태를 붙인다")
  void candidatesMaskStudentId() {
    User applicant = userWithRole(22L, UserRole.MEMBER);
    given(clubAccessService.requireLeaderOrStaff(CLUB_ID, LEADER_ID, false)).willReturn(club);
    given(userRepository.searchByName(eq("민지"), anyCollection(), any()))
        .willReturn(List.of(userWithRole(21L, UserRole.MEMBER), applicant));
    given(clubMemberRepository.findAllByClubIdAndUserIdInAndStatusIn(any(), any(), anyCollection()))
        .willReturn(List.of(member(2L, applicant, false)));

    List<ClubMemberCandidateResponse> result =
        service.findCandidates(CLUB_ID, LEADER_ID, false, " 민지 ");

    assertThat(result).extracting(ClubMemberCandidateResponse::studentId).containsOnly("1224****");
    assertThat(result)
        .extracting(ClubMemberCandidateResponse::memberStatus)
        .containsExactly(null, ClubMemberStatus.PENDING);
  }

  @Test
  @DisplayName("멤버가 아닌 사람에게는 리더를 넘길 수 없다")
  void cannotHandOverToOutsider() {
    given(clubAccessService.requireLeader(CLUB_ID, LEADER_ID)).willReturn(club);
    given(clubAccessService.isActiveMember(CLUB_ID, USER_ID)).willReturn(false);
    assertError(
        () -> service.changeLeader(CLUB_ID, LEADER_ID, USER_ID), ClubErrorCode.TARGET_NOT_ACTIVE_MEMBER);
  }
}
