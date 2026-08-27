package inha.gdgoc.domain.eventapplication.service;

import static inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode.*;

import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.board.event.repository.EventBoardRepository;
import inha.gdgoc.domain.eventapplication.dto.request.EventFormSaveRequest;
import inha.gdgoc.domain.eventapplication.dto.request.QuestionOrderRequest;
import inha.gdgoc.domain.eventapplication.dto.request.QuestionSaveRequest;
import inha.gdgoc.domain.eventapplication.dto.response.EventFormResponse;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationRepository;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.exception.BusinessException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영진이 신청 폼과 질문을 만드는 서비스.
 *
 * <p>신청이 한 건이라도 들어온 뒤에는 고칠 수 있는 범위가 좁아진다. 저장된 답변의 형태가 달라지거나 답변이 고아가 되는 변경을 막기 위해서다. 그 판정 기준이 되는 신청 수는
 * 취소를 뺀 {@code APPLIED} 만 센다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventFormAdminService {

  private final EventApplicationFormRepository formRepository;
  private final EventApplicationRepository applicationRepository;
  private final EventBoardRepository eventBoardRepository;
  private final EventFormValidator validator;

  public EventFormResponse getForm(Long eventBoardId) {
    EventApplicationForm form = findForm(eventBoardId);
    return EventFormResponse.of(form, countApplied(form));
  }

  /** 신청 받기를 켠다. 이 행이 생겨야 행사 상세에 신청 버튼이 뜬다. */
  @Transactional
  public Long createForm(Long eventBoardId, EventFormSaveRequest req) {
    if (formRepository.existsByEventBoardId(eventBoardId)) {
      throw new BusinessException(FORM_ALREADY_EXISTS);
    }
    EventBoard board = findLiveBoard(eventBoardId);

    EventApplicationForm form =
        EventApplicationForm.create(
            eventBoardId,
            board.getTitle(),
            board.getEventStartDate(),
            board.getEventEndDate(),
            req.opensAt(),
            req.closesAt(),
            req.capacity(),
            req.minRole() != null ? req.minRole() : UserRole.MEMBER,
            req.isOpen() == null || req.isOpen());
    return formRepository.save(form).getId();
  }

  @Transactional
  public void updateForm(Long eventBoardId, EventFormSaveRequest req) {
    EventApplicationForm form = findForm(eventBoardId);

    if (req.capacity() != null && req.capacity() < countApplied(form)) {
      throw new BusinessException(CAPACITY_BELOW_APPLICANTS);
    }
    form.updateSettings(req.opensAt(), req.closesAt(), req.capacity(), req.minRole(), req.isOpen());
    if (req.clearCapacity()) {
      form.clearCapacity();
    }
    if (form.getOpensAt() != null
        && form.getClosesAt() != null
        && !form.getOpensAt().isBefore(form.getClosesAt())) {
      throw new BusinessException(PERIOD_INVALID);
    }
  }

  /** 신청 받기를 해제한다. 신청자가 있으면 지우지 않고 마감으로 닫도록 안내한다. */
  @Transactional
  public void deleteForm(Long eventBoardId) {
    EventApplicationForm form = findForm(eventBoardId);
    if (countApplied(form) > 0) {
      throw new BusinessException(FORM_HAS_APPLICATIONS);
    }
    formRepository.delete(form);
  }

  @Transactional
  public Long addQuestion(Long eventBoardId, QuestionSaveRequest req) {
    EventApplicationForm form = findForm(eventBoardId);
    List<EventFormQuestion> siblings = form.activeQuestions();
    boolean hasApplicants = countApplied(form) > 0;

    // 신청이 들어온 뒤에 필수 질문을 더하면 기존 신청자가 미응답 상태로 필수를 위반하게 된다.
    if (hasApplicants && req.isRequired()) {
      throw new BusinessException(QUESTION_MUST_BE_OPTIONAL);
    }
    validator.validateShape(req.type(), req.options());

    int sortOrder = siblings.stream().mapToInt(EventFormQuestion::getSortOrder).max().orElse(-1) + 1;
    validator.validateCondition(
        sortOrder, req.visibleWhenQuestionId(), req.visibleWhenValues(), siblings);

    EventFormQuestion question =
        EventFormQuestion.create(
            form,
            req.type(),
            req.label(),
            req.helpText(),
            req.isRequired(),
            sortOrder,
            req.options(),
            req.visibleWhenQuestionId(),
            req.visibleWhenValues());
    form.addQuestion(question);
    formRepository.flush();
    return question.getId();
  }

  @Transactional
  public void updateQuestion(Long eventBoardId, Long questionId, QuestionSaveRequest req) {
    EventApplicationForm form = findForm(eventBoardId);
    List<EventFormQuestion> siblings = form.activeQuestions();
    EventFormQuestion question = findQuestion(siblings, questionId);
    boolean hasApplicants = countApplied(form) > 0;

    if (hasApplicants) {
      if (req.type() != question.getType()) {
        throw new BusinessException(QUESTION_SHAPE_LOCKED);
      }
      if (req.isRequired() && !question.isRequired()) {
        throw new BusinessException(QUESTION_REQUIRED_LOCKED);
      }
      // 라벨은 고칠 수 있지만 값은 지울 수 없다. 그 값을 고른 답변이 고아가 된다.
      validator.validateOptionValuesKept(question.getOptions(), req.options());
    } else if (req.type() != question.getType()) {
      // 유형이 바뀌면 이 질문을 기준으로 삼던 조건이 성립하지 않을 수 있다.
      validator.validateNotReferenced(questionId, siblings);
    }

    validator.validateShape(req.type(), req.options());
    Long baseId = req.clearCondition() ? null : req.visibleWhenQuestionId();
    List<String> values = req.clearCondition() ? null : req.visibleWhenValues();
    validator.validateCondition(question.getSortOrder(), baseId, values, siblings);

    question.updateContent(req.label(), req.helpText(), req.isRequired());
    question.updateShape(req.type(), req.options());
    question.updateCondition(baseId, values);
  }

  /** 지운 표시만 한다. 기존 답변은 남고 신청자 목록에 흐리게 나온다. */
  @Transactional
  public void deleteQuestion(Long eventBoardId, Long questionId) {
    EventApplicationForm form = findForm(eventBoardId);
    List<EventFormQuestion> siblings = form.activeQuestions();
    EventFormQuestion question = findQuestion(siblings, questionId);

    validator.validateNotReferenced(questionId, siblings);
    question.softDelete();
  }

  @Transactional
  public void reorderQuestions(Long eventBoardId, QuestionOrderRequest req) {
    EventApplicationForm form = findForm(eventBoardId);
    List<EventFormQuestion> siblings = form.activeQuestions();

    Set<Long> current = new HashSet<>(siblings.stream().map(EventFormQuestion::getId).toList());
    Set<Long> requested = new HashSet<>(req.questionIds());
    if (current.size() != requested.size() || !current.containsAll(requested)) {
      throw new BusinessException(SORT_ORDER_MISMATCH);
    }

    List<EventFormQuestion> inNewOrder = new ArrayList<>();
    for (Long id : req.questionIds()) {
      inNewOrder.add(findQuestion(siblings, id));
    }
    validator.validateOrderKeepsConditions(inNewOrder);

    for (int i = 0; i < inNewOrder.size(); i++) {
      inNewOrder.get(i).updateSortOrder(i);
    }
  }

  private EventApplicationForm findForm(Long eventBoardId) {
    return formRepository
        .findByEventBoardId(eventBoardId)
        .orElseThrow(() -> new BusinessException(FORM_NOT_FOUND));
  }

  private EventBoard findLiveBoard(Long eventBoardId) {
    return eventBoardRepository
        .findById(eventBoardId)
        .filter(board -> board.getDeletedAt() == null)
        .orElseThrow(() -> new BusinessException(EVENT_BOARD_NOT_FOUND));
  }

  private EventFormQuestion findQuestion(List<EventFormQuestion> siblings, Long questionId) {
    return siblings.stream()
        .filter(q -> q.getId().equals(questionId))
        .findFirst()
        .orElseThrow(() -> new BusinessException(QUESTION_NOT_FOUND));
  }

  private long countApplied(EventApplicationForm form) {
    return applicationRepository.countByFormIdAndStatus(form.getId(), ApplicationStatus.APPLIED);
  }
}
