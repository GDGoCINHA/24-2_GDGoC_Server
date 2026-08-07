package inha.gdgoc.domain.board.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.board.common.dto.AttachmentEntry;
import inha.gdgoc.domain.board.common.service.AttachmentPolicy;
import inha.gdgoc.domain.board.notice.dto.request.NoticeCreateRequest;
import inha.gdgoc.domain.board.notice.dto.request.NoticeUpdateRequest;
import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.board.notice.enums.NoticeCategory;
import inha.gdgoc.domain.board.notice.repository.NoticeBoardRepository;
import inha.gdgoc.domain.board.notice.repository.PinnedNoticeRepository;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 공지 CRUD 의 권한 경계와 조회수 동작. */
@ExtendWith(MockitoExtension.class)
class NoticeBoardServiceTest {

  @Mock private NoticeBoardRepository noticeBoardRepository;
  @Mock private PinnedNoticeRepository pinnedNoticeRepository;
  @Mock private UserRepository userRepository;
  @Mock private S3Service s3Service;

  private NoticeBoardService noticeBoardService;

  @BeforeEach
  void setUp() {
    noticeBoardService =
        new NoticeBoardService(
            noticeBoardRepository,
            pinnedNoticeRepository,
            userRepository,
            new AttachmentPolicy(s3Service));
  }

  @Test
  @DisplayName("상세를 조회하면 조회수 벌크 UPDATE 가 호출된다")
  void getNoticeIncreasesViewCount() {
    NoticeBoard notice = published();
    when(noticeBoardRepository.findById(1L)).thenReturn(Optional.of(notice));

    noticeBoardService.getNotice(1L, UserRole.GUEST);

    verify(noticeBoardRepository).increaseViewCount(1L);
  }

  @Test
  @DisplayName("미공개 공지는 CORE 미만에게 404 다")
  void unpublishedNoticeIsNotFoundForLowRole() {
    NoticeBoard notice =
        NoticeBoard.create("제목", "본문", NoticeCategory.OPERATION, false, 1L, "홍길동");
    when(noticeBoardRepository.findById(1L)).thenReturn(Optional.of(notice));

    assertThatThrownBy(() -> noticeBoardService.getNotice(1L, UserRole.MEMBER))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(GlobalErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("작성자 본인은 수정할 수 있다")
  void authorCanUpdate() {
    NoticeBoard notice = published();
    when(noticeBoardRepository.findById(1L)).thenReturn(Optional.of(notice));

    noticeBoardService.updateNotice(1L, updateTitleTo("바뀐 제목"), 1L, UserRole.CORE);

    assertThat(notice.getTitle()).isEqualTo("바뀐 제목");
  }

  @Test
  @DisplayName("작성자가 아닌 CORE 는 수정할 수 없다")
  void otherCoreCannotUpdate() {
    NoticeBoard notice = published();
    when(noticeBoardRepository.findById(1L)).thenReturn(Optional.of(notice));

    assertThatThrownBy(
            () -> noticeBoardService.updateNotice(1L, updateTitleTo("침입"), 99L, UserRole.CORE))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(GlobalErrorCode.FORBIDDEN_USER);
  }

  @Test
  @DisplayName("ORGANIZER 는 남의 글도 수정할 수 있다")
  void organizerCanUpdateOthers() {
    NoticeBoard notice = published();
    when(noticeBoardRepository.findById(1L)).thenReturn(Optional.of(notice));

    noticeBoardService.updateNotice(1L, updateTitleTo("운영진 수정"), 99L, UserRole.ORGANIZER);

    assertThat(notice.getTitle()).isEqualTo("운영진 수정");
  }

  @Test
  @DisplayName("삭제하면 deletedAt 이 채워지고 고정 행도 지워진다")
  void deleteAlsoUnpins() {
    NoticeBoard notice = published();
    when(noticeBoardRepository.findById(1L)).thenReturn(Optional.of(notice));

    noticeBoardService.deleteNotice(1L, 1L, UserRole.CORE);

    assertThat(notice.getDeletedAt()).isNotNull();
    verify(pinnedNoticeRepository).deleteByNoticeBoardId(1L);
  }

  @Test
  @DisplayName("작성자가 아닌 CORE 는 삭제할 수 없다")
  void otherCoreCannotDelete() {
    NoticeBoard notice = published();
    when(noticeBoardRepository.findById(1L)).thenReturn(Optional.of(notice));

    assertThatThrownBy(() -> noticeBoardService.deleteNotice(1L, 99L, UserRole.CORE))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(GlobalErrorCode.FORBIDDEN_USER);
  }

  @Test
  @DisplayName("복구해도 고정은 돌아오지 않는다")
  void restoreDoesNotRepin() {
    NoticeBoard notice = published();
    notice.softDelete();
    when(noticeBoardRepository.findDeletedById(1L)).thenReturn(Optional.of(notice));

    noticeBoardService.restoreNotice(1L, 1L, UserRole.CORE);

    assertThat(notice.getDeletedAt()).isNull();
    verifyNoInteractions(pinnedNoticeRepository);
  }

  @Test
  @DisplayName("작성하면 작성자 이름과 첨부가 채워진 채로 저장된다")
  void createSavesNoticeWithAuthorAndAttachments() {
    User author = mock(User.class);
    when(author.getName()).thenReturn("홍길동");
    when(userRepository.findById(1L)).thenReturn(Optional.of(author));
    when(noticeBoardRepository.save(any(NoticeBoard.class))).thenAnswer(inv -> inv.getArgument(0));
    when(s3Service.getObjectSize("user/1/notice/a.pdf")).thenReturn(2048L);

    noticeBoardService.createNotice(
        new NoticeCreateRequest(
            "제목",
            "본문",
            NoticeCategory.OPERATION,
            true,
            List.of(new AttachmentEntry("user/1/notice/a.pdf", "a.pdf", null))),
        1L);

    ArgumentCaptor<NoticeBoard> captor = ArgumentCaptor.forClass(NoticeBoard.class);
    verify(noticeBoardRepository).save(captor.capture());

    assertThat(captor.getValue().getTitle()).isEqualTo("제목");
    assertThat(captor.getValue().getAuthorName()).isEqualTo("홍길동");
    assertThat(captor.getValue().getAttachments())
        .singleElement()
        .satisfies(a -> assertThat(a.getFileSize()).isEqualTo(2048L));
  }

  private NoticeBoard published() {
    return NoticeBoard.create("제목", "본문", NoticeCategory.OPERATION, true, 1L, "홍길동");
  }

  private NoticeUpdateRequest updateTitleTo(String title) {
    return new NoticeUpdateRequest(title, null, null, null, null);
  }
}
