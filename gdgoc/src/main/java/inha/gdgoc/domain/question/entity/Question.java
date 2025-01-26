package inha.gdgoc.domain.question.entity;

import inha.gdgoc.domain.question.enums.InputType;
import inha.gdgoc.domain.question.enums.SurveyType;
import inha.gdgoc.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SurveyType surveyType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_type", nullable = false)
    private InputType dataType;

    @Column(name = "order", nullable = false)
    private int order;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed;
}
