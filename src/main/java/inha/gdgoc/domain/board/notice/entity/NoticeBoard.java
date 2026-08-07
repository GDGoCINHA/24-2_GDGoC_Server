package inha.gdgoc.domain.board.notice.entity;

import inha.gdgoc.domain.board.common.entity.BoardEntity;
import inha.gdgoc.domain.board.notice.enums.NoticeCategory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "notice_board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeBoard extends BoardEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private NoticeCategory category;

  @Column(nullable = false)
  private int viewCount;

  @Column(nullable = false)
  private boolean isPublished;

  @OneToMany(mappedBy = "noticeBoard", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<NoticeBoardAttachment> attachments = new ArrayList<>();

  public static NoticeBoard create(
      String title,
      String content,
      NoticeCategory category,
      boolean isPublished,
      Long authorId,
      String authorName) {
    NoticeBoard notice = new NoticeBoard();
    notice.initBoard(title, content, authorId, authorName);
    notice.category = category;
    notice.isPublished = isPublished;
    notice.viewCount = 0;
    return notice;
  }

  @Override
  public void addFileAttachment(String fileKey, String fileName, Long fileSize, int sortOrder) {
    this.attachments.add(
        NoticeBoardAttachment.createFile(this, fileKey, fileName, fileSize, sortOrder));
  }

  @Override
  public void addLinkAttachment(String url, int sortOrder) {
    this.attachments.add(NoticeBoardAttachment.createLink(this, url, sortOrder));
  }

  public void update(String title, String content, NoticeCategory category, Boolean isPublished) {
    updateTitle(title);
    updateContent(content);
    if (category != null) this.category = category;
    if (isPublished != null) this.isPublished = isPublished;
  }
}
