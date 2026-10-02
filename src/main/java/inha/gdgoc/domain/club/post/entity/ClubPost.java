package inha.gdgoc.domain.club.post.entity;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.post.enums.ClubPostCategory;
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
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 팀 일반 게시글. 완주에 반영하지 않는다.
 */
@Entity
@Table(name = "club_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubPost extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false)
  private Club club;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 16)
  private ClubPostCategory category;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "image_urls", columnDefinition = "jsonb")
  private List<String> imageUrls;

  @Column(name = "deleted_at")
  private Instant deletedAt;
}
