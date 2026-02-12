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
import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreAlreadyAppliedException;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreApplicationNotFoundException;
import inha.gdgoc.domain.recruit.core.repository.RecruitCoreApplicationRepository;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecruitCoreApplicationServiceTest {

    private static final String SESSION = "2026-1";

    @Mock
    private RecruitCoreApplicationRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecruitCoreSessionResolver recruitCoreSessionResolver;

    @InjectMocks
    private RecruitCoreApplicationService service;

    @BeforeEach
    void setUp() {
        lenient().when(recruitCoreSessionResolver.currentSession()).thenReturn(SESSION);
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
