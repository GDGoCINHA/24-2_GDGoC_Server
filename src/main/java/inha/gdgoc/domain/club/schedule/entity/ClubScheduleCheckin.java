package inha.gdgoc.domain.club.schedule.entity;

import inha.gdgoc.domain.user.entity.User;
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
 * QR 출석. 활동 기록 작성 시 출석 체크의 기본값으로만 쓴다.
 */
@Entity
@Table(name = "club_schedule_checkin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubScheduleCheckin extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "schedule_id", nullable = false)
  private ClubSchedule schedule;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "checked_at", nullable = false)
  private Instant checkedAt;

  public static ClubScheduleCheckin of(ClubSchedule schedule, User user, Instant checkedAt) {
    ClubScheduleCheckin checkin = new ClubScheduleCheckin();
    checkin.schedule = schedule;
    checkin.user = user;
    checkin.checkedAt = checkedAt;
    return checkin;
  }
}
