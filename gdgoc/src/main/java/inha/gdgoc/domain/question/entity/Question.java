package inha.gdgoc.domain.question.entity;

import inha.gdgoc.domain.question.enums.InputType;
import inha.gdgoc.domain.question.enums.SurveyType;
import inha.gdgoc.domain.recruit.entity.Answer;
import inha.gdgoc.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false)
    private InputType inputType;

    @Column(name = "order", nullable = false)
    private int order;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = true, columnDefinition = "Text")
    private String description;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed;

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL)
    private Answer answer;
}
