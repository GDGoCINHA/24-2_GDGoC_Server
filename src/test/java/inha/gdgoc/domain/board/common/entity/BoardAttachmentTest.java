package inha.gdgoc.domain.board.common.entity;

import static org.assertj.core.api.Assertions.assertThat;

import inha.gdgoc.domain.board.common.enums.AttachmentKind;
import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.board.event.entity.EventBoardAttachment;
import inha.gdgoc.domain.user.enums.TeamType;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 첨부는 파일과 링크 두 종류이며, 종류에 따라 채워지는 필드가 배타적이다. */
class BoardAttachmentTest {

  @Test
  @DisplayName("파일 첨부는 fileKey·fileName·fileSize 를 갖고 url 은 비어 있다")
  void fileAttachmentHasFileFieldsOnly() {
    EventBoardAttachment attachment =
        EventBoardAttachment.createFile(board(), "user/1/event/uuid-a.pdf", "a.pdf", 1024L, 0);

    assertThat(attachment.getKind()).isEqualTo(AttachmentKind.FILE);
    assertThat(attachment.getFileKey()).isEqualTo("user/1/event/uuid-a.pdf");
    assertThat(attachment.getFileName()).isEqualTo("a.pdf");
    assertThat(attachment.getFileSize()).isEqualTo(1024L);
    assertThat(attachment.getUrl()).isNull();
    assertThat(attachment.getSortOrder()).isZero();
  }

  @Test
  @DisplayName("링크 첨부는 url 만 갖고 파일 필드는 비어 있다")
  void linkAttachmentHasUrlOnly() {
    EventBoardAttachment attachment =
        EventBoardAttachment.createLink(board(), "https://pf.kakao.com/_abc/chat", 1);

    assertThat(attachment.getKind()).isEqualTo(AttachmentKind.LINK);
    assertThat(attachment.getUrl()).isEqualTo("https://pf.kakao.com/_abc/chat");
    assertThat(attachment.getFileKey()).isNull();
    assertThat(attachment.getFileName()).isNull();
    assertThat(attachment.getFileSize()).isNull();
    assertThat(attachment.getSortOrder()).isEqualTo(1);
  }

  @Test
  @DisplayName("게시글에 첨부를 더하면 순서대로 쌓인다")
  void boardAccumulatesAttachmentsInOrder() {
    EventBoard board = board();

    board.addFileAttachment("user/1/event/uuid-a.pdf", "a.pdf", 10L, 0);
    board.addLinkAttachment("https://example.com", 1);

    assertThat(board.getAttachments())
        .extracting(a -> a.getKind())
        .containsExactly(AttachmentKind.FILE, AttachmentKind.LINK);
  }

  private EventBoard board() {
    return EventBoard.create(
        "제목",
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 2),
        TeamType.PR_DESIGN,
        null,
        "본문",
        true,
        1L,
        "홍길동");
  }
}
