package inha.gdgoc.domain.recruit.member.notification.service;

import inha.gdgoc.domain.auth.service.MailService;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import inha.gdgoc.domain.recruit.member.notification.entity.RecruitMemberMemoNotification;
import inha.gdgoc.domain.recruit.member.notification.repository.RecruitMemberMemoNotificationRepository;
import inha.gdgoc.global.util.SemesterCalculator;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RecruitMemberMemoNotificationService {

    private static final String DISTINCT_TARGET_COUNT_SQL = """
            SELECT COUNT(*)
            FROM (
                SELECT LOWER(email) AS email
                FROM recruit_member_memo
                WHERE privacy_agreement = true
                  AND freshman_memo_agreement = true
                GROUP BY LOWER(email)
            ) t
            """;

    private static final String ENQUEUE_SQL = """
            INSERT INTO recruit_member_memo_notification
                (semester, email, subject, body, status, attempt_count, next_attempt_at, created_at, updated_at)
            SELECT ?, LOWER(email), ?, ?, 'PENDING', 0, NOW(), NOW(), NOW()
            FROM recruit_member_memo
            WHERE privacy_agreement = true
              AND freshman_memo_agreement = true
            GROUP BY LOWER(email)
            ON CONFLICT (semester, email) DO NOTHING
            """;

    private final RecruitMemberMemoNotificationRepository notificationRepository;
    private final MailService mailService;
    private final SemesterCalculator semesterCalculator;
    private final JdbcTemplate jdbcTemplate;
    private final int maxAttempts;
    private final int batchSize;
    private final String recruitFrom;
    private final String defaultSender;

    public RecruitMemberMemoNotificationService(
            RecruitMemberMemoNotificationRepository notificationRepository,
            MailService mailService,
            SemesterCalculator semesterCalculator,
            JdbcTemplate jdbcTemplate,
            @Value("${app.recruit.member.memo.notification.max-attempts:3}") int maxAttempts,
            @Value("${app.recruit.member.memo.notification.batch-size:100}") int batchSize,
            @Value("${app.mail.recruit-from:}") String recruitFrom,
            @Value("${spring.mail.username}") String defaultSender
    ) {
        this.notificationRepository = notificationRepository;
        this.mailService = mailService;
        this.semesterCalculator = semesterCalculator;
        this.jdbcTemplate = jdbcTemplate;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
        this.recruitFrom = recruitFrom;
        this.defaultSender = defaultSender;
    }

    @Transactional
    public RecruitMemberMemoNotificationEnqueueResult enqueueOpeningNotificationsForCurrentSemester(
            String subject,
            String body
    ) {
        AdmissionSemester currentSemester = semesterCalculator.currentSemester();
        String semester = currentSemester.name();
        String trimmedSubject = subject.trim();
        String trimmedBody = body.trim();

        int distinctTargetCount = Optional.ofNullable(
                jdbcTemplate.queryForObject(DISTINCT_TARGET_COUNT_SQL, Integer.class)
        ).orElse(0);

        int enqueuedCount = jdbcTemplate.update(ENQUEUE_SQL, semester, trimmedSubject, trimmedBody);
        int alreadyProcessedCount = Math.max(distinctTargetCount - enqueuedCount, 0);

        log.info(
                "recruit-member-memo enqueue done. semester={}, distinctTargets={}, enqueued={}, alreadyProcessed={}",
                semester,
                distinctTargetCount,
                enqueuedCount,
                alreadyProcessedCount
        );

        return new RecruitMemberMemoNotificationEnqueueResult(
                semester,
                distinctTargetCount,
                enqueuedCount,
                alreadyProcessedCount
        );
    }

    @Transactional(readOnly = true)
    public RecruitMemberMemoNotificationTemplateInfo getTemplateInfoForCurrentSemester() {
        String semester = semesterCalculator.currentSemester().name();
        Optional<RecruitMemberMemoNotification> latest = notificationRepository.findTopBySemesterOrderByCreatedAtDesc(
                semester
        );

        return new RecruitMemberMemoNotificationTemplateInfo(
                semester,
                RecruitMemberMemoNotificationTemplate.OPENING_SUBJECT,
                RecruitMemberMemoNotificationTemplate.OPENING_BODY,
                latest.map(RecruitMemberMemoNotification::getSubject).orElse(null),
                latest.map(RecruitMemberMemoNotification::getBody).orElse(null)
        );
    }

    @Transactional
    public RecruitMemberMemoNotificationRetryResult retryFailedForCurrentSemester() {
        String semester = semesterCalculator.currentSemester().name();
        int retriedCount = notificationRepository.retryFailedBySemester(semester, Instant.now());
        return new RecruitMemberMemoNotificationRetryResult(semester, retriedCount);
    }

    @Transactional
    public void processPendingNotifications() {
        List<RecruitMemberMemoNotification> notifications = notificationRepository.findPendingBatchForUpdate(batchSize);
        if (notifications.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        String effectiveFrom = recruitFrom == null || recruitFrom.isBlank() ? defaultSender : recruitFrom;
        for (RecruitMemberMemoNotification notification : notifications) {
            try {
                mailService.sendPlainMail(
                        notification.getEmail(),
                        notification.getSubject(),
                        notification.getBody(),
                        effectiveFrom
                );
                notification.markSent(now);
            } catch (Exception ex) {
                int nextAttemptCount = notification.getAttemptCount() + 1;
                notification.markFailed(
                        now,
                        maxAttempts,
                        retryDelay(nextAttemptCount),
                        compactErrorMessage(ex)
                );
                log.warn(
                        "recruit-member-memo send failed. id={}, email={}, attempt={}/{}, error={}",
                        notification.getId(),
                        notification.getEmail(),
                        nextAttemptCount,
                        maxAttempts,
                        ex.getMessage()
                );
            }
        }

        notificationRepository.saveAll(notifications);
    }

    private Duration retryDelay(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            default -> Duration.ofMinutes(30);
        };
    }

    private String compactErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
