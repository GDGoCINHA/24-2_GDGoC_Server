package inha.gdgoc.domain.club.schedule.entity;

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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모임 일정. 등록은 선택이다.
 */
@Entity
@Table(name = "club_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubSchedule extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false)
  private Club club;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "location", length = 200)
  private String location;

  @Column(name = "online_link", length = 500)
  private String onlineLink;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "created_by")
  private Long createdBy;

  public static ClubSchedule create(
      Club club,
      String title,
      Instant startsAt,
      String location,
      String onlineLink,
      String description,
      Long creatorId) {
    ClubSchedule schedule = new ClubSchedule();
    schedule.club = club;
    schedule.title = title;
    schedule.startsAt = startsAt;
    schedule.location = location;
    schedule.onlineLink = onlineLink;
    schedule.description = description;
    schedule.createdBy = creatorId;
    return schedule;
  }

  public void update(
      String title, Instant startsAt, String location, String onlineLink, String description) {
    this.title = title;
    this.startsAt = startsAt;
    this.location = location;
    this.onlineLink = onlineLink;
    this.description = description;
  }
}
