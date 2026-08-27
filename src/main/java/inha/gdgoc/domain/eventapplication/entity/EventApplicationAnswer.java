package inha.gdgoc.domain.eventapplication.entity;

import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 질문 하나에 대한 답변.
 *
 * <p>유형마다 형태가 달라 JSON 문자열로 저장한다 — 단답은 {@code "홍길동"}, 복수선택은 {@code ["A","C"]}, 동의는 {@code true}. 지원서
 * {@code Answer} 가 이미 같은 방식으로 돌아가고 있다.
 */
@Entity
@Table(
    name = "event_application_answer",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_event_answer_application_question",
            columnNames = {"application_id", "question_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventApplicationAnswer extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id", nullable = false)
  private EventApplication application;

  @Column(name = "question_id", nullable = false)
  private Long questionId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "value", nullable = false, columnDefinition = "jsonb")
  private String value;

  public static EventApplicationAnswer create(
      EventApplication application, Long questionId, String value) {
    EventApplicationAnswer answer = new EventApplicationAnswer();
    answer.application = application;
    answer.questionId = questionId;
    answer.value = value;
    return answer;
  }
}
