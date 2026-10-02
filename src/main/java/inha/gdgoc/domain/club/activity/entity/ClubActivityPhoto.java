package inha.gdgoc.domain.club.activity.entity;

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
 * 활동 사진.
 */
@Entity
@Table(name = "club_activity_photo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubActivityPhoto extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "activity_id", nullable = false)
  private ClubActivity activity;

  @Column(name = "url", nullable = false, length = 500)
  private String url;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;
}
