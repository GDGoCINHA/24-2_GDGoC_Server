package inha.gdgoc.domain.admin.recruit.member.controller.message;

public final class RecruitMemberMemoAdminMessage {

    public static final String MEMBER_MEMO_NOTIFICATION_ENQUEUED = "신입생 지원 알림 메일 발송 작업을 큐잉했습니다.";
    public static final String MEMBER_MEMO_NOTIFICATION_TEMPLATE_RETRIEVED = "신입생 지원 알림 기본 문구를 조회했습니다.";
    public static final String MEMBER_MEMO_NOTIFICATION_FAILED_RETRIED = "신입생 지원 알림 실패 건을 재시도 큐에 반영했습니다.";

    private RecruitMemberMemoAdminMessage() {
    }
}
