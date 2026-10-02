package inha.gdgoc.domain.club.club.entity;

import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기수. 날짜는 없다 — 활동 기간은 소모임마다 둔다.
 */
@Entity
@Table(name = "club_term")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubTerm extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "attendance_ratio", nullable = false, precision = 3, scale = 2)
  private BigDecimal attendanceRatio;
}
