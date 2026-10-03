package inha.gdgoc.domain.club.club.entity;

import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubRecruitStatus;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소모임.
 *
 * <p>리더는 {@code users.role} 이 아니라 {@link #leader} 다. 일반 부원도 리더가 될 수 있다.
 */
@Entity
@Table(name = "club")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "term_id", nullable = false)
  private ClubTerm term;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "leader_id", nullable = false)
  private User leader;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 16)
  private ClubCategory category;

  @Column(name = "summary", nullable = false, length = 200)
  private String summary;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "activity_method", columnDefinition = "TEXT")
  private String activityMethod;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(name = "kakao_link", length = 500)
  private String kakaoLink;

  @Column(name = "capacity")
  private Integer capacity;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "recruit_status", nullable = false, length = 16)
  private ClubRecruitStatus recruitStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private ClubStatus status;

  /** 반려 사유. 승인하면 지운다. */
  @Column(name = "reject_reason", length = 500)
  private String rejectReason;

  /** 개설하면 승인 대기다. 운영진이 승인해야 게시판에 올라간다. */
  public static Club create(
      ClubTerm term,
      User leader,
      String name,
      ClubCategory category,
      String summary,
      String description,
      String activityMethod,
      String imageUrl,
      String kakaoLink,
      Integer capacity,
      LocalDate startDate,
      LocalDate endDate) {
    Club club = new Club();
    club.term = term;
    club.leader = leader;
    club.name = name;
    club.category = category;
    club.summary = summary;
    club.description = description;
    club.activityMethod = activityMethod;
    club.imageUrl = imageUrl;
    club.kakaoLink = kakaoLink;
    club.capacity = capacity;
    club.startDate = startDate;
    club.endDate = endDate;
    club.recruitStatus = ClubRecruitStatus.RECRUITING;
    club.status = ClubStatus.PENDING;
    return club;
  }

  /**
   * 수정 화면 저장. 보낸 값으로 통째로 바꾼다 — null 이면 비운다. 모집 상태·기수·리더는 건드리지 않는다.
   * 반려된 소모임을 고쳐 저장하면 다시 승인 대기로 올린다.
   */
  public void replace(
      String name,
      ClubCategory category,
      String summary,
      String description,
      String activityMethod,
      String imageUrl,
      String kakaoLink,
      Integer capacity,
      LocalDate startDate,
      LocalDate endDate) {
    this.name = name;
    this.category = category;
    this.summary = summary;
    this.description = description;
    this.activityMethod = activityMethod;
    this.imageUrl = imageUrl;
    this.kakaoLink = kakaoLink;
    this.capacity = capacity;
    this.startDate = startDate;
    this.endDate = endDate;
    if (this.status == ClubStatus.REJECTED) {
      this.status = ClubStatus.PENDING;
    }
  }

  /** 부분 수정(모집 마감 버튼 등). null 인 항목은 건드리지 않는다. 검증하지 않는다 — 정원·기간은 경고로만 보여준다. */
  public void update(
      String name,
      ClubCategory category,
      String summary,
      String description,
      String activityMethod,
      String imageUrl,
      String kakaoLink,
      Integer capacity,
      LocalDate startDate,
      LocalDate endDate,
      ClubRecruitStatus recruitStatus) {
    if (name != null) this.name = name;
    if (category != null) this.category = category;
    if (summary != null) this.summary = summary;
    if (description != null) this.description = description;
    if (activityMethod != null) this.activityMethod = activityMethod;
    if (imageUrl != null) this.imageUrl = imageUrl;
    if (kakaoLink != null) this.kakaoLink = kakaoLink;
    if (capacity != null) this.capacity = capacity;
    if (startDate != null) this.startDate = startDate;
    if (endDate != null) this.endDate = endDate;
    if (recruitStatus != null) this.recruitStatus = recruitStatus;
  }

  /** 운영진만 바꾸는 항목. */
  public void updateByStaff(ClubStatus status, ClubTerm term) {
    if (status != null) this.status = status;
    if (term != null) this.term = term;
  }

  public void approve() {
    this.status = ClubStatus.ACTIVE;
    this.rejectReason = null;
  }

  public void reject(String reason) {
    this.status = ClubStatus.REJECTED;
    this.rejectReason = reason;
  }

  public void changeLeader(User newLeader) {
    this.leader = newLeader;
  }

  public boolean isRecruiting() {
    return status == ClubStatus.ACTIVE && recruitStatus == ClubRecruitStatus.RECRUITING;
  }
}
