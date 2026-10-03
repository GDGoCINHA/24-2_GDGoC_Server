package inha.gdgoc.domain.club.completion.entity;

import inha.gdgoc.domain.club.club.entity.Club;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 쉬는 주. {@code weekStart} 는 그 주의 월요일이다.
 */
@Entity
@Table(name = "club_rest_week")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubRestWeek extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false)
  private Club club;

  @Column(name = "week_start", nullable = false)
  private LocalDate weekStart;

  /** {@code weekStart} 는 부르는 쪽이 월요일로 맞춰서 넘긴다. */
  public static ClubRestWeek create(Club club, LocalDate weekStart) {
    ClubRestWeek restWeek = new ClubRestWeek();
    restWeek.club = club;
    restWeek.weekStart = weekStart;
    return restWeek;
  }
}
