package inha.gdgoc.domain.board.common.entity;

import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;

/**
 * 세 게시판(공지·자유·행사)이 공유하는 글의 뼈대.
 *
 * <p>테이블은 게시판별로 분리하고 필드 정의만 여기서 공유한다. 권한·조회 로직은 각 도메인이 갖는다.
 */
@Getter
@MappedSuperclass
public abstract class BoardEntity extends BaseEntity {

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "author_id", nullable = false)
  private Long authorId;

  /** 작성 시점 스냅샷. 회원이 탈퇴해도 작성자가 사라지지 않는다. */
  @Column(name = "author_name", nullable = false, length = 100)
  private String authorName;

  @Column private Instant deletedAt;

  protected void initBoard(String title, String content, Long authorId, String authorName) {
    this.title = title;
    this.content = content;
    this.authorId = authorId;
    this.authorName = authorName;
  }

  protected void updateTitle(String title) {
    if (title != null) this.title = title;
  }

  protected void updateContent(String content) {
    if (content != null) this.content = content;
  }

  public void softDelete() {
    this.deletedAt = Instant.now();
  }

  public void restore() {
    this.deletedAt = null;
  }
}
