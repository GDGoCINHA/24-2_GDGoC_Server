package inha.gdgoc.domain.admin.recruit.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.admin.recruit.core.dto.request.RecruitCoreApplicationAcceptRequest;
import inha.gdgoc.domain.admin.recruit.core.dto.request.RecruitCoreApplicationRejectRequest;
import inha.gdgoc.domain.admin.recruit.core.dto.response.RecruitCoreApplicationDecisionResponse;
import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import inha.gdgoc.domain.recruit.core.repository.RecruitCoreApplicationRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class RecruitCoreAdminServiceTest {

    @Mock
    private RecruitCoreApplicationRepository repository;

    @InjectMocks
    private RecruitCoreAdminService adminService;

    @Test
    void searchApplications_buildsSpecificationAndDelegates() {
        RecruitCoreApplication app = createApplication(1L, createUser(1L));
        Page<RecruitCoreApplication> page = new PageImpl<>(List.of(app));
        when(repository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        Page<RecruitCoreApplication> result = adminService.searchApplications(
            "2026-1",
            RecruitCoreResultStatus.SUBMITTED,
            TeamType.TECH,
            PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void accept_setsReviewerInfoAndUpdatesUser() {
        User user = createUser(5L);
        RecruitCoreApplication application = createApplication(100L, user);
        when(repository.findById(100L)).thenReturn(Optional.of(application));

        RecruitCoreApplicationDecisionResponse response = adminService.accept(
            100L,
            9L,
            new RecruitCoreApplicationAcceptRequest("함께 하시죠", true)
        );

        assertThat(response.resultStatus()).isEqualTo(RecruitCoreResultStatus.ACCEPTED);
        assertThat(application.getResultStatus()).isEqualTo(RecruitCoreResultStatus.ACCEPTED);
        assertThat(application.getReviewedBy()).isEqualTo(9L);
        assertThat(application.getReviewedAt()).isNotNull();
        assertThat(user.getUserRole()).isEqualTo(UserRole.CORE);
        assertThat(user.getTeam()).isEqualTo(TeamType.TECH);
    }

    @Test
    void reject_setsRejectedStatus() {
        User user = createUser(5L);
        RecruitCoreApplication application = createApplication(200L, user);
        when(repository.findById(200L)).thenReturn(Optional.of(application));

        RecruitCoreApplicationDecisionResponse response = adminService.reject(
            200L,
            8L,
            new RecruitCoreApplicationRejectRequest("죄송합니다.")
        );

        assertThat(response.resultStatus()).isEqualTo(RecruitCoreResultStatus.REJECTED);
        assertThat(application.getReviewedBy()).isEqualTo(8L);
        assertThat(application.getResultStatus()).isEqualTo(RecruitCoreResultStatus.REJECTED);
    }

    @Test
    void accept_afterDecision_throwsException() {
        User user = createUser(1L);
        RecruitCoreApplication application = createApplication(1L, user);
        application.accept(3L, "이미 처리", Instant.now());
        when(repository.findById(1L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> adminService.accept(
            1L,
            2L,
            new RecruitCoreApplicationAcceptRequest("다시", true)
        )).isInstanceOf(BusinessException.class);
    }

    private RecruitCoreApplication createApplication(Long id, User user) {
        RecruitCoreApplication application = RecruitCoreApplication.builder()
            .user(user)
            .session("2026-1")
            .name("홍길동")
            .studentId("12201234")
            .phone("01012345678")
            .major("컴퓨터공학과")
            .email("hong@inha.edu")
            .team("TECH")
            .motivation("motivation")
            .wish("wish")
            .strengths("strengths")
            .pledge("pledge")
            .fileUrls(List.of())
            .resultStatus(RecruitCoreResultStatus.SUBMITTED)
            .build();
        setId(application, id);
        setTimeStamps(application);
        return application;
    }

    private User createUser(Long id) {
        User user = User.builder()
            .name("홍길동")
            .major("컴퓨터공학과")
            .studentId("12201234")
            .phoneNumber("01012345678")
            .email("hong@inha.edu")
            .password("encoded")
            .userRole(UserRole.GUEST)
            .team(null)
            .salt(new byte[]{1})
            .image(null)
            .social(null)
            .careers(null)
            .build();
        setId(user, id);
        return user;
    }

    private void setId(Object target, Long id) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void setTimeStamps(RecruitCoreApplication application) {
        try {
            java.lang.reflect.Field created = application.getClass().getSuperclass().getDeclaredField("createdAt");
            java.lang.reflect.Field updated = application.getClass().getSuperclass().getDeclaredField("updatedAt");
            created.setAccessible(true);
            updated.setAccessible(true);
            Instant now = Instant.now();
            created.set(application, now);
            updated.set(application, now);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
