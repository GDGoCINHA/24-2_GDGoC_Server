package inha.gdgoc.domain.eventapplication.service;

import static inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode.*;

import inha.gdgoc.domain.eventapplication.dto.request.EventApplicationSubmitRequest;
import inha.gdgoc.domain.eventapplication.dto.response.EventFormPublicResponse;
import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationAnswer;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Clock;
import java.time.ZoneId;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부원의 신청과 취소.
 *
 * <p>신청 가능 여부 판정을 한 곳({@link #blockedBy})에 모아두고 조회와 신청이 같은 것을 쓴다. 화면에 보이는 버튼 상태와 실제 처리 결과가 어긋나지 않게 하려는
 * 것이다.
 */
@Service
@Transactional(readOnly = true)
public class EventApplicationService {

  private final EventApplicationFormRepository formRepository;
  private final EventApplicationRepository applicationRepository;
  private final UserRepository userRepository;
  private final AnswerValidator answerValidator;
  private final AnswerCodec answerCodec;
  private final Clock clock;

  @Autowired
  public EventApplicationService(
      EventApplicationFormRepository formRepository,
      EventApplicationRepository applicationRepository,
      UserRepository userRepository,
      AnswerValidator answerValidator,
      AnswerCodec answerCodec) {
    this(
        formRepository,
        applicationRepository,
        userRepository,
        answerValidator,
        answerCodec,
        Clock.system(ZoneId.of("Asia/Seoul")));
  }

  /** 테스트가 마감·개시 시각을 고정할 때 쓴다. */
  public EventApplicationService(
      EventApplicationFormRepository formRepository,
      EventApplicationRepository applicationRepository,
      UserRepository userRepository,
      AnswerValidator answerValidator,
      AnswerCodec answerCodec,
      Clock clock) {
    this.formRepository = formRepository;
    this.applicationRepository = applicationRepository;
    this.userRepository = userRepository;
    this.answerValidator = answerValidator;
    this.answerCodec = answerCodec;
    this.clock = clock;
  }

  public EventFormPublicResponse getForm(Long eventBoardId, Long userId, UserRole role) {
    EventApplicationForm form = findForm(eventBoardId);
    long applied = countApplied(form);

    EventApplication mine =
        userId == null
            ? null
            : applicationRepository.findByFormIdAndUserId(form.getId(), userId).orElse(null);

    EventApplicationErrorCode blocked = blockedBy(form, role, applied, Instant.now(clock));
    // 이미 신청한 사람에게는 정원이 찼다는 안내가 의미 없다.
    if (mine != null && mine.isApplied() && blocked == CAPACITY_FULL) {
      blocked = null;
    }

    return EventFormPublicResponse.of(
        form,
        applied,
        blocked == null,
        blocked == null ? null : blocked.getMessage(),
        mine,
        mine == null ? null : answerCodec.readAll(mine));
  }

  @Transactional
  public void apply(Long eventBoardId, EventApplicationSubmitRequest req, Long userId, UserRole role) {
    // 정원을 세기 전에 폼을 잠근다. 잠그지 않으면 동시 신청이 정원을 넘긴다.
    EventApplicationForm form =
        formRepository
            .findByEventBoardIdForUpdate(eventBoardId)
            .filter(EventApplicationForm::isPublished)
            .orElseThrow(() -> new BusinessException(FORM_NOT_FOUND));

    EventApplication existing =
        applicationRepository.findByFormIdAndUserId(form.getId(), userId).orElse(null);
    if (existing != null && existing.isApplied()) {
      throw new BusinessException(ALREADY_APPLIED);
    }

    long applied = countApplied(form);
    EventApplicationErrorCode blocked = blockedBy(form, role, applied, Instant.now(clock));
    if (blocked != null) {
      throw new BusinessException(blocked);
    }

    Map<Long, Object> submitted = toAnswerMap(req);
    Map<Long, Object> kept = answerValidator.validateAndFilter(form.activeQuestions(), submitted);

    Instant now = Instant.now(clock);
    EventApplication application;
    if (existing != null) {
      // 취소했던 신청은 같은 행을 되살린다. UNIQUE(form_id, user_id) 때문에 새로 만들 수 없다.
      existing.clearAnswers();
      existing.reapply(now);
      application = existing;
    } else {
      User user =
          userRepository
              .findById(userId)
              .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
      application = EventApplication.create(form, user, now);
      applicationRepository.save(application);
    }

    for (Map.Entry<Long, Object> entry : kept.entrySet()) {
      application.addAnswer(
          EventApplicationAnswer.create(application, entry.getKey(), answerCodec.write(entry.getValue())));
    }
  }

  @Transactional
  public void cancel(Long eventBoardId, Long userId) {
    EventApplicationForm form = findForm(eventBoardId);
    EventApplication application =
        applicationRepository
            .findByFormIdAndUserId(form.getId(), userId)
            .filter(EventApplication::isApplied)
            .orElseThrow(() -> new BusinessException(APPLICATION_NOT_FOUND));

    application.cancel(Instant.now(clock));
  }

  /** 신청을 막는 첫 번째 사유. 없으면 null 이다. */
  private EventApplicationErrorCode blockedBy(
      EventApplicationForm form, UserRole role, long applied, Instant now) {
    if (!form.isOpen()) {
      return FORM_CLOSED;
    }
    if (form.getOpensAt() != null && now.isBefore(form.getOpensAt())) {
      return NOT_OPEN_YET;
    }
    if (form.getClosesAt() != null && !now.isBefore(form.getClosesAt())) {
      return ALREADY_CLOSED;
    }
    if (!UserRole.hasAtLeast(role, form.getMinRole())) {
      return NOT_ELIGIBLE;
    }
    if (form.getCapacity() != null && applied >= form.getCapacity()) {
      return CAPACITY_FULL;
    }
    return null;
  }

  private Map<Long, Object> toAnswerMap(EventApplicationSubmitRequest req) {
    Map<Long, Object> submitted = new LinkedHashMap<>();
    if (req != null && req.answers() != null) {
      for (EventApplicationSubmitRequest.AnswerEntry entry : req.answers()) {
        submitted.put(entry.questionId(), entry.value());
      }
    }
    return submitted;
  }




  /**
   * 부원에게 보이는 폼만 찾는다.
   *
   * <p>발행 전 폼은 없는 것으로 다룬다. 반쯤 만들어진 질문들이 행사 상세에 뜨면 안 되고, 웹은 404 를 "신청을 받지 않는 행사" 로 읽어 신청 영역을
   * 통째로 그리지 않는다.
   */
  private EventApplicationForm findForm(Long eventBoardId) {
    return formRepository
        .findByEventBoardId(eventBoardId)
        .filter(EventApplicationForm::isPublished)
        .orElseThrow(() -> new BusinessException(FORM_NOT_FOUND));
  }

  private long countApplied(EventApplicationForm form) {
    return applicationRepository.countByFormIdAndStatus(form.getId(), ApplicationStatus.APPLIED);
  }
}
