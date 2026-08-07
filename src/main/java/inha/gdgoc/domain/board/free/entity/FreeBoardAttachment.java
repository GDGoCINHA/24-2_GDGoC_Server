package inha.gdgoc.domain.board.free.entity;

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
@Table(name = "free_board_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreeBoardAttachment extends BoardAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "free_board_id", nullable = false)
  private FreeBoard freeBoard;

  public static FreeBoardAttachment createFile(
      FreeBoard freeBoard, String fileKey, String fileName, Long fileSize, int sortOrder) {
    FreeBoardAttachment attachment = new FreeBoardAttachment();
    attachment.freeBoard = freeBoard;
    attachment.initFile(fileKey, fileName, fileSize, sortOrder);
    return attachment;
  }

  public static FreeBoardAttachment createLink(FreeBoard freeBoard, String url, int sortOrder) {
    FreeBoardAttachment attachment = new FreeBoardAttachment();
    attachment.freeBoard = freeBoard;
    attachment.initLink(url, sortOrder);
    return attachment;
  }
}
