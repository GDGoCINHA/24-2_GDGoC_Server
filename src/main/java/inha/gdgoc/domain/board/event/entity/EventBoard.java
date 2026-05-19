package inha.gdgoc.domain.board.event.entity;

import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.global.entity.BaseEntity;
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
import java.time.Instant;
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
public class EventBoard extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false)
  private LocalDate eventStartDate;

  @Column(nullable = false)
  private LocalDate eventEndDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TeamType organizingTeam;

  @Column(length = 512)
  private String thumbnailKey;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(nullable = false)
  private boolean isPublished;

  @Column(name = "author_id", nullable = false)
  private Long authorId;

  @Column
  private Instant deletedAt;

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
      Long authorId) {
    EventBoard board = new EventBoard();
    board.title = title;
    board.eventStartDate = eventStartDate;
    board.eventEndDate = eventEndDate;
    board.organizingTeam = organizingTeam;
    board.thumbnailKey = thumbnailKey;
    board.content = content;
    board.isPublished = isPublished;
    board.authorId = authorId;
    return board;
  }

  public void softDelete() {
    this.deletedAt = Instant.now();
  }

  public void restore() {
    this.deletedAt = null;
  }

  public void addAttachment(String fileKey, String fileName) {
    this.attachments.add(EventBoardAttachment.create(this, fileKey, fileName));
  }

  public void update(
      String title,
      LocalDate eventStartDate,
      LocalDate eventEndDate,
      TeamType organizingTeam,
      String thumbnailKey,
      String content,
      Boolean isPublished) {
    if (title != null) this.title = title;
    if (eventStartDate != null) this.eventStartDate = eventStartDate;
    if (eventEndDate != null) this.eventEndDate = eventEndDate;
    if (organizingTeam != null) this.organizingTeam = organizingTeam;
    if (thumbnailKey != null) this.thumbnailKey = thumbnailKey;
    if (content != null) this.content = content;
    if (isPublished != null) this.isPublished = isPublished;
  }
}
