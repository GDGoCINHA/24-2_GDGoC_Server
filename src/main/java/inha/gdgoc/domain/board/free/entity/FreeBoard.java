package inha.gdgoc.domain.board.free.entity;

import inha.gdgoc.domain.board.common.entity.BoardEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자유게시판 글.
 *
 * <p>공지와 달리 분류(category)와 임시저장(isPublished)이 없다. 분류는 운영진이 공지를 갈래별로 묶으려고 둔 것이고,
 * 임시저장은 공개 시점을 고르는 기능이라 회원이 쓰는 자유게시판에는 쓰이지 않는다.
 */
@Entity
@Table(name = "free_board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreeBoard extends BoardEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private int viewCount;

  @OneToMany(mappedBy = "freeBoard", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<FreeBoardAttachment> attachments = new ArrayList<>();

  public static FreeBoard create(String title, String content, Long authorId, String authorName) {
    FreeBoard post = new FreeBoard();
    post.initBoard(title, content, authorId, authorName);
    post.viewCount = 0;
    return post;
  }

  @Override
  public void addFileAttachment(String fileKey, String fileName, Long fileSize, int sortOrder) {
    this.attachments.add(
        FreeBoardAttachment.createFile(this, fileKey, fileName, fileSize, sortOrder));
  }

  @Override
  public void addLinkAttachment(String url, int sortOrder) {
    this.attachments.add(FreeBoardAttachment.createLink(this, url, sortOrder));
  }

  public void update(String title, String content) {
    updateTitle(title);
    updateContent(content);
  }
}
