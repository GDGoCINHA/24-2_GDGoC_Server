package inha.gdgoc.domain.eventapplication.service;

import static inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode.*;

import inha.gdgoc.domain.eventapplication.dto.request.AttendanceUpdateRequest;
import inha.gdgoc.domain.eventapplication.dto.request.ProxyApplicationRequest;
import inha.gdgoc.domain.eventapplication.dto.response.ApplicantResponse;
import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.enums.EventAttendanceStatus;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 운영진의 신청자 확인과 참석 처리. */
@Service
@Transactional(readOnly = true)
public class EventApplicantAdminService {

  private final EventApplicationFormRepository formRepository;
  private final EventApplicationRepository applicationRepository;
  private final UserRepository userRepository;
  private final AnswerCodec answerCodec;
  private final ApplicantCsvWriter csvWriter;
  private final Clock clock;

  @Autowired
  public EventApplicantAdminService(
      EventApplicationFormRepository formRepository,
      EventApplicationRepository applicationRepository,
      UserRepository userRepository,
      AnswerCodec answerCodec,
      ApplicantCsvWriter csvWriter) {
    this(
        formRepository,
        applicationRepository,
        userRepository,
        answerCodec,
        csvWriter,
        Clock.system(ZoneId.of("Asia/Seoul")));
  }

  public EventApplicantAdminService(
      EventApplicationFormRepository formRepository,
      EventApplicationRepository applicationRepository,
      UserRepository userRepository,
      AnswerCodec answerCodec,
      ApplicantCsvWriter csvWriter,
      Clock clock) {
    this.formRepository = formRepository;
    this.applicationRepository = applicationRepository;
    this.userRepository = userRepository;
    this.answerCodec = answerCodec;
    this.csvWriter = csvWriter;
    this.clock = clock;
  }

  public Page<ApplicantResponse> listApplicants(
      Long eventBoardId, ApplicationStatus status, Pageable pageable) {
    EventApplicationForm form = findForm(eventBoardId);
    return applicationRepository
        .findApplicants(form.getId(), status, pageable)
        .map(application -> ApplicantResponse.of(application, answerCodec.readAll(application)));
  }

  /** 삭제된 질문도 컬럼으로 넣는다. 지운 뒤에도 과거 답변은 남아 있기 때문이다. */
  public byte[] exportCsv(Long eventBoardId, ApplicationStatus status) {
    EventApplicationForm form = findForm(eventBoardId);
    List<ApplicantResponse> applicants =
        applicationRepository.findAllApplicants(form.getId(), status).stream()
            .map(application -> ApplicantResponse.of(application, answerCodec.readAll(application)))
            .toList();
    return csvWriter.write(form.getQuestions(), applicants);
  }

  public String csvFileName(Long eventBoardId) {
    return findForm(eventBoardId).getEventTitle() + "_신청자.csv";
  }

  @Transactional
  public void updateAttendance(
      Long eventBoardId, Long applicationId, AttendanceUpdateRequest req) {
    EventApplication application = findApplication(eventBoardId, applicationId);
    application.markAttendance(req.status());
  }

  /**
   * 신청 없이 현장에 온 사람을 등록한다.
   *
   * <p>마감이나 정원을 넘겨도 막지 않는다. 이미 온 사람을 돌려보낼 수는 없고, 현장 판단이 폼 설정보다 우선이기 때문이다.
   */
  @Transactional
  public Long registerProxy(Long eventBoardId, ProxyApplicationRequest req) {
    EventApplicationForm form = findForm(eventBoardId);
    User user =
        userRepository
            .findById(req.userId())
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    Instant now = Instant.now(clock);
    EventApplication application =
        applicationRepository.findByFormIdAndUserId(form.getId(), req.userId()).orElse(null);

    if (application == null) {
      application = EventApplication.create(form, user, now);
      applicationRepository.save(application);
    } else if (!application.isApplied()) {
      application.reapply(now);
    } else {
      throw new BusinessException(ALREADY_APPLIED);
    }

    if (req.markAttended()) {
      application.markAttendance(EventAttendanceStatus.ATTENDED);
    }
    return application.getId();
  }

  private EventApplicationForm findForm(Long eventBoardId) {
    return formRepository
        .findByEventBoardId(eventBoardId)
        .orElseThrow(() -> new BusinessException(FORM_NOT_FOUND));
  }

  private EventApplication findApplication(Long eventBoardId, Long applicationId) {
    EventApplicationForm form = findForm(eventBoardId);
    EventApplication application =
        applicationRepository
            .findById(applicationId)
            .orElseThrow(() -> new BusinessException(APPLICATION_NOT_FOUND));
    // 다른 행사의 신청 id 를 넘겨 남의 참석을 바꾸지 못하게 한다.
    if (!application.getForm().getId().equals(form.getId())) {
      throw new BusinessException(APPLICATION_NOT_FOUND);
    }
    return application;
  }
}
