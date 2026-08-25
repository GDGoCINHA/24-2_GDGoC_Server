package inha.gdgoc.domain.recruit.common.entity;

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
}
