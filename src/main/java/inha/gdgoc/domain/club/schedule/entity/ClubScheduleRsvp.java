package inha.gdgoc.domain.club.schedule.entity;

import inha.gdgoc.domain.club.schedule.enums.ClubRsvp;
import inha.gdgoc.domain.user.entity.User;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참석 예정 응답. 실제 출석과 무관하다.
 */
@Entity
@Table(name = "club_schedule_rsvp")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubScheduleRsvp extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "schedule_id", nullable = false)
  private ClubSchedule schedule;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "response", nullable = false, length = 16)
  private ClubRsvp response;
}
