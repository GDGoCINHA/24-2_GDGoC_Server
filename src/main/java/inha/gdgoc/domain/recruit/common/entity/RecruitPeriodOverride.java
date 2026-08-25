package inha.gdgoc.domain.recruit.common.entity;

import inha.gdgoc.domain.recruit.common.dto.RecruitScheduleNotice;
import inha.gdgoc.domain.recruit.common.enums.RecruitType;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 설정값을 덮어쓰는 모집 기간. 종류마다 최대 한 행이다. */
@Entity
@Table(name = "recruit_period_override")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitPeriodOverride extends BaseEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "recruit_type", length = 16)
    private RecruitType recruitType;

    @Column(name = "open_at", nullable = false)
    private Instant openAt;

    @Column(name = "close_at", nullable = false)
    private Instant closeAt;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    // 아래는 화면에 보여주는 안내 일정이다. 지원 창구를 여닫는 값이 아니라서 전부 비어도 된다.
    // 종류마다 쓰는 칸이 다르다 — CORE 는 서류·면접·최종, MEMBER 는 집중 모집 기간.

    @Column(name = "document_result_at")
    private Instant documentResultAt;

    @Column(name = "interview_open_at")
    private Instant interviewOpenAt;

    @Column(name = "interview_close_at")
    private Instant interviewCloseAt;

    @Column(name = "final_result_at")
    private Instant finalResultAt;

    @Column(name = "interview_note", length = 300)
    private String interviewNote;

    @Column(name = "meeting_note", length = 300)
    private String meetingNote;

    @Column(name = "intensive_open_at")
    private Instant intensiveOpenAt;

    @Column(name = "intensive_close_at")
    private Instant intensiveCloseAt;

    public static RecruitPeriodOverride create(
        RecruitType recruitType, Instant openAt, Instant closeAt, Long updatedBy) {
        RecruitPeriodOverride override = new RecruitPeriodOverride();
        override.recruitType = recruitType;
        override.apply(openAt, closeAt, updatedBy);
        return override;
    }

    public void apply(Instant openAt, Instant closeAt, Long updatedBy) {
        this.openAt = openAt;
        this.closeAt = closeAt;
        this.updatedBy = updatedBy;
    }

    /** 안내 일정을 통째로 덮어쓴다. 비운 칸은 비운 채로 저장한다 — 화면에서 지웠다는 뜻이다. */
    public void applyNotice(RecruitScheduleNotice notice) {
        this.documentResultAt = notice.documentResultAt();
        this.interviewOpenAt = notice.interviewOpenAt();
        this.interviewCloseAt = notice.interviewCloseAt();
        this.finalResultAt = notice.finalResultAt();
        this.interviewNote = notice.interviewNote();
        this.meetingNote = notice.meetingNote();
        this.intensiveOpenAt = notice.intensiveOpenAt();
        this.intensiveCloseAt = notice.intensiveCloseAt();
    }

    public RecruitScheduleNotice toNotice() {
        return new RecruitScheduleNotice(
            documentResultAt,
            interviewOpenAt,
            interviewCloseAt,
            finalResultAt,
            interviewNote,
            meetingNote,
            intensiveOpenAt,
            intensiveCloseAt);
    }
}
