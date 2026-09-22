package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.eventapplication.dto.request.AnonymousApplicationRequest;
import inha.gdgoc.domain.eventapplication.dto.request.EventApplicationSubmitRequest;
import inha.gdgoc.domain.eventapplication.dto.request.EventApplicationSubmitRequest.AnswerEntry;
import inha.gdgoc.domain.eventapplication.dto.response.ApplicantResponse;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import inha.gdgoc.domain.eventapplication.enums.QuestionType;
import inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 없이 낸 신청을 <b>실제 DB 와 함께</b> 검증한다.
 *
 * <p>목 기반 테스트는 SQL 을 내보내지 않는다. 여기서는 사용자가 없는 행이 관리자 목록에서 빠지지 않는지(left join), 계정 신청과 학번으로 겹치는지를
 * 쿼리 그대로 본다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class EventAnonymousApplicationTest {

  private static final Long BOARD_ID = 4343L;
  private static final String MEMBER_STUDENT_ID = "12200002";

  @Autowired private EventApplicationService eventApplicationService;
  @Autowired private EventApplicantAdminService eventApplicantAdminService;
  @Autowired private EventApplicationFormRepository formRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager entityManager;

  private Long userId;
  private Long questionId;

  @BeforeEach
  void setUp() {
    User user =
        userRepository.save(
            User.builder()
                .name("홍길동")
                .oauthSubject("oauth-event-anonymous")
                .major("CSE")
                .studentId(MEMBER_STUDENT_ID)
                .phoneNumber("01000000002")
                .email("hong2@inha.edu")
                .build());
    user.changeRole(UserRole.MEMBER);
    userId = user.getId();

    EventApplicationForm form =
        EventApplicationForm.create(
            BOARD_ID,
            "강연 & ROUND TABLE",
            // 날짜를 고정하면 그 날이 지난 뒤부터 EVENT_ENDED 가드에 걸려 테스트가 깨진다.
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            null,
            null,
            null,
            UserRole.MEMBER,
            true);
    form.changeAllowAnonymous(true);
    form.publish(Instant.parse("2026-08-01T00:00:00Z"));
    form.addQuestion(
        EventFormQuestion.create(
            form, QuestionType.SHORT_TEXT, "한마디", null, false, 0, null, null, null));
    formRepository.saveAndFlush(form);
    questionId = form.activeQuestions().get(0).getId();
  }

  @Test
  @DisplayName("계정 신청과 비로그인 신청이 관리자 목록·CSV 에 함께 나온다")
  void adminSeesBothKinds() {
    eventApplicationService.apply(
        BOARD_ID,
        new EventApplicationSubmitRequest(List.of(new AnswerEntry(questionId, "계정"))),
        userId,
        UserRole.MEMBER);
    applyAnonymously("12241234");

    List<ApplicantResponse> applicants =
        eventApplicantAdminService
            .listApplicants(BOARD_ID, null, PageRequest.of(0, 50, Sort.by("appliedAt")))
            .getContent();

    // inner join 이면 사용자가 없는 신청이 여기서 조용히 빠진다.
    assertThat(applicants).extracting(ApplicantResponse::name).containsExactlyInAnyOrder("홍길동", "김링크");
    ApplicantResponse anonymous =
        applicants.stream().filter(a -> a.userId() == null).findFirst().orElseThrow();
    assertThat(anonymous.studentId()).isEqualTo("12241234");
    assertThat(anonymous.major()).isEqualTo("CSE");
    assertThat(anonymous.phoneNumber()).isEqualTo("01012345678");
    assertThat(anonymous.answers()).containsEntry(questionId, "비로그인");

    String csv = new String(eventApplicantAdminService.exportCsv(BOARD_ID, null), StandardCharsets.UTF_8);
    assertThat(csv).contains("김링크").contains("12241234").contains("컴퓨터공학과");
  }

  @Test
  @DisplayName("계정으로 신청한 학번은 로그인 없이 다시 낼 수 없다")
  void anonymousBlockedByAccountStudentId() {
    eventApplicationService.apply(
        BOARD_ID, new EventApplicationSubmitRequest(List.of()), userId, UserRole.MEMBER);
    flush();

    assertErrorCode(
        () -> applyAnonymously(MEMBER_STUDENT_ID), EventApplicationErrorCode.ALREADY_APPLIED);
  }

  @Test
  @DisplayName("로그인 없이 같은 학번으로 두 번 낼 수 없다")
  void anonymousBlockedBySameStudentId() {
    applyAnonymously("12241234");

    assertErrorCode(() -> applyAnonymously("12241234"), EventApplicationErrorCode.ALREADY_APPLIED);
  }

  @Test
  @DisplayName("로그인 없이 먼저 낸 학번으로 계정 신청할 수 없다")
  void accountBlockedByAnonymousStudentId() {
    applyAnonymously(MEMBER_STUDENT_ID);

    assertErrorCode(
        () ->
            eventApplicationService.apply(
                BOARD_ID, new EventApplicationSubmitRequest(List.of()), userId, UserRole.MEMBER),
        EventApplicationErrorCode.ALREADY_APPLIED);
  }

  private void applyAnonymously(String studentId) {
    eventApplicationService.applyAnonymously(
        BOARD_ID,
        new AnonymousApplicationRequest(
            "김링크",
            studentId,
            "CSE",
            "010-1234-5678",
            true,
            List.of(new AnswerEntry(questionId, "비로그인"))));
    flush();
  }

  /** 서비스의 트랜잭션이 테스트 트랜잭션에 합류해 커밋이 없다. 제약 위반은 flush 에서 드러난다. */
  private void flush() {
    entityManager.flush();
    entityManager.clear();
  }

  private void assertErrorCode(Runnable action, EventApplicationErrorCode expected) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(expected);
  }
}
