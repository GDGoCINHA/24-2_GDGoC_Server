package inha.gdgoc.domain.board.free.entity;

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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자유게시판 댓글. 대댓글은 1단계까지만이다.
 *
 * <p>깊이를 한 단계로 묶은 이유는 두 가지다. 무한 중첩은 화면에서 들여쓰기가 감당이 안 되고, 조회할 때 재귀가 필요해
 * 쿼리가 복잡해진다. parent 가 있으면 대댓글, 없으면 댓글이고 그 이상은 없다 — 대댓글에 다는 답글도 같은 부모에
 * 붙인다({@code resolveParent} 참고).
 *
 * <p>삭제는 soft delete 다. 자식이 달린 댓글을 지워도 행은 남는다. 트리에서 부모가 사라지면 자식이 갈 곳이 없어
 * 화면에서 통째로 사라지기 때문이다. 대신 내용을 감추고 '삭제된 댓글입니다'로 보이게 한다.
 */
@Entity
@Table(name = "free_board_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreeBoardComment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "free_board_id", nullable = false)
  private FreeBoard freeBoard;

  /** null 이면 최상위 댓글이다. 값이 있으면 그 댓글에 달린 대댓글이고, 그 아래는 없다. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private FreeBoardComment parent;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "author_id", nullable = false)
  private Long authorId;

  /** 작성 시점 스냅샷. 회원이 탈퇴해도 작성자가 사라지지 않는다. 글과 같은 규칙이다. */
  @Column(name = "author_name", nullable = false, length = 100)
  private String authorName;

  @Column private Instant deletedAt;

  public static FreeBoardComment create(
      FreeBoard freeBoard,
      FreeBoardComment parent,
      String content,
      Long authorId,
      String authorName) {
    FreeBoardComment comment = new FreeBoardComment();
    comment.freeBoard = freeBoard;
    comment.parent = resolveParent(parent);
    comment.content = content;
    comment.authorId = authorId;
    comment.authorName = authorName;
    return comment;
  }

  /**
   * 깊이를 1단계로 고정한다.
   *
   * <p>대댓글에 답글을 달면 그 대댓글의 부모(=최상위 댓글)에 붙인다. 요청을 거부하지 않는 이유는, 화면에서 대댓글에도
   * '답글' 버튼이 보이는 편이 자연스럽고 사용자는 깊이를 신경 쓸 이유가 없기 때문이다.
   */
  private static FreeBoardComment resolveParent(FreeBoardComment parent) {
    if (parent == null) return null;
    return parent.parent == null ? parent : parent.parent;
  }

  public void update(String content) {
    this.content = content;
  }

  public void softDelete() {
    this.deletedAt = Instant.now();
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public boolean isReply() {
    return parent != null;
  }
}
