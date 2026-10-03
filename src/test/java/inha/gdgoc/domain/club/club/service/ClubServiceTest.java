package inha.gdgoc.domain.club.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import inha.gdgoc.domain.club.club.dto.request.ClubCreateRequest;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubRecruitStatus;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.club.repository.ClubRepository;
import inha.gdgoc.domain.club.club.repository.ClubTermRepository;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.member.repository.ClubMemberRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/** 단톡방 링크는 ACTIVE 멤버와 운영진에게만 — 가시성 회귀 방지. */
@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

  private static final Long CLUB_ID = 1L;
  private static final Long LEADER_ID = 10L;
  private static final Long USER_ID = 20L;
  private static final String LINK = "https://open.kakao.com/o/secret";

  @Mock private ClubAccessService clubAccessService;
  @Mock private ClubRepository clubRepository;
  @Mock private ClubTermRepository clubTermRepository;
  @Mock private ClubMemberRepository clubMemberRepository;
  @Mock private UserRepository userRepository;

  private ClubService service;
  private Club club;

  @BeforeEach
  void setUp() {
    service =
        new ClubService(
            clubAccessService, clubRepository, clubTermRepository, clubMemberRepository, userRepository);
    User leader = User.builder().build();
    ReflectionTestUtils.setField(leader, "id", LEADER_ID);
    club = BeanUtils.instantiateClass(Club.class);
    ReflectionTestUtils.setField(club, "id", CLUB_ID);
    ReflectionTestUtils.setField(club, "leader", leader);
    ReflectionTestUtils.setField(club, "term", BeanUtils.instantiateClass(ClubTerm.class));
    ReflectionTestUtils.setField(club, "status", ClubStatus.ACTIVE);
    ReflectionTestUtils.setField(club, "kakaoLink", LINK);
    lenient().when(clubAccessService.getClub(CLUB_ID)).thenReturn(club);
  }

  private void givenMembership(ClubMember m) {
    given(clubMemberRepository.findFirstByClubIdAndUserIdAndStatusIn(any(), any(), anyCollection()))
        .willReturn(Optional.ofNullable(m));
  }

  private ClubMember membership(boolean active) {
    User user = User.builder().build();
    ReflectionTestUtils.setField(user, "id", USER_ID);
    ClubMember m = ClubMember.apply(club, user, null, Instant.now());
    if (active) {
      m.approve(Instant.now());
    }
    return m;
  }

  @Test
  @DisplayName("멤버가 아니면 단톡방 링크를 주지 않는다")
  void outsiderGetsNoLink() {
    givenMembership(null);
    assertThat(service.getDetail(CLUB_ID, USER_ID, false).kakaoLink()).isNull();
  }

  @Test
  @DisplayName("수정 화면 저장은 비운 칸을 비운다 — 모집 상태는 그대로")
  void replaceClearsBlankFields() {
    ReflectionTestUtils.setField(club, "description", "소개");
    ReflectionTestUtils.setField(club, "capacity", 6);
    ReflectionTestUtils.setField(club, "recruitStatus", ClubRecruitStatus.CLOSED);
    given(clubAccessService.requireLeader(CLUB_ID, LEADER_ID)).willReturn(club);

    service.replace(
        CLUB_ID,
        LEADER_ID,
        new ClubCreateRequest(
            "이름", ClubCategory.ETC, "한 줄", null, null, null, null, null, null, null, null));

    assertThat(club.getKakaoLink()).isNull();
    assertThat(club.getDescription()).isNull();
    assertThat(club.getCapacity()).isNull();
    assertThat(club.getRecruitStatus()).isEqualTo(ClubRecruitStatus.CLOSED);
  }

  @Test
  @DisplayName("비로그인은 링크도 신청 상태도 받지 않는다")
  void anonymousGetsNoLink() {
    var detail = service.getDetail(CLUB_ID, null, false);
    assertThat(detail.kakaoLink()).isNull();
    assertThat(detail.myMembership()).isNull();
  }

  @Test
  @DisplayName("비로그인에게 숨긴 소모임은 없는 것처럼 보인다")
  void hiddenClubIsNotFoundForAnonymous() {
    ReflectionTestUtils.setField(club, "status", ClubStatus.HIDDEN);
    assertThatThrownBy(() -> service.getDetail(CLUB_ID, null, false))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ClubErrorCode.CLUB_NOT_FOUND);
  }

  @Test
  @DisplayName("신청 중(PENDING)이어도 단톡방 링크를 주지 않는다")
  void pendingGetsNoLink() {
    givenMembership(membership(false));
    assertThat(service.getDetail(CLUB_ID, USER_ID, false).kakaoLink()).isNull();
  }

  @Test
  @DisplayName("ACTIVE 멤버는 링크를 받는다")
  void activeGetsLink() {
    givenMembership(membership(true));
    assertThat(service.getDetail(CLUB_ID, USER_ID, false).kakaoLink()).isEqualTo(LINK);
  }

  @Test
  @DisplayName("운영진은 멤버가 아니어도 링크를 받는다")
  void staffGetsLink() {
    givenMembership(null);
    assertThat(service.getDetail(CLUB_ID, USER_ID, true).kakaoLink()).isEqualTo(LINK);
  }

  @Test
  @DisplayName("숨긴 소모임은 관계없는 사람에게 없는 것처럼 보인다")
  void hiddenClubIsNotFoundForOutsider() {
    ReflectionTestUtils.setField(club, "status", ClubStatus.HIDDEN);
    givenMembership(null);
    assertThatThrownBy(() -> service.getDetail(CLUB_ID, USER_ID, false))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ClubErrorCode.CLUB_NOT_FOUND);
  }

  @Test
  @DisplayName("개설하면 리더 권한 없이도 승인 대기로 생기고, 개설자는 리더 멤버가 된다")
  void createStartsPending() {
    User leader = club.getLeader();
    given(userRepository.findById(LEADER_ID)).willReturn(Optional.of(leader));
    given(clubTermRepository.findTopByOrderByIdDesc())
        .willReturn(Optional.of(BeanUtils.instantiateClass(ClubTerm.class)));
    given(clubRepository.save(any(Club.class))).willAnswer(inv -> inv.getArgument(0));
    ArgumentCaptor<Club> saved = ArgumentCaptor.forClass(Club.class);

    service.create(
        LEADER_ID,
        new ClubCreateRequest(
            "이름", ClubCategory.ETC, "한 줄", null, null, null, null, null, null, null, null));

    verify(clubRepository).save(saved.capture());
    assertThat(saved.getValue().getStatus()).isEqualTo(ClubStatus.PENDING);
    verify(clubMemberRepository).save(any(ClubMember.class));
  }

  @Test
  @DisplayName("승인 대기 소모임은 관계없는 사람에게 없는 것처럼 보인다")
  void pendingClubIsNotFoundForOutsider() {
    ReflectionTestUtils.setField(club, "status", ClubStatus.PENDING);
    givenMembership(null);
    assertThatThrownBy(() -> service.getDetail(CLUB_ID, USER_ID, false))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ClubErrorCode.CLUB_NOT_FOUND);
  }

  @Test
  @DisplayName("일반 목록은 공개(ACTIVE·ENDED) 소모임만 찾는다 — 상태 필터를 줘도 무시한다")
  void searchShowsOnlyPublicToNonStaff() {
    given(clubRepository.search(anyCollection(), any(), any(), any(), any()))
        .willReturn(Page.empty());

    service.search(null, null, ClubStatus.PENDING, null, false, Pageable.unpaged());

    verify(clubRepository).search(eq(ClubStatus.PUBLIC), any(), any(), eq(""), any());
  }

  @Test
  @DisplayName("승인하면 공개되고 반려 사유가 지워진다")
  void approvePublishes() {
    ReflectionTestUtils.setField(club, "status", ClubStatus.PENDING);
    ReflectionTestUtils.setField(club, "rejectReason", "이전 사유");

    service.approve(CLUB_ID);

    assertThat(club.getStatus()).isEqualTo(ClubStatus.ACTIVE);
    assertThat(club.getRejectReason()).isNull();
  }

  @Test
  @DisplayName("반려하면 사유가 남는다")
  void rejectKeepsReason() {
    ReflectionTestUtils.setField(club, "status", ClubStatus.PENDING);

    service.reject(CLUB_ID, "소개를 더 써 주세요");

    assertThat(club.getStatus()).isEqualTo(ClubStatus.REJECTED);
    assertThat(club.getRejectReason()).isEqualTo("소개를 더 써 주세요");
  }

  @Test
  @DisplayName("승인 대기가 아니면 승인할 수 없다")
  void approveRequiresPending() {
    assertThatThrownBy(() -> service.approve(CLUB_ID))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ClubErrorCode.CLUB_NOT_PENDING);
  }

  @Test
  @DisplayName("반려된 소모임을 고쳐 저장하면 다시 승인 대기로 올라간다")
  void replaceResubmitsRejected() {
    ReflectionTestUtils.setField(club, "status", ClubStatus.REJECTED);
    given(clubAccessService.requireLeader(CLUB_ID, LEADER_ID)).willReturn(club);

    service.replace(
        CLUB_ID,
        LEADER_ID,
        new ClubCreateRequest(
            "이름", ClubCategory.ETC, "한 줄", null, null, null, null, null, null, null, null));

    assertThat(club.getStatus()).isEqualTo(ClubStatus.PENDING);
  }
}
