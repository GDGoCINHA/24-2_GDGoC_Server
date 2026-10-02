package inha.gdgoc.domain.club.activity.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 출석. 행 = 활동일 당시 팀 명단이고, 행 수가 참석 비율의 분모다.
 */
@Entity
@Table(name = "club_activity_attendance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubActivityAttendance extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "activity_id", nullable = false)
  private ClubActivity activity;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "attended", nullable = false)
  private boolean attended;
}
