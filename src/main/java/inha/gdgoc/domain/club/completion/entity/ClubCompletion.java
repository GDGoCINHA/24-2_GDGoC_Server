package inha.gdgoc.domain.club.completion.entity;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionStatus;
import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
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
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 목표·완주. 소모임당 한 행이며 처음 쓸 때 만든다.
 *
 * <p>계산값은 참고용이다. 최종 완주는 운영진이 확정하며 미충족이어도 확정할 수 있다.
 */
@Entity
@Table(name = "club_completion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubCompletion extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false, unique = true)
  private Club club;

  @Column(name = "goal", columnDefinition = "TEXT")
  private String goal;

  @Column(name = "goal_criteria", columnDefinition = "TEXT")
  private String goalCriteria;

  @Column(name = "goal_result", columnDefinition = "TEXT")
  private String goalResult;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "goal_evidence_urls", columnDefinition = "jsonb")
  private List<String> goalEvidenceUrls;

  @Enumerated(EnumType.STRING)
  @Column(name = "goal_status", nullable = false, length = 16)
  private ClubGoalStatus goalStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "completion_status", nullable = false, length = 16)
  private ClubCompletionStatus completionStatus;

  @Column(name = "completion_memo", length = 1000)
  private String completionMemo;

  @Column(name = "confirmed_by")
  private Long confirmedBy;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;
}
