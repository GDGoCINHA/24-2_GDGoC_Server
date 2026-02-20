package inha.gdgoc.domain.recruit.member.notification.repository;

import inha.gdgoc.domain.recruit.member.notification.entity.RecruitMemberMemoNotification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruitMemberMemoNotificationRepository extends JpaRepository<RecruitMemberMemoNotification, Long> {

    @Query(
            value = """
            SELECT *
            FROM recruit_member_memo_notification
            WHERE status = 'PENDING'
              AND next_attempt_at <= NOW()
            ORDER BY next_attempt_at ASC, id ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """,
            nativeQuery = true
    )
    List<RecruitMemberMemoNotification> findPendingBatchForUpdate(@Param("batchSize") int batchSize);

    Optional<RecruitMemberMemoNotification> findTopBySemesterOrderByCreatedAtDesc(String semester);

    @Modifying
    @Query(
            """
            UPDATE RecruitMemberMemoNotification n
               SET n.status = inha.gdgoc.domain.recruit.member.notification.entity.RecruitMemberMemoNotificationStatus.PENDING,
                   n.attemptCount = 0,
                   n.nextAttemptAt = :now
             WHERE n.semester = :semester
               AND n.status = inha.gdgoc.domain.recruit.member.notification.entity.RecruitMemberMemoNotificationStatus.FAILED
            """
    )
    int retryFailedBySemester(@Param("semester") String semester, @Param("now") java.time.Instant now);
}
