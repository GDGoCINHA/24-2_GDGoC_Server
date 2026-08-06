package inha.gdgoc.domain.board.notice.entity;

import inha.gdgoc.domain.board.common.entity.BoardAttachment;
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

@Entity
@Table(name = "notice_board_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeBoardAttachment extends BoardAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notice_board_id", nullable = false)
  private NoticeBoard noticeBoard;

  public static NoticeBoardAttachment createFile(
      NoticeBoard noticeBoard, String fileKey, String fileName, Long fileSize, int sortOrder) {
    NoticeBoardAttachment attachment = new NoticeBoardAttachment();
    attachment.noticeBoard = noticeBoard;
    attachment.initFile(fileKey, fileName, fileSize, sortOrder);
    return attachment;
  }

  public static NoticeBoardAttachment createLink(
      NoticeBoard noticeBoard, String url, int sortOrder) {
    NoticeBoardAttachment attachment = new NoticeBoardAttachment();
    attachment.noticeBoard = noticeBoard;
    attachment.initLink(url, sortOrder);
    return attachment;
  }
}
