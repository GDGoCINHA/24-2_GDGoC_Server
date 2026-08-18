package inha.gdgoc.domain.recruit.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.recruit.core.config.RecruitCoreSessionResolver;
import inha.gdgoc.domain.recruit.core.dto.request.RecruitCoreApplicationCreateRequest;
import inha.gdgoc.domain.recruit.core.dto.request.RecruitCoreApplicationCreateRequest.RecruitCoreApplicationSnapshotRequest;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreApplicantDetailResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreEligibilityResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreApplicationCreateResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreMyApplicationResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCorePeriodResponse;
import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCorePeriodStatus;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreAlreadyAppliedException;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreApplicationNotFoundException;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreClosedException;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreNotOpenException;
import inha.gdgoc.domain.recruit.core.repository.RecruitCoreApplicationRepository;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.util.MajorNormalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecruitCoreApplicationServiceTest {

    private static final String SESSION = "2026-1";

    // 기간은 테스트가 스스로 정한다. 운영 기본값(app.recruit.core.*)을 참조하면
    // 모집 일정이 바뀔 때마다 이 클래스가 다시 무너진다 — 실제로 그랬다.
    private static final Instant OPEN_AT = Instant.parse("2026-08-09T15:00:00Z");
    private static final Instant CLOSE_AT = Instant.parse("2026-08-30T14:59:59Z");

    private static final Instant BEFORE_OPEN = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant DURING_OPEN = Instant.parse("2026-08-20T00:00:00Z");
    private static final Instant AFTER_CLOSE = Instant.parse("2026-09-01T00:00:00Z");

    @Mock
    private RecruitCoreApplicationRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecruitCoreSessionResolver recruitCoreSessionResolver;

    @Mock
    private MajorNormalizer majorNormalizer;

    @Mock
    private S3Service s3Service;

    private RecruitCoreApplicationService service;

    @BeforeEach
    void setUp() {
        lenient().when(recruitCoreSessionResolver.currentSession()).thenReturn(SESSION);
        service = serviceAt(DURING_OPEN);
    }

    private RecruitCoreApplicationService serviceAt(Instant now) {
        return new RecruitCoreApplicationService(
            repository,
            userRepository,
            recruitCoreSessionResolver,
            majorNormalizer,
            s3Service,
            OPEN_AT,
            CLOSE_AT,
            Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void checkEligibility_whenNoApplication_returnsEligible() {
        when(repository.findByUserIdAndSession(1L, SESSION)).thenReturn(Optional.empty());

        RecruitCoreEligibilityResponse response = service.checkEligibility(1L);

        assertThat(response.eligible()).isTrue();
        assertThat(response.session()).isEqualTo(SESSION);
        assertThat(response.applicationId()).isNull();
    }

    @Test
    void checkEligibility_whenApplicationExists_returnsIneligible() {
        RecruitCoreApplication existing = createApplication(10L, createUser(1L), SESSION);
        when(repository.findByUserIdAndSession(1L, SESSION)).thenReturn(Optional.of(existing));

        RecruitCoreEligibilityResponse response = service.checkEligibility(1L);

        assertThat(response.eligible()).isFalse();
        assertThat(response.reason()).isEqualTo("ALREADY_APPLIED");
        assertThat(response.applicationId()).isEqualTo(10L);
    }

    @Test
    void submit_whenEligible_savesApplication() {
        RecruitCoreApplicationCreateRequest request = sampleRequest();
        User user = createUser(1L);
        RecruitCoreApplication saved = createApplication(55L, user, SESSION);
        when(repository.findByUserIdAndSession(1L, SESSION)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(majorNormalizer.normalize("컴퓨터공학과")).thenReturn("컴퓨터공학과");
        when(repository.save(any())).thenReturn(saved);

        RecruitCoreApplicationCreateResponse response = service.submit(1L, request);

        assertThat(response.applicationId()).isEqualTo(55L);
        assertThat(response.session()).isEqualTo(SESSION);
        assertThat(response.resultStatus()).isEqualTo(RecruitCoreResultStatus.SUBMITTED);
        assertThat(response.submittedAt()).isNotNull();

        ArgumentCaptor<RecruitCoreApplication> captor =
            ArgumentCaptor.forClass(RecruitCoreApplication.class);
        verify(repository).save(captor.capture());
        RecruitCoreApplication toSave = captor.getValue();
        assertThat(toSave.getUser()).isEqualTo(user);
        assertThat(toSave.getSession()).isEqualTo(SESSION);
        assertThat(toSave.getTeam()).isEqualTo("TECH");
        assertThat(toSave.getFileUrls()).containsExactly("https://file");
    }

    @Test
    void submit_whenAlreadyApplied_throwsException() {
        RecruitCoreApplication existing = createApplication(77L, createUser(1L), SESSION);
        when(repository.findByUserIdAndSession(1L, SESSION)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.submit(1L, sampleRequest()))
            .isInstanceOf(RecruitCoreAlreadyAppliedException.class);
    }

    @Test
    void getMyApplication_whenExists_returnsResponse() {
        RecruitCoreApplication existing = createApplication(33L, createUser(1L), SESSION);
        when(repository.findByUserIdAndSession(1L, SESSION)).thenReturn(Optional.of(existing));

        RecruitCoreMyApplicationResponse response = service.getMyApplication(1L);

        assertThat(response.applicationId()).isEqualTo(33L);
        assertThat(response.session()).isEqualTo(SESSION);
        assertThat(response.team()).isEqualTo("TECH");
    }

    @Test
    void getMyApplication_whenMissing_throwsException() {
        when(repository.findByUserIdAndSession(1L, SESSION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyApplication(1L))
            .isInstanceOf(RecruitCoreApplicationNotFoundException.class);
    }

    @Test
    void getApplicantDetailForViewer_whenOwnerAlllowed() {
        RecruitCoreApplication application = createApplication(99L, createUser(1L), SESSION);
        when(repository.findById(99L)).thenReturn(Optional.of(application));

        RecruitCoreApplicantDetailResponse detail =
            service.getApplicantDetailForViewer(99L, 1L, UserRole.MEMBER);

        assertThat(detail.applicationId()).isEqualTo(99L);
    }

    // 본인도 자기 지원서를 마이페이지에서 연다. 검토자·내부 메모까지 실어 보내면
    // 화면에서 안 그려도 개발자 도구로 읽힌다.
    @Test
    void getApplicantDetailForViewer_whenOwner_hidesReview() {
        RecruitCoreApplication application = createApplication(99L, createUser(1L), SESSION);
        ReflectionTestUtils.setField(application, "reviewedBy", 7L);
        ReflectionTestUtils.setField(application, "resultNote", "면접 태도 미흡");
        when(repository.findById(99L)).thenReturn(Optional.of(application));

        RecruitCoreApplicantDetailResponse detail =
            service.getApplicantDetailForViewer(99L, 1L, UserRole.MEMBER);

        assertThat(detail.review()).isNull();
    }

    @Test
    void getApplicantDetailForViewer_whenPrivileged_keepsReview() {
        RecruitCoreApplication application = createApplication(99L, createUser(2L), SESSION);
        ReflectionTestUtils.setField(application, "resultNote", "면접 태도 미흡");
        when(repository.findById(99L)).thenReturn(Optional.of(application));

        RecruitCoreApplicantDetailResponse detail =
            service.getApplicantDetailForViewer(99L, 1L, UserRole.LEAD);

        assertThat(detail.review().resultNote()).isEqualTo("면접 태도 미흡");
    }

    @Test
    void getApplicantDetailForViewer_whenUnauthorized_throwsException() {
        RecruitCoreApplication application = createApplication(99L, createUser(2L), SESSION);
        when(repository.findById(99L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.getApplicantDetailForViewer(99L, 1L, UserRole.MEMBER))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void prefill_returnsUserSnapshot() {
        User user = createUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var response = service.prefill(1L);

        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.email()).isEqualTo("hong@inha.edu");
    }

    @Test
    void checkEligibility_afterDeadline_throwsClosedException() {
        assertThatThrownBy(() -> serviceAt(AFTER_CLOSE).checkEligibility(1L))
            .isInstanceOf(RecruitCoreClosedException.class);
    }

    @Test
    void prefill_afterDeadline_throwsClosedException() {
        assertThatThrownBy(() -> serviceAt(AFTER_CLOSE).prefill(1L))
            .isInstanceOf(RecruitCoreClosedException.class);
    }

    @Test
    void submit_afterDeadline_throwsClosedException() {
        assertThatThrownBy(() -> serviceAt(AFTER_CLOSE).submit(1L, sampleRequest()))
            .isInstanceOf(RecruitCoreClosedException.class);
    }

    @Test
    void checkEligibility_beforeOpen_throwsNotOpen() {
        assertThatThrownBy(() -> serviceAt(BEFORE_OPEN).checkEligibility(1L))
            .isInstanceOf(RecruitCoreNotOpenException.class);
    }

    @Test
    void prefill_beforeOpen_throwsNotOpen() {
        assertThatThrownBy(() -> serviceAt(BEFORE_OPEN).prefill(1L))
            .isInstanceOf(RecruitCoreNotOpenException.class);
    }

    @Test
    void submit_beforeOpen_throwsNotOpen() {
        assertThatThrownBy(() -> serviceAt(BEFORE_OPEN).submit(1L, sampleRequest()))
            .isInstanceOf(RecruitCoreNotOpenException.class);
    }

    @Test
    void getPeriodStatus_beforeOpen_returnsBeforeOpen() {
        assertThat(serviceAt(BEFORE_OPEN).getPeriodStatus())
            .isEqualTo(RecruitCorePeriodStatus.BEFORE_OPEN);
    }

    @Test
    void getPeriodStatus_duringOpen_returnsOpen() {
        assertThat(serviceAt(DURING_OPEN).getPeriodStatus())
            .isEqualTo(RecruitCorePeriodStatus.OPEN);
    }

    @Test
    void getPeriodStatus_afterClose_returnsClosed() {
        assertThat(serviceAt(AFTER_CLOSE).getPeriodStatus())
            .isEqualTo(RecruitCorePeriodStatus.CLOSED);
    }

    @Test
    void getPeriod_returnsSessionAndBoundsAndStatus() {
        RecruitCorePeriodResponse response = serviceAt(DURING_OPEN).getPeriod();

        assertThat(response.session()).isEqualTo(SESSION);
        assertThat(response.openAt()).isEqualTo(OPEN_AT);
        assertThat(response.closeAt()).isEqualTo(CLOSE_AT);
        assertThat(response.status()).isEqualTo(RecruitCorePeriodStatus.OPEN);
    }

    @Test
    void getPeriod_beforeOpen_doesNotThrow() {
        // 기간 조회는 게이트 앞에 있다. 시작 전에도 200 이어야 웹이 로그인을
        // 요구하지 않고 안내를 띄울 수 있다.
        RecruitCorePeriodResponse response = serviceAt(BEFORE_OPEN).getPeriod();

        assertThat(response.status()).isEqualTo(RecruitCorePeriodStatus.BEFORE_OPEN);
    }

    @Test
    void constructor_whenOpenAtNotBeforeCloseAt_throws() {
        assertThatThrownBy(() -> new RecruitCoreApplicationService(
            repository, userRepository, recruitCoreSessionResolver, majorNormalizer, s3Service,
            CLOSE_AT, OPEN_AT, Clock.fixed(DURING_OPEN, ZoneOffset.UTC)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("open-at");
    }

    private RecruitCoreApplicationCreateRequest sampleRequest() {
        RecruitCoreApplicationSnapshotRequest snapshot =
            new RecruitCoreApplicationSnapshotRequest(
                "홍길동", "12201234", "01012345678", "컴퓨터공학과", "hong@inha.edu");
        return new RecruitCoreApplicationCreateRequest(
            snapshot,
            "TECH",
            "motivation",
            "wish",
            "strengths",
            "pledge",
            List.of("https://file"));
    }

    private User createUser(Long id) {
        User user = User.builder()
            .name("홍길동")
            .major("컴퓨터공학과")
            .studentId("12201234")
            .phoneNumber("01012345678")
            .email("hong@inha.edu")
            .userRole(UserRole.GUEST)
            .team(null)
            .image(null)
            .social(null)
            .careers(null)
            .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private RecruitCoreApplication createApplication(Long id, User user, String session) {
        RecruitCoreApplication application = RecruitCoreApplication.builder()
            .user(user)
            .session(session)
            .name("홍길동")
            .studentId("12201234")
            .phone("01012345678")
            .major("컴퓨터공학과")
            .email(user.getEmail())
            .team("TECH")
            .motivation("motivation")
            .wish("wish")
            .strengths("strengths")
            .pledge("pledge")
            .fileUrls(List.of())
            .resultStatus(RecruitCoreResultStatus.SUBMITTED)
            .build();
        ReflectionTestUtils.setField(application, "id", id);
        ReflectionTestUtils.setField(application, "createdAt", Instant.now());
        ReflectionTestUtils.setField(application, "updatedAt", Instant.now());
        return application;
    }
}
