package inha.gdgoc.domain.admin.recruit.member.service;

import inha.gdgoc.domain.admin.recruit.member.dto.request.RecruitMemberMemoOpeningNotificationRequest;
import inha.gdgoc.domain.admin.recruit.member.dto.response.RecruitMemberMemoFailedRetryResponse;
import inha.gdgoc.domain.admin.recruit.member.dto.response.RecruitMemberMemoOpeningNotificationEnqueueResponse;
import inha.gdgoc.domain.admin.recruit.member.dto.response.RecruitMemberMemoNotificationTemplateResponse;
import inha.gdgoc.domain.recruit.member.notification.service.RecruitMemberMemoNotificationEnqueueResult;
import inha.gdgoc.domain.recruit.member.notification.service.RecruitMemberMemoNotificationRetryResult;
import inha.gdgoc.domain.recruit.member.notification.service.RecruitMemberMemoNotificationService;
import inha.gdgoc.domain.recruit.member.notification.service.RecruitMemberMemoNotificationTemplateInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecruitMemberMemoAdminService {

    private final RecruitMemberMemoNotificationService notificationService;

    @Transactional(readOnly = true)
    public RecruitMemberMemoNotificationTemplateResponse getTemplate() {
        RecruitMemberMemoNotificationTemplateInfo info = notificationService.getTemplateInfoForCurrentSemester();
        return new RecruitMemberMemoNotificationTemplateResponse(
                info.semester(),
                info.defaultSubject(),
                info.defaultBody(),
                info.lastSubject(),
                info.lastBody()
        );
    }

    @Transactional
    public RecruitMemberMemoOpeningNotificationEnqueueResponse enqueueOpeningNotifications(
            RecruitMemberMemoOpeningNotificationRequest request
    ) {
        RecruitMemberMemoNotificationEnqueueResult result =
                notificationService.enqueueOpeningNotificationsForCurrentSemester(
                        request.subject(),
                        request.body()
                );
        return RecruitMemberMemoOpeningNotificationEnqueueResponse.from(result);
    }

    @Transactional
    public RecruitMemberMemoFailedRetryResponse retryFailedNotifications() {
        RecruitMemberMemoNotificationRetryResult result = notificationService.retryFailedForCurrentSemester();
        return new RecruitMemberMemoFailedRetryResponse(result.semester(), result.retriedCount());
    }
}
