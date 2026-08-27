package inha.gdgoc.domain.eventapplication.entity;

import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 행사 신청 폼.
 *
 * <p>이 행이 있어야 행사 상세 화면에 신청 버튼이 뜬다. 신청을 받지 않는 행사는 이 행이 없고, 그런 행사의 게시글은 지금까지와 완전히 동일하게 동작한다.
 *
 * <p>{@code eventBoardId} 는 FK 가 아니다. 행사명·기간을 이 표에 복사해 두고, 게시글이 휴지통에 들어가도 신청 데이터와 마이페이지 이력이 살아 있게 한다.
 * 복사본은 게시글을 수정할 때 {@link #syncEventInfo} 로 함께 갱신한다.
 */
@Entity
@Table(name = "event_application_form")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventApplicationForm extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_board_id", nullable = false, unique = true)
  private Long eventBoardId;

  @Column(name = "event_title", nullable = false, length = 255)
  private String eventTitle;

  @Column(name = "event_start_date", nullable = false)
  private LocalDate eventStartDate;

  @Column(name = "event_end_date", nullable = false)
  private LocalDate eventEndDate;

  @Column(name = "opens_at")
  private Instant opensAt;

  @Column(name = "closes_at")
  private Instant closesAt;

  @Column(name = "capacity")
  private Integer capacity;

  @Enumerated(EnumType.STRING)
  @Column(name = "min_role", nullable = false, length = 16)
  private UserRole minRole;

  @Column(name = "is_open", nullable = false)
  private boolean isOpen;

  @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder ASC, id ASC")
  private List<EventFormQuestion> questions = new ArrayList<>();

  public static EventApplicationForm create(
      Long eventBoardId,
      String eventTitle,
      LocalDate eventStartDate,
      LocalDate eventEndDate,
      Instant opensAt,
      Instant closesAt,
      Integer capacity,
      UserRole minRole,
      boolean isOpen) {
    EventApplicationForm form = new EventApplicationForm();
    form.eventBoardId = eventBoardId;
    form.eventTitle = eventTitle;
    form.eventStartDate = eventStartDate;
    form.eventEndDate = eventEndDate;
    form.opensAt = opensAt;
    form.closesAt = closesAt;
    form.capacity = capacity;
    form.minRole = minRole != null ? minRole : UserRole.MEMBER;
    form.isOpen = isOpen;
    form.questions = new ArrayList<>();
    return form;
  }

  /** 신청 설정을 바꾼다. null 인 항목은 건드리지 않는다. */
  public void updateSettings(
      Instant opensAt, Instant closesAt, Integer capacity, UserRole minRole, Boolean isOpen) {
    if (opensAt != null) this.opensAt = opensAt;
    if (closesAt != null) this.closesAt = closesAt;
    if (capacity != null) this.capacity = capacity;
    if (minRole != null) this.minRole = minRole;
    if (isOpen != null) this.isOpen = isOpen;
  }

  /** 정원을 무제한으로 되돌린다. {@link #updateSettings} 로는 null 을 넣을 수 없기 때문이다. */
  public void clearCapacity() {
    this.capacity = null;
  }

  /** 게시글이 수정될 때 복사본을 최신으로 맞춘다. */
  public void syncEventInfo(String eventTitle, LocalDate eventStartDate, LocalDate eventEndDate) {
    if (eventTitle != null) this.eventTitle = eventTitle;
    if (eventStartDate != null) this.eventStartDate = eventStartDate;
    if (eventEndDate != null) this.eventEndDate = eventEndDate;
  }

  public void addQuestion(EventFormQuestion question) {
    this.questions.add(question);
  }

  /** 삭제되지 않은 질문만 순서대로 돌려준다. */
  public List<EventFormQuestion> activeQuestions() {
    return this.questions.stream().filter(q -> !q.isDeleted()).toList();
  }
}
