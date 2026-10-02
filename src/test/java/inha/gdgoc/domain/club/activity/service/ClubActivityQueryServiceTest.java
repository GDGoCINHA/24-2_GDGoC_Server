package inha.gdgoc.domain.club.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.club.activity.dto.ApprovedActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.activity.exception.ClubActivityErrorCode;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * C 에게 주는 계약({@code findApproved}·{@code getForUpdate})을 <b>실제 SQL 로</b> 검증한다.
 *
 * <p>{@code findApproved} 는 JPQL 집계와 생성자 표현식이라 목으로는 아무것도 확인되지 않는다. 숫자가 틀리면 C 의 참석 비율 판정이 전부
 * 틀린다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ClubActivityQueryServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");
  private static final Long REVIEWER_ID = 99L;
  private static final LocalDate FROM = LocalDate.of(2026, 10, 5);
  private static final LocalDate TO = LocalDate.of(2026, 10, 18);

  @Autowired private ClubActivityQueryService clubActivityQueryService;
  @Autowired private EntityManager entityManager;

  private Club club;
  private Club otherClub;
  private List<User> members;

  @BeforeEach
  void setUp() {
    members = List.of(user(1), user(2), user(3));
    ClubTerm term = term();
    club = club(term, members.get(0), "알고리즘 스터디");
    otherClub = club(term, members.get(0), "러닝 크루");
  }

  @Test
  @DisplayName("인증 완료된 기록만 명단 수·출석 수와 함께 나온다")
  void onlyApprovedWithCounts() {
    ClubActivity approved = activity(club, LocalDate.of(2026, 10, 7), true, true, false);
    activity(club, LocalDate.of(2026, 10, 8), true, true, true); // 확인 중
    ClubActivity revised = activity(club, LocalDate.of(2026, 10, 9), true, true, true);
    revised.requestRevision(REVIEWER_ID, "사진이 없어요", NOW);
    approve(approved);

    List<ApprovedActivity> result = clubActivityQueryService.findApproved(club.getId(), FROM, TO);

    assertThat(result)
        .containsExactly(
            new ApprovedActivity(approved.getId(), LocalDate.of(2026, 10, 7), 3, 2));
  }

  @Test
  @DisplayName("기간은 양 끝을 포함하고, 밖의 기록은 빠진다")
  void periodIsInclusive() {
    ClubActivity before = approve(activity(club, FROM.minusDays(1), true));
    ClubActivity first = approve(activity(club, FROM, true));
    ClubActivity last = approve(activity(club, TO, true));
    ClubActivity after = approve(activity(club, TO.plusDays(1), true));

    List<ApprovedActivity> result = clubActivityQueryService.findApproved(club.getId(), FROM, TO);

    assertThat(result)
        .extracting(ApprovedActivity::activityId)
        .containsExactly(first.getId(), last.getId())
        .doesNotContain(before.getId(), after.getId());
  }

  @Test
  @DisplayName("활동일 순으로 나오고, 같은 날 여러 번 모인 기록은 각각 나온다")
  void orderedByDateWithSameDayRecords() {
    ClubActivity laterDay = approve(activity(club, LocalDate.of(2026, 10, 10), true));
    ClubActivity sameDayFirst = approve(activity(club, LocalDate.of(2026, 10, 7), true));
    ClubActivity sameDaySecond = approve(activity(club, LocalDate.of(2026, 10, 7), false));

    List<ApprovedActivity> result = clubActivityQueryService.findApproved(club.getId(), FROM, TO);

    assertThat(result)
        .extracting(ApprovedActivity::activityId)
        .containsExactly(sameDayFirst.getId(), sameDaySecond.getId(), laterDay.getId());
  }

  @Test
  @DisplayName("다른 소모임의 기록은 섞이지 않는다")
  void otherClubExcluded() {
    approve(activity(otherClub, LocalDate.of(2026, 10, 7), true, true, true));

    assertThat(clubActivityQueryService.findApproved(club.getId(), FROM, TO)).isEmpty();
  }

  @Test
  @DisplayName("명단 행이 없는 기록은 0/0 으로 나온다")
  void noRosterRowsIsZero() {
    ClubActivity empty = approve(activity(club, LocalDate.of(2026, 10, 7)));

    assertThat(clubActivityQueryService.findApproved(club.getId(), FROM, TO))
        .containsExactly(new ApprovedActivity(empty.getId(), LocalDate.of(2026, 10, 7), 0, 0));
  }

  @Test
  @DisplayName("잠가서 읽은 기록을 인증 완료하면 저장된다")
  void getForUpdateThenApprove() {
    Long id = activity(club, LocalDate.of(2026, 10, 7), true).getId();
    entityManager.clear();

    clubActivityQueryService.getForUpdate(id).approve(REVIEWER_ID, NOW);
    entityManager.flush();
    entityManager.clear();

    assertThat(entityManager.find(ClubActivity.class, id).getStatus())
        .isEqualTo(ClubActivityStatus.APPROVED);
  }

  @Test
  @DisplayName("없는 기록을 잠가 읽으면 404")
  void getForUpdateMissing() {
    assertThatThrownBy(() -> clubActivityQueryService.getForUpdate(-1L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ClubActivityErrorCode.ACTIVITY_NOT_FOUND);
  }

  /** 명단은 {@code members} 앞에서부터 출석 여부 개수만큼. */
  private ClubActivity activity(Club target, LocalDate date, boolean... attended) {
    ClubActivity activity = ClubActivity.create(target, null, date, "모임", null, 1L, NOW);
    entityManager.persist(activity);
    for (int i = 0; i < attended.length; i++) {
      entityManager.persist(ClubActivityAttendance.of(activity, members.get(i), attended[i]));
    }
    entityManager.flush();
    return activity;
  }

  private ClubActivity approve(ClubActivity activity) {
    activity.approve(REVIEWER_ID, NOW);
    entityManager.flush();
    return activity;
  }

  private User user(int n) {
    User user =
        User.builder()
            .name("멤버" + n)
            .oauthSubject("oauth-club-activity-" + n)
            .major("CSE")
            .studentId("1226000" + n)
            .phoneNumber("0100000000" + n)
            .email("club-activity-" + n + "@inha.edu")
            .build();
    entityManager.persist(user);
    return user;
  }

  // ClubTerm 의 생성 메서드는 아직 없다(A·C 담당). 생기면 그걸로 바꾼다.
  private ClubTerm term() {
    ClubTerm term = BeanUtils.instantiateClass(ClubTerm.class);
    ReflectionTestUtils.setField(term, "name", "2026-2");
    ReflectionTestUtils.setField(term, "attendanceRatio", new BigDecimal("0.50"));
    entityManager.persist(term);
    return term;
  }

  private Club club(ClubTerm term, User leader, String name) {
    Club club =
        Club.create(
            term, leader, name, ClubCategory.STUDY, name + " 소개", null, null, null, null, null,
            null, null);
    entityManager.persist(club);
    return club;
  }
}
