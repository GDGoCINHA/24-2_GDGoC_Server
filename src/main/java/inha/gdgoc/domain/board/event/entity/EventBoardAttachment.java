package inha.gdgoc.domain.board.event.entity;

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
@Table(name = "event_board_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventBoardAttachment extends BoardAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_board_id", nullable = false)
  private EventBoard eventBoard;

  public static EventBoardAttachment createFile(
      EventBoard eventBoard, String fileKey, String fileName, Long fileSize, int sortOrder) {
    EventBoardAttachment attachment = new EventBoardAttachment();
    attachment.eventBoard = eventBoard;
    attachment.initFile(fileKey, fileName, fileSize, sortOrder);
    return attachment;
  }

  public static EventBoardAttachment createLink(EventBoard eventBoard, String url, int sortOrder) {
    EventBoardAttachment attachment = new EventBoardAttachment();
    attachment.eventBoard = eventBoard;
    attachment.initLink(url, sortOrder);
    return attachment;
  }
}
