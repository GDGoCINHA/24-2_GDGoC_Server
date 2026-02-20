package inha.gdgoc.domain.recruit.member.notification.entity;

import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recruit_member_memo_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecruitMemberMemoNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "semester", nullable = false, length = 16)
    private String semester;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "body", nullable = false, length = 5000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecruitMemberMemoNotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    public void markSent(Instant now) {
        this.status = RecruitMemberMemoNotificationStatus.SENT;
        this.sentAt = now;
        this.nextAttemptAt = now;
        this.lastError = null;
    }

    public void markFailed(Instant now, int maxAttempts, Duration retryDelay, String errorMessage) {
        this.attemptCount += 1;
        this.lastError = errorMessage;

        if (this.attemptCount >= maxAttempts) {
            this.status = RecruitMemberMemoNotificationStatus.FAILED;
            this.nextAttemptAt = now;
            return;
        }

        this.status = RecruitMemberMemoNotificationStatus.PENDING;
        this.nextAttemptAt = now.plus(retryDelay);
    }

    public void retry(Instant now) {
        this.status = RecruitMemberMemoNotificationStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
    }
}
