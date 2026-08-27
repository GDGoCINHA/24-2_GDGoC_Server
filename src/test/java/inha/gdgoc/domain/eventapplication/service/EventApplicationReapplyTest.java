package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import inha.gdgoc.domain.eventapplication.dto.request.EventApplicationSubmitRequest;
import inha.gdgoc.domain.eventapplication.dto.request.EventApplicationSubmitRequest.AnswerEntry;
import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.enums.QuestionType;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 취소한 신청을 다시 내는 경로를 <b>실제 DB 제약과 함께</b> 검증한다.
 *
 * <p>같은 도메인의 다른 테스트는 리포지토리를 목으로 두어 SQL 이 나가지 않는다. 그래서 {@code
 * uq_event_answer_application_question} 같은 제약 위반을 잡지 못했다 — 실제로 2026-08-27 dev 에서
 * "취소 후 재신청" 이 500 으로 실패했고 목 기반 테스트는 전부 통과하고 있었다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class EventApplicationReapplyTest {

  private static final Long BOARD_ID = 4242L;

  @Autowired private EventApplicationService eventApplicationService;
  @Autowired private EventApplicationFormRepository formRepository;
  @Autowired private EventApplicationRepository applicationRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager entityManager;

  private Long userId;
  private EventApplicationForm form;

  @BeforeEach
  void setUp() {
    User user =
        userRepository.save(
            User.builder()
                .name("홍길동")
                .oauthSubject("oauth-event-reapply")
                .major("CSE")
                .studentId("12200001")
                .phoneNumber("01000000001")
                .email("hong@inha.edu")
                .build());
    user.changeRole(UserRole.MEMBER);
    userId = user.getId();

    form =
        EventApplicationForm.create(
            BOARD_ID,
            "가을 해커톤",
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 2),
            null,
            null,
            null,
            UserRole.MEMBER,
            true);
    form.publish(Instant.parse("2026-08-01T00:00:00Z"));
    form.addQuestion(question(form, "나이", 0));
    formRepository.saveAndFlush(form);
  }

  // 2026-08-27 dev 재현: 신청 → 취소 → 재신청에서 500 이 났다.
  // 되살린 행에 같은 질문의 답변을 다시 넣는데, 옛 답변이 아직 지워지지 않은 채였다.
  @Test
  @DisplayName("취소한 신청을 같은 답으로 다시 낼 수 있다")
  void reapplyWithSameAnswer() {
    Long questionId = firstQuestionId();
    apply(new AnswerEntry(questionId, "17"));
    eventApplicationService.cancel(BOARD_ID, userId);

    assertThatCode(() -> apply(new AnswerEntry(questionId, "18"))).doesNotThrowAnyException();

    EventApplication application = onlyApplication();
    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    assertThat(application.getAnswers())
        .singleElement()
        .satisfies(answer -> assertThat(answer.getValue()).contains("18"));
  }

  // 사용자가 실제로 밟은 순서다. 취소한 사이에 운영진이 질문을 늘렸다.
  @Test
  @DisplayName("취소한 사이에 질문이 늘어도 다시 낼 수 있다")
  void reapplyAfterFormGrew() {
    Long firstQuestionId = firstQuestionId();
    apply(new AnswerEntry(firstQuestionId, "17"));
    eventApplicationService.cancel(BOARD_ID, userId);

    EventApplicationForm saved = formRepository.findById(form.getId()).orElseThrow();
    saved.addQuestion(question(saved, "한마디", 1));
    formRepository.saveAndFlush(saved);
    Long addedQuestionId = saved.activeQuestions().get(1).getId();

    assertThatCode(
            () ->
                apply(
                    new AnswerEntry(firstQuestionId, "17"),
                    new AnswerEntry(addedQuestionId, "잘 부탁드립니다")))
        .doesNotThrowAnyException();

    assertThat(onlyApplication().getAnswers()).hasSize(2);
  }

  // 답을 지우고 다시 내면 옛 답변이 남아서는 안 된다.
  @Test
  @DisplayName("재신청에서 뺀 답변은 남지 않는다")
  void reapplyDropsRemovedAnswer() {
    Long questionId = firstQuestionId();
    apply(new AnswerEntry(questionId, "17"));
    eventApplicationService.cancel(BOARD_ID, userId);

    apply();

    assertThat(onlyApplication().getAnswers()).isEmpty();
  }

  private void apply(AnswerEntry... answers) {
    eventApplicationService.apply(
        BOARD_ID, new EventApplicationSubmitRequest(List.of(answers)), userId, UserRole.MEMBER);
    // 서비스의 트랜잭션이 테스트 트랜잭션에 합류해 커밋이 없다. 제약 위반은 flush 에서 드러난다.
    entityManager.flush();
    entityManager.clear();
  }

  private Long firstQuestionId() {
    return formRepository.findById(form.getId()).orElseThrow().activeQuestions().get(0).getId();
  }

  private EventApplication onlyApplication() {
    List<EventApplication> all = applicationRepository.findAll();
    assertThat(all).hasSize(1);
    return all.get(0);
  }

  private static EventFormQuestion question(
      EventApplicationForm form, String label, int sortOrder) {
    return EventFormQuestion.create(
        form, QuestionType.SHORT_TEXT, label, null, false, sortOrder, null, null, null);
  }
}
