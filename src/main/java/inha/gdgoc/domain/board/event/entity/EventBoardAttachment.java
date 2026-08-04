package inha.gdgoc.domain.board.event.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_board_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventBoardAttachment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_board_id", nullable = false)
  private EventBoard eventBoard;

  @Column(nullable = false, length = 512)
  private String fileKey;

  @Column(nullable = false, length = 255)
  private String fileName;

  static EventBoardAttachment create(EventBoard eventBoard, String fileKey, String fileName) {
    EventBoardAttachment attachment = new EventBoardAttachment();
    attachment.eventBoard = eventBoard;
    attachment.fileKey = fileKey;
    attachment.fileName = fileName;
    return attachment;
  }
}
