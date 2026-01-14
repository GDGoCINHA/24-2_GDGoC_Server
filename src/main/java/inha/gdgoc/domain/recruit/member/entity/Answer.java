package inha.gdgoc.domain.recruit.member.entity;

import inha.gdgoc.domain.recruit.member.enums.InputType;
import inha.gdgoc.domain.recruit.member.enums.SurveyType;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruit_member")
    private RecruitMember recruitMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "survey_type", nullable = false)
    private SurveyType surveyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false)
    private InputType inputType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") // PostgreSQL jsonb 타입 지정
    private String responseValue; // JSON 데이터를 문자열 형태로 저장

    public Answer(RecruitMember recruitMember, SurveyType surveyType, InputType inputType, String responseValue) {
        this.recruitMember = recruitMember;
        this.surveyType = surveyType;
        this.inputType = inputType;
        this.responseValue = responseValue;
    }
}
