package inha.gdgoc.domain.club.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.schedule.dto.response.ClubCheckinResponse;
import inha.gdgoc.domain.club.schedule.entity.ClubSchedule;
import inha.gdgoc.domain.club.schedule.exception.ClubScheduleErrorCode;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * QR 체크인을 실제 DB 로 검증한다.
 *
 * <p><b>이 테스트는 롤백하지 않는다.</b> 체크인 서비스가 트랜잭션 밖에서 돌기 때문에({@link ClubCheckinService#checkIn}) 테스트
 * 트랜잭션 안에 넣은 데이터를 보지 못한다. 그래서 준비 데이터를 커밋하고 {@link #cleanUp} 에서 직접 지운다.
 */
@ActiveProfiles("test")
@SpringBootTest
class ClubCheckinServiceTest {

  @Autowired private ClubCheckinService clubCheckinService;
  @Autowired private ClubCheckinTokenService clubCheckinTokenService;
  @Autowired private EntityManager entityManager;
  @Autowired private TransactionTemplate transactionTemplate;

  private final List<Object> created = new ArrayList<>();
  private Long clubId;
  private Long scheduleId;
  private Long memberId;
  private Long outsiderId;

  @BeforeEach
  void setUp() {
    transactionTemplate.executeWithoutResult(
        status -> {
          User leader = persist(user(1, "리더"));
          User member = persist(user(2, "민지"));
          User outsider = persist(user(3, "외부인"));
          ClubTerm term = BeanUtils.instantiateClass(ClubTerm.class);
          ReflectionTestUtils.setField(term, "name", "2026-2");
          ReflectionTestUtils.setField(term, "attendanceRatio", new BigDecimal("0.50"));
          persist(term);
          Club club =
              persist(
                  Club.create(
                      term,
                      leader,
                      "체크인 테스트",
                      ClubCategory.STUDY,
                      "소개",
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null));
          Instant joined = Instant.now().minus(Duration.ofDays(7));
          persist(ClubMember.founder(club, leader, joined));
          ClubMember joining = ClubMember.apply(club, member, null, joined);
          joining.approve(joined);
          persist(joining);
          ClubSchedule schedule =
              persist(ClubSchedule.create(club, "3주차", Instant.now(), null, null, null, null));

          clubId = club.getId();
          scheduleId = schedule.getId();
          memberId = member.getId();
          outsiderId = outsider.getId();
        });
  }

  @AfterEach
  void cleanUp() {
    transactionTemplate.executeWithoutResult(
        status -> {
          entityManager
              .createQuery("delete from ClubScheduleCheckin c where c.schedule.id = :s")
              .setParameter("s", scheduleId)
              .executeUpdate();
          // 만든 순서의 역순으로 지운다(외래 키).
          for (int i = created.size() - 1; i >= 0; i--) {
            entityManager.remove(entityManager.merge(created.get(i)));
          }
        });
    created.clear();
  }

  @Test
  @DisplayName("멤버가 찍으면 체크인되고, 다시 찍으면 처음 시각 그대로 200 이다")
  void checkInThenAgain() {
    String token = clubCheckinTokenService.issue(scheduleId);

    ClubCheckinResponse first = clubCheckinService.checkIn(token, memberId);
    ClubCheckinResponse second = clubCheckinService.checkIn(token, memberId);

    assertThat(first.alreadyCheckedIn()).isFalse();
    assertThat(first.clubId()).isEqualTo(clubId);
    assertThat(first.scheduleTitle()).isEqualTo("3주차");
    assertThat(second.alreadyCheckedIn()).isTrue();
    assertThat(second.checkedAt()).isEqualTo(first.checkedAt());
    assertThat(checkinCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("팀원이 아니면 체크인할 수 없다")
  void outsiderIsForbidden() {
    String token = clubCheckinTokenService.issue(scheduleId);

    assertError(() -> clubCheckinService.checkIn(token, outsiderId), ClubErrorCode.NOT_CLUB_MEMBER);
    assertThat(checkinCount()).isZero();
  }

  @Test
  @DisplayName("위조·만료된 토큰은 400 이다")
  void invalidTokenIsRejected() {
    assertError(
        () -> clubCheckinService.checkIn("1.zzzz.forged", memberId),
        ClubScheduleErrorCode.CHECKIN_TOKEN_INVALID);
  }

  @Test
  @DisplayName("지워진 일정의 토큰은 404 다")
  void deletedScheduleIsNotFound() {
    String token = clubCheckinTokenService.issue(Long.MAX_VALUE / 1_000);

    assertError(
        () -> clubCheckinService.checkIn(token, memberId),
        ClubScheduleErrorCode.SCHEDULE_NOT_FOUND);
  }

  private long checkinCount() {
    return transactionTemplate.execute(
        status ->
            entityManager
                .createQuery(
                    "select count(c) from ClubScheduleCheckin c where c.schedule.id = :s",
                    Long.class)
                .setParameter("s", scheduleId)
                .getSingleResult());
  }

  private static void assertError(ThrowingCallable call, ErrorCode expected) {
    assertThatThrownBy(call)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(expected);
  }

  private <T> T persist(T entity) {
    entityManager.persist(entity);
    created.add(entity);
    return entity;
  }

  private static User user(int n, String name) {
    return User.builder()
        .name(name)
        .oauthSubject("oauth-club-checkin-svc-" + n)
        .major("CSE")
        .studentId("1226300" + n)
        .phoneNumber("0103000000" + n)
        .email("club-checkin-svc-" + n + "@inha.edu")
        .build();
  }
}
