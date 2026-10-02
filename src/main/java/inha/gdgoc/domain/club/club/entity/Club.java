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
 * <p>이끔이는 {@code users.role} 이 아니라 {@link #leader} 다. 일반 부원도 이끔이가 될 수 있다.
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
}
