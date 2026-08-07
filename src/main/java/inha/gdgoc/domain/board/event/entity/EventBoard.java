package inha.gdgoc.domain.board.event.entity;

import inha.gdgoc.domain.board.common.entity.BoardEntity;
import inha.gdgoc.domain.user.enums.TeamType;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventBoard extends BoardEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private LocalDate eventStartDate;

  @Column(nullable = false)
  private LocalDate eventEndDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TeamType organizingTeam;

  @Column(length = 512)
  private String thumbnailKey;

  @Column(nullable = false)
  private boolean isPublished;

  @OneToMany(mappedBy = "eventBoard", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<EventBoardAttachment> attachments = new ArrayList<>();

  public static EventBoard create(
      String title,
      LocalDate eventStartDate,
      LocalDate eventEndDate,
      TeamType organizingTeam,
      String thumbnailKey,
      String content,
      boolean isPublished,
      Long authorId,
      String authorName) {
    EventBoard board = new EventBoard();
    board.initBoard(title, content, authorId, authorName);
    board.eventStartDate = eventStartDate;
    board.eventEndDate = eventEndDate;
    board.organizingTeam = organizingTeam;
    board.thumbnailKey = thumbnailKey;
    board.isPublished = isPublished;
    return board;
  }

  @Override
  public void addFileAttachment(String fileKey, String fileName, Long fileSize, int sortOrder) {
    this.attachments.add(
        EventBoardAttachment.createFile(this, fileKey, fileName, fileSize, sortOrder));
  }

  @Override
  public void addLinkAttachment(String url, int sortOrder) {
    this.attachments.add(EventBoardAttachment.createLink(this, url, sortOrder));
  }

  public void update(
      String title,
      LocalDate eventStartDate,
      LocalDate eventEndDate,
      TeamType organizingTeam,
      String thumbnailKey,
      String content,
      Boolean isPublished) {
    updateTitle(title);
    updateContent(content);
    if (eventStartDate != null) this.eventStartDate = eventStartDate;
    if (eventEndDate != null) this.eventEndDate = eventEndDate;
    if (organizingTeam != null) this.organizingTeam = organizingTeam;
    if (thumbnailKey != null) this.thumbnailKey = thumbnailKey;
    if (isPublished != null) this.isPublished = isPublished;
  }
}
