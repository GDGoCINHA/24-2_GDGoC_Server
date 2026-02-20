package inha.gdgoc.domain.recruit.member.notification.scheduler;

import inha.gdgoc.domain.recruit.member.notification.service.RecruitMemberMemoNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitMemberMemoNotificationScheduler {

    private final RecruitMemberMemoNotificationService notificationService;

    @Scheduled(fixedDelayString = "${app.recruit.member.memo.notification.fixed-delay-ms:60000}")
    public void processPendingNotifications() {
        try {
            notificationService.processPendingNotifications();
        } catch (Exception ex) {
            log.error("recruit-member-memo notification scheduler failed", ex);
        }
    }
}
