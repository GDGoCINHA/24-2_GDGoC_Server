package inha.gdgoc.domain.eventapplication.entity;

import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.enums.EventAttendanceStatus;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 사람의 행사 신청.
 *
 * <p>취소해도 행을 지우지 않고 {@link ApplicationStatus} 만 바꾼다. {@code UNIQUE(form_id, user_id)} 로 중복 신청을 막는데,
 * 취소를 삭제로 처리하면 재신청 때 이 제약과 충돌하기 때문이다. 재신청은 {@link #reapply} 로 같은 행을 되살린다.
 *
 * <p>참석 여부를 여기에 컬럼으로 둔 것은 신청자가 곧 참석 대상이어서다. 별도 참석 표를 만들 이유가 없다.
 */
@Entity
@Table(
    name = "event_application",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_event_application_form_user",
            columnNames = {"form_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventApplication extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "form_id", nullable = false)
  private EventApplicationForm form;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private ApplicationStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "attendance_status", nullable = false, length = 16)
  private EventAttendanceStatus attendanceStatus;

  /** QR 로 체크인한 시각. 비어 있으면 운영진이 수기로 처리한 것이다. */
  @Column(name = "checked_in_at")
  private Instant checkedInAt;

  @Column(name = "applied_at", nullable = false)
  private Instant appliedAt;

  @Column(name = "canceled_at")
  private Instant canceledAt;

  @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<EventApplicationAnswer> answers = new ArrayList<>();

  public static EventApplication create(EventApplicationForm form, User user, Instant now) {
    EventApplication application = new EventApplication();
    application.form = form;
    application.user = user;
    application.status = ApplicationStatus.APPLIED;
    application.attendanceStatus = EventAttendanceStatus.PENDING;
    application.appliedAt = now;
    application.answers = new ArrayList<>();
    return application;
  }

  public void cancel(Instant now) {
    this.status = ApplicationStatus.CANCELED;
    this.canceledAt = now;
  }

  /** 취소했던 신청을 되살린다. 답변은 새로 받은 것으로 갈아끼우므로 호출 전에 비운다. */
  public void reapply(Instant now) {
    this.status = ApplicationStatus.APPLIED;
    this.appliedAt = now;
    this.canceledAt = null;
  }

  public void checkIn(Instant now) {
    this.attendanceStatus = EventAttendanceStatus.ATTENDED;
    this.checkedInAt = now;
  }

  /** 운영진이 수기로 참석을 바꾼다. QR 체크인 시각은 건드리지 않는다. */
  public void markAttendance(EventAttendanceStatus status) {
    this.attendanceStatus = status;
  }

  public void clearAnswers() {
    this.answers.clear();
  }

  public void addAnswer(EventApplicationAnswer answer) {
    this.answers.add(answer);
  }

  public boolean isApplied() {
    return this.status == ApplicationStatus.APPLIED;
  }
}
