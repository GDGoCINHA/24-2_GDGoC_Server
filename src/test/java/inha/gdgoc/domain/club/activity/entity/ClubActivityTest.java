package inha.gdgoc.domain.club.activity.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.activity.exception.ClubActivityErrorCode;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.time.LocalDate;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 인증 상태 전이. 인증 완료(APPROVED) 잠금이 풀리면 완주 실적이 사후에 바뀐다. */
class ClubActivityTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 7);
  private static final Instant SUBMITTED = Instant.parse("2026-10-07T12:00:00Z");
  private static final Instant REVIEWED = Instant.parse("2026-10-08T03:00:00Z");
  private static final Instant RESUBMITTED = Instant.parse("2026-10-09T09:00:00Z");
  private static final Long LEADER_ID = 1L;
  private static final Long REVIEWER_ID = 99L;

  private static ClubActivity submitted() {
    return ClubActivity.create(null, null, DAY, "1주차 모임", null, LEADER_ID, SUBMITTED);
  }

  private static ClubActivity approved() {
    ClubActivity activity = submitted();
    activity.approve(REVIEWER_ID, REVIEWED);
    return activity;
  }

  private static void assertLocked(ThrowingCallable call) {
    assertThatThrownBy(call)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ClubActivityErrorCode.ACTIVITY_LOCKED);
  }

  @Test
  @DisplayName("제출하면 확인 중이다")
  void createIsPending() {
    ClubActivity activity = submitted();

    assertThat(activity.getStatus()).isEqualTo(ClubActivityStatus.PENDING);
    assertThat(activity.getSubmittedAt()).isEqualTo(SUBMITTED);
    assertThat(activity.getCreatedBy()).isEqualTo(LEADER_ID);
  }

  @Test
  @DisplayName("인증 완료하면 검토자와 시각이 남는다")
  void approveRecordsReviewer() {
    ClubActivity activity = approved();

    assertThat(activity.getStatus()).isEqualTo(ClubActivityStatus.APPROVED);
    assertThat(activity.getReviewedBy()).isEqualTo(REVIEWER_ID);
    assertThat(activity.getReviewedAt()).isEqualTo(REVIEWED);
  }

  @Test
  @DisplayName("보완 요청하면 사유가 남는다")
  void requestRevisionRecordsReason() {
    ClubActivity activity = submitted();

    activity.requestRevision(REVIEWER_ID, "사진이 흐려요", REVIEWED);

    assertThat(activity.getStatus()).isEqualTo(ClubActivityStatus.REVISION_REQUESTED);
    assertThat(activity.getRevisionReason()).isEqualTo("사진이 흐려요");
    assertThat(activity.getReviewedBy()).isEqualTo(REVIEWER_ID);
  }

  @Test
  @DisplayName("보완 요청된 기록을 고치면 확인 중으로 돌아가고, 사유는 재검토용으로 남는다")
  void updateAfterRevisionGoesBackToPending() {
    ClubActivity activity = submitted();
    activity.requestRevision(REVIEWER_ID, "사진이 흐려요", REVIEWED);

    activity.update(null, DAY.plusDays(1), "1주차 모임 (사진 교체)", "1장 완료", RESUBMITTED);

    assertThat(activity.getStatus()).isEqualTo(ClubActivityStatus.PENDING);
    assertThat(activity.getSubmittedAt()).isEqualTo(RESUBMITTED);
    assertThat(activity.getActivityDate()).isEqualTo(DAY.plusDays(1));
    assertThat(activity.getRevisionReason()).isEqualTo("사진이 흐려요");
  }

  @Test
  @DisplayName("확인 중인 기록을 고쳐도 확인 중이다")
  void updatePendingStaysPending() {
    ClubActivity activity = submitted();

    activity.update(null, DAY, "내용 보충", null, RESUBMITTED);

    assertThat(activity.getStatus()).isEqualTo(ClubActivityStatus.PENDING);
    assertThat(activity.getContent()).isEqualTo("내용 보충");
  }

  @Test
  @DisplayName("인증 완료된 기록은 고칠 수 없다")
  void approvedCannotBeUpdated() {
    ClubActivity activity = approved();

    assertLocked(() -> activity.update(null, DAY, "몰래 수정", null, RESUBMITTED));
    assertThat(activity.getContent()).isEqualTo("1주차 모임");
    assertThat(activity.getStatus()).isEqualTo(ClubActivityStatus.APPROVED);
  }

  @Test
  @DisplayName("인증 완료된 기록은 다시 인증하거나 보완 요청할 수 없다")
  void approvedCannotBeReviewedAgain() {
    ClubActivity activity = approved();

    assertLocked(() -> activity.approve(REVIEWER_ID, RESUBMITTED));
    assertLocked(() -> activity.requestRevision(REVIEWER_ID, "늦게 발견", RESUBMITTED));
    assertThat(activity.getReviewedAt()).isEqualTo(REVIEWED);
    assertThat(activity.getRevisionReason()).isNull();
  }

  @Test
  @DisplayName("출석·사진을 고치기 전 검사도 인증 완료면 막힌다")
  void requireEditableGuardsOutsideChanges() {
    assertLocked(() -> approved().requireEditable());
  }
}
