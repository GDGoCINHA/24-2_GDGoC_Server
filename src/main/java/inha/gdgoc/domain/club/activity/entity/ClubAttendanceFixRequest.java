package inha.gdgoc.domain.club.activity.entity;

import inha.gdgoc.domain.club.activity.enums.ClubFixRequestStatus;
import inha.gdgoc.domain.club.activity.exception.ClubActivityErrorCode;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.entity.BaseEntity;
import inha.gdgoc.global.exception.BusinessException;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멤버의 출석 수정 요청.
 */
@Entity
@Table(name = "club_attendance_fix_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubAttendanceFixRequest extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "activity_id", nullable = false)
  private ClubActivity activity;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "reason", length = 1000)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private ClubFixRequestStatus status;

  @Column(name = "handled_at")
  private Instant handledAt;

  /** 요청한 출석 값. 수락하면 출석을 이 값으로 맞춘다 — "반전" 이 아니라서 그 사이 리더가 고쳤어도 다시 뒤집히지 않는다. */
  @Column(name = "requested_attended", nullable = false)
  private boolean requestedAttended;

  public static ClubAttendanceFixRequest create(
      ClubActivity activity, User user, boolean requestedAttended, String reason) {
    ClubAttendanceFixRequest request = new ClubAttendanceFixRequest();
    request.activity = activity;
    request.user = user;
    request.requestedAttended = requestedAttended;
    request.reason = reason;
    request.status = ClubFixRequestStatus.PENDING;
    return request;
  }

  public boolean isPending() {
    return status == ClubFixRequestStatus.PENDING;
  }

  public void accept(Instant now) {
    handle(ClubFixRequestStatus.ACCEPTED, now);
  }

  public void reject(Instant now) {
    handle(ClubFixRequestStatus.REJECTED, now);
  }

  private void handle(ClubFixRequestStatus result, Instant now) {
    if (!isPending()) {
      throw new BusinessException(ClubActivityErrorCode.FIX_REQUEST_ALREADY_HANDLED);
    }
    this.status = result;
    this.handledAt = now;
  }
}
