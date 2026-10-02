package inha.gdgoc.domain.club.reaction.entity;

import inha.gdgoc.domain.club.reaction.enums.ClubTargetType;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글. 대상은 일반 게시글 또는 활동 기록.
 */
@Entity
@Table(name = "club_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubComment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 16)
  private ClubTargetType targetType;

  @Column(name = "target_id", nullable = false)
  private long targetId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  public static ClubComment create(
      ClubTargetType targetType, long targetId, User author, String content) {
    ClubComment comment = new ClubComment();
    comment.targetType = targetType;
    comment.targetId = targetId;
    comment.author = author;
    comment.content = content;
    return comment;
  }

  /** 소프트 삭제. 이미 지운 댓글을 다시 지워도 처음 지운 시각을 유지한다. */
  public void softDelete(Instant now) {
    if (deletedAt == null) {
      deletedAt = now;
    }
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }
}
