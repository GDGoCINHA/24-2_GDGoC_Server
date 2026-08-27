package inha.gdgoc.domain.eventapplication.entity;

import inha.gdgoc.domain.eventapplication.enums.QuestionType;
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
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 신청 폼의 질문 하나.
 *
 * <p>{@code visibleWhenQuestionId} 가 있으면 조건부 표시다 — 기준 질문의 답이 {@code visibleWhenValues} 중 하나일 때만 보인다.
 * 기준 질문은 반드시 자기보다 {@code sortOrder} 가 작아야 하며, 그 규칙 하나로 순환 참조가 구조적으로 불가능해진다.
 *
 * <p>기준 질문을 엔티티가 아니라 id 로 들고 있는 것은 의도다. 조건을 평가할 때는 같은 폼의 질문 목록에서 찾으면 되고, 자기 참조 연관을 만들면 로딩만 복잡해진다.
 */
@Entity
@Table(name = "event_form_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventFormQuestion extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "form_id", nullable = false)
  private EventApplicationForm form;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 24)
  private QuestionType type;

  @Column(name = "label", nullable = false, length = 255)
  private String label;

  @Column(name = "help_text", length = 500)
  private String helpText;

  @Column(name = "is_required", nullable = false)
  private boolean isRequired;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "options", columnDefinition = "jsonb")
  private List<QuestionOption> options;

  @Column(name = "visible_when_question_id")
  private Long visibleWhenQuestionId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "visible_when_values", columnDefinition = "jsonb")
  private List<String> visibleWhenValues;

  @Column(name = "is_deleted", nullable = false)
  private boolean isDeleted;

  public static EventFormQuestion create(
      EventApplicationForm form,
      QuestionType type,
      String label,
      String helpText,
      boolean isRequired,
      int sortOrder,
      List<QuestionOption> options,
      Long visibleWhenQuestionId,
      List<String> visibleWhenValues) {
    EventFormQuestion question = new EventFormQuestion();
    question.form = form;
    question.type = type;
    question.label = label;
    question.helpText = helpText;
    question.isRequired = isRequired;
    question.sortOrder = sortOrder;
    question.options = options;
    question.visibleWhenQuestionId = visibleWhenQuestionId;
    question.visibleWhenValues = visibleWhenValues;
    question.isDeleted = false;
    return question;
  }

  /** 문구·도움말·필수 여부를 바꾼다. 신청이 들어온 뒤에도 허용되는 범위다. */
  public void updateContent(String label, String helpText, Boolean isRequired) {
    if (label != null) this.label = label;
    if (helpText != null) this.helpText = helpText;
    if (isRequired != null) this.isRequired = isRequired;
  }

  /** 유형과 선택지를 바꾼다. 신청이 한 건이라도 있으면 서비스가 미리 막는다. */
  public void updateShape(QuestionType type, List<QuestionOption> options) {
    if (type != null) this.type = type;
    if (options != null) this.options = options;
  }

  public void updateSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  /** 표시 조건을 바꾼다. 기준 질문이 null 이면 조건을 없앤다. */
  public void updateCondition(Long visibleWhenQuestionId, List<String> visibleWhenValues) {
    this.visibleWhenQuestionId = visibleWhenQuestionId;
    this.visibleWhenValues = visibleWhenQuestionId == null ? null : visibleWhenValues;
  }

  /** 지운 표시만 한다. 기존 답변이 고아가 되지 않도록. */
  public void softDelete() {
    this.isDeleted = true;
  }

  public boolean hasCondition() {
    return this.visibleWhenQuestionId != null;
  }

  public boolean referencesOptionValue(String value) {
    return this.visibleWhenValues != null && this.visibleWhenValues.contains(value);
  }
}
