package inha.gdgoc.domain.board.notice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import inha.gdgoc.domain.board.common.enums.AttachmentKind;
import inha.gdgoc.domain.board.notice.enums.NoticeCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 공지 엔티티의 생성·수정·조회수·첨부 동작. */
class NoticeBoardTest {

  @Test
  @DisplayName("생성하면 조회수는 0이고 첨부는 비어 있다")
  void createStartsWithZeroViewsAndNoAttachments() {
    NoticeBoard notice = notice();

    assertThat(notice.getTitle()).isEqualTo("공지 제목");
    assertThat(notice.getContent()).isEqualTo("공지 본문");
    assertThat(notice.getCategory()).isEqualTo(NoticeCategory.OPERATION);
    assertThat(notice.isPublished()).isTrue();
    assertThat(notice.getViewCount()).isZero();
    assertThat(notice.getAttachments()).isEmpty();
    assertThat(notice.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("수정은 null 이 아닌 필드만 바꾼다")
  void updateChangesOnlyNonNullFields() {
    NoticeBoard notice = notice();

    notice.update("새 제목", null, NoticeCategory.RECRUIT, null);

    assertThat(notice.getTitle()).isEqualTo("새 제목");
    assertThat(notice.getContent()).isEqualTo("공지 본문");
    assertThat(notice.getCategory()).isEqualTo(NoticeCategory.RECRUIT);
    assertThat(notice.isPublished()).isTrue();
  }

  @Test
  @DisplayName("첨부를 더하면 종류와 순서가 그대로 담긴다")
  void addAttachmentsKeepsKindAndOrder() {
    NoticeBoard notice = notice();

    notice.addFileAttachment("user/1/notice/a.pdf", "a.pdf", 1024L, 0);
    notice.addLinkAttachment("https://example.com", 1);

    assertThat(notice.getAttachments())
        .extracting(a -> a.getKind())
        .containsExactly(AttachmentKind.FILE, AttachmentKind.LINK);
    assertThat(notice.getAttachments().get(0).getNoticeBoard()).isSameAs(notice);
    assertThat(notice.getAttachments().get(1).getUrl()).isEqualTo("https://example.com");
    assertThat(notice.getAttachments().get(1).getFileKey()).isNull();
  }

  private NoticeBoard notice() {
    return NoticeBoard.create("공지 제목", "공지 본문", NoticeCategory.OPERATION, true, 1L, "홍길동");
  }
}
