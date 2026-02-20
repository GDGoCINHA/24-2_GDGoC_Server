package inha.gdgoc.domain.recruit.member.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.auth.service.MailService;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import inha.gdgoc.domain.recruit.member.notification.entity.RecruitMemberMemoNotification;
import inha.gdgoc.domain.recruit.member.notification.entity.RecruitMemberMemoNotificationStatus;
import inha.gdgoc.domain.recruit.member.notification.repository.RecruitMemberMemoNotificationRepository;
import inha.gdgoc.global.util.SemesterCalculator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RecruitMemberMemoNotificationServiceTest {

    @Mock
    private RecruitMemberMemoNotificationRepository notificationRepository;

    @Mock
    private MailService mailService;

    @Mock
    private SemesterCalculator semesterCalculator;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void enqueueOpeningNotifications_returnsExpectedCounts() {
        RecruitMemberMemoNotificationService service = new RecruitMemberMemoNotificationService(
                notificationRepository,
                mailService,
                semesterCalculator,
                jdbcTemplate,
                3,
                100,
                "recruit@gdgocinha.com",
                "sender@test.com"
        );

        when(semesterCalculator.currentSemester()).thenReturn(AdmissionSemester.Y26_1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(5);
        when(jdbcTemplate.update(anyString(), eq("Y26_1"), eq("subject"), eq("body"))).thenReturn(3);

        RecruitMemberMemoNotificationEnqueueResult result =
                service.enqueueOpeningNotificationsForCurrentSemester("subject", "body");

        assertThat(result.semester()).isEqualTo("Y26_1");
        assertThat(result.distinctTargetCount()).isEqualTo(5);
        assertThat(result.enqueuedCount()).isEqualTo(3);
        assertThat(result.alreadyProcessedCount()).isEqualTo(2);
    }

    @Test
    void processPendingNotifications_marksSentAndFailed() {
        RecruitMemberMemoNotificationService service = new RecruitMemberMemoNotificationService(
                notificationRepository,
                mailService,
                semesterCalculator,
                jdbcTemplate,
                3,
                100,
                "recruit@gdgocinha.com",
                "sender@test.com"
        );

        RecruitMemberMemoNotification success = RecruitMemberMemoNotification.builder()
                .id(1L)
                .semester("Y26_1")
                .email("ok@test.com")
                .subject("s")
                .body("b")
                .status(RecruitMemberMemoNotificationStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(Instant.now())
                .build();

        RecruitMemberMemoNotification fail = RecruitMemberMemoNotification.builder()
                .id(2L)
                .semester("Y26_1")
                .email("fail@test.com")
                .subject("s")
                .body("b")
                .status(RecruitMemberMemoNotificationStatus.PENDING)
                .attemptCount(2)
                .nextAttemptAt(Instant.now())
                .build();

        when(notificationRepository.findPendingBatchForUpdate(anyInt())).thenReturn(List.of(success, fail));
        doThrow(new RuntimeException("smtp error"))
                .when(mailService)
                .sendPlainMail(eq("fail@test.com"), anyString(), anyString(), anyString());

        service.processPendingNotifications();

        ArgumentCaptor<List<RecruitMemberMemoNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<RecruitMemberMemoNotification> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getStatus()).isEqualTo(RecruitMemberMemoNotificationStatus.SENT);
        assertThat(saved.get(0).getSentAt()).isNotNull();
        assertThat(saved.get(1).getStatus()).isEqualTo(RecruitMemberMemoNotificationStatus.FAILED);
        assertThat(saved.get(1).getAttemptCount()).isEqualTo(3);
        assertThat(saved.get(1).getLastError()).contains("smtp error");
    }

    @Test
    void getTemplateInfoForCurrentSemester_prefersLastMessage() {
        RecruitMemberMemoNotificationService service = new RecruitMemberMemoNotificationService(
                notificationRepository,
                mailService,
                semesterCalculator,
                jdbcTemplate,
                3,
                100,
                "recruit@gdgocinha.com",
                "sender@test.com"
        );
        RecruitMemberMemoNotification latest = RecruitMemberMemoNotification.builder()
                .semester("Y26_1")
                .email("a@test.com")
                .subject("latest-subject")
                .body("latest-body")
                .status(RecruitMemberMemoNotificationStatus.SENT)
                .attemptCount(0)
                .nextAttemptAt(Instant.now())
                .build();

        when(semesterCalculator.currentSemester()).thenReturn(AdmissionSemester.Y26_1);
        when(notificationRepository.findTopBySemesterOrderByCreatedAtDesc("Y26_1"))
                .thenReturn(Optional.of(latest));

        RecruitMemberMemoNotificationTemplateInfo info = service.getTemplateInfoForCurrentSemester();

        assertThat(info.semester()).isEqualTo("Y26_1");
        assertThat(info.lastSubject()).isEqualTo("latest-subject");
        assertThat(info.lastBody()).isEqualTo("latest-body");
    }

    @Test
    void retryFailedForCurrentSemester_returnsRetriedCount() {
        RecruitMemberMemoNotificationService service = new RecruitMemberMemoNotificationService(
                notificationRepository,
                mailService,
                semesterCalculator,
                jdbcTemplate,
                3,
                100,
                "recruit@gdgocinha.com",
                "sender@test.com"
        );
        when(semesterCalculator.currentSemester()).thenReturn(AdmissionSemester.Y26_1);
        when(notificationRepository.retryFailedBySemester(eq("Y26_1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(4);

        RecruitMemberMemoNotificationRetryResult result = service.retryFailedForCurrentSemester();

        assertThat(result.semester()).isEqualTo("Y26_1");
        assertThat(result.retriedCount()).isEqualTo(4);
    }
}
