package inha.gdgoc.domain.club.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.activity.exception.ClubActivityErrorCode;
import inha.gdgoc.domain.club.activity.service.ClubActivityQueryService;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.review.dto.response.ClubReviewItemResponse;
import inha.gdgoc.domain.club.review.repository.ClubReviewQueryRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** 인증 검토. 필요 인원 미리 계산, 재제출 표시, 인증 완료 잠금(409)을 고정한다. */
@ExtendWith(MockitoExtension.class)
class ClubReviewServiceTest {

  private static final Long ACTIVITY_ID = 5L;
  private static final Long REVIEWER_ID = 99L;

  @Mock private ClubReviewQueryRepository clubReviewQueryRepository;
  @Mock private ClubActivityQueryService clubActivityQueryService;

  private ClubReviewService service;
  private Club club;

  @BeforeEach
  void setUp() {
    service = new ClubReviewService(clubReviewQueryRepository, clubActivityQueryService);
    club = BeanUtils.instantiateClass(Club.class);
    ReflectionTestUtils.setField(club, "id", 1L);
    ReflectionTestUtils.setField(club, "name", "알고리즘");
    ReflectionTestUtils.setField(club, "term", ClubTerm.create("2026-2", new BigDecimal("0.50")));
  }

  private ClubActivity activity() {
    ClubActivity a =
        ClubActivity.create(club, null, LocalDate.of(2026, 10, 7), "내용", null, 10L, Instant.now());
    ReflectionTestUtils.setField(a, "id", ACTIVITY_ID);
    return a;
  }

  private static User user(long id, String name) {
    User u = User.builder().name(name).build();
    ReflectionTestUtils.setField(u, "id", id);
    return u;
  }

  @Test
  @DisplayName("명단 5명 중 3명 출석이면 필요 3명 충족, 사유가 남은 PENDING 은 재제출로 표시")
  void queueItemIsPrecomputed() {
    ClubActivity a = activity();
    a.requestRevision(REVIEWER_ID, "사진 부족", Instant.now());
    a.update(null, a.getActivityDate(), "고친 내용", null, Instant.now());
    given(clubReviewQueryRepository.findForReview(ClubActivityStatus.PENDING, null))
        .willReturn(List.of(a));
    given(clubReviewQueryRepository.findPhotos(anyCollection())).willReturn(List.of());
    given(clubReviewQueryRepository.findRosters(anyCollection()))
        .willReturn(
            List.of(
                ClubActivityAttendance.of(a, user(1, "가"), true),
                ClubActivityAttendance.of(a, user(2, "나"), true),
                ClubActivityAttendance.of(a, user(3, "다"), true),
                ClubActivityAttendance.of(a, user(4, "라"), false),
                ClubActivityAttendance.of(a, user(5, "마"), false)));

    ClubReviewItemResponse item = service.findQueue(null, null).get(0);

    assertThat(item.rosterCount()).isEqualTo(5);
    assertThat(item.attendedCount()).isEqualTo(3);
    assertThat(item.required()).isEqualTo(3);
    assertThat(item.requiredSatisfied()).isTrue();
    assertThat(item.resubmitted()).isTrue();
    assertThat(item.revisionReason()).isEqualTo("사진 부족");
    assertThat(item.roster()).extracting(ClubReviewItemResponse.Attendee::name)
        .containsExactly("가", "나", "다", "라", "마");
  }

  @Test
  @DisplayName("명단이 없는 기록은 필요 인원 미충족으로 보인다")
  void emptyRosterIsNotSatisfied() {
    given(clubReviewQueryRepository.findForReview(ClubActivityStatus.PENDING, 1L))
        .willReturn(List.of(activity()));
    given(clubReviewQueryRepository.findPhotos(anyCollection())).willReturn(List.of());
    given(clubReviewQueryRepository.findRosters(anyCollection())).willReturn(List.of());

    ClubReviewItemResponse item = service.findQueue(ClubActivityStatus.PENDING, 1L).get(0);

    assertThat(item.requiredSatisfied()).isFalse();
    assertThat(item.resubmitted()).isFalse();
  }

  @Test
  @DisplayName("잠가서 읽은 기록을 인증 완료한다")
  void approveUsesLockedRead() {
    ClubActivity a = activity();
    given(clubActivityQueryService.getForUpdate(ACTIVITY_ID)).willReturn(a);

    service.approve(ACTIVITY_ID, REVIEWER_ID);

    assertThat(a.getStatus()).isEqualTo(ClubActivityStatus.APPROVED);
    assertThat(a.getReviewedBy()).isEqualTo(REVIEWER_ID);
  }

  @Test
  @DisplayName("인증 완료된 기록에 보완 요청하면 409")
  void approvedIsLocked() {
    ClubActivity a = activity();
    a.approve(REVIEWER_ID, Instant.now());
    given(clubActivityQueryService.getForUpdate(ACTIVITY_ID)).willReturn(a);

    assertThatThrownBy(() -> service.requestRevision(ACTIVITY_ID, REVIEWER_ID, "다시"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ClubActivityErrorCode.ACTIVITY_LOCKED);
  }

  @Test
  @DisplayName("보완 사유는 앞뒤 공백을 지워 저장한다")
  void reasonIsStripped() {
    ClubActivity a = activity();
    given(clubActivityQueryService.getForUpdate(ACTIVITY_ID)).willReturn(a);

    service.requestRevision(ACTIVITY_ID, REVIEWER_ID, "  사진을 더 올려 주세요  ");

    assertThat(a.getStatus()).isEqualTo(ClubActivityStatus.REVISION_REQUESTED);
    assertThat(a.getRevisionReason()).isEqualTo("사진을 더 올려 주세요");
  }
}
