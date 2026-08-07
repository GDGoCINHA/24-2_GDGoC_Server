package inha.gdgoc.domain.board.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.board.notice.dto.request.PinnedUpdateRequest;
import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.board.notice.entity.PinnedNotice;
import inha.gdgoc.domain.board.notice.enums.NoticeCategory;
import inha.gdgoc.domain.board.notice.exception.NoticeErrorCode;
import inha.gdgoc.domain.board.notice.repository.NoticeBoardRepository;
import inha.gdgoc.domain.board.notice.repository.PinnedNoticeRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** 고정은 전체 교체 한 방식뿐이며, 배열 순서가 곧 display_order 다. */
@ExtendWith(MockitoExtension.class)
class PinnedNoticeServiceTest {

  @Mock private PinnedNoticeRepository pinnedNoticeRepository;
  @Mock private NoticeBoardRepository noticeBoardRepository;
  @Mock private NoticeBoardService noticeBoardService;

  @InjectMocks private PinnedNoticeService pinnedNoticeService;

  @Test
  @DisplayName("배열 순서가 display_order 1·2·3 이 된다")
  void arrayOrderBecomesDisplayOrder() {
    // IN 절의 결과 순서는 DB 가 보장하지 않는다. 요청과 반대로 돌아온 상황을 일부러 모사한다 —
    // 목이 요청과 같은 순서로 돌려주면 재정렬 코드를 통째로 지워도 이 테스트가 통과해 버린다.
    when(noticeBoardRepository.findAllByIdIn(List.of(12L, 7L)))
        .thenReturn(List.of(published(7L), published(12L)));

    pinnedNoticeService.replacePinned(new PinnedUpdateRequest(List.of(12L, 7L)), 1L);

    ArgumentCaptor<List<PinnedNotice>> captor = ArgumentCaptor.forClass(List.class);
    verify(pinnedNoticeRepository).saveAll(captor.capture());

    assertThat(captor.getValue())
        .extracting(PinnedNotice::getDisplayOrder)
        .containsExactly(1, 2);
    // 핵심: 요청 배열 순서대로 고정됐는가. 재정렬을 빼면 [7, 12] 가 되어 실패한다.
    assertThat(captor.getValue())
        .extracting(p -> p.getNoticeBoard().getId())
        .containsExactly(12L, 7L);
  }

  @Test
  @DisplayName("저장 전에 기존 고정을 전부 지운다")
  void clearsExistingBeforeInsert() {
    when(noticeBoardRepository.findAllByIdIn(List.of(12L))).thenReturn(List.of(published(12L)));

    pinnedNoticeService.replacePinned(new PinnedUpdateRequest(List.of(12L)), 1L);

    // display_order UNIQUE 를 지키려면 삽입 전에 반드시 비워야 한다 — 순서까지 검증한다.
    InOrder inOrder = inOrder(pinnedNoticeRepository);
    inOrder.verify(pinnedNoticeRepository).deleteAllInBatch();
    inOrder.verify(pinnedNoticeRepository).saveAll(anyList());
  }

  @Test
  @DisplayName("빈 배열이면 전부 해제한다")
  void emptyArrayUnpinsAll() {
    pinnedNoticeService.replacePinned(new PinnedUpdateRequest(List.of()), 1L);

    verify(pinnedNoticeRepository).deleteAllInBatch();
    verify(pinnedNoticeRepository).saveAll(List.of());
  }

  @Test
  @DisplayName("4개를 보내면 거절한다")
  void fourIdsRejected() {
    assertThatThrownBy(
            () ->
                pinnedNoticeService.replacePinned(
                    new PinnedUpdateRequest(List.of(1L, 2L, 3L, 4L)), 1L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(NoticeErrorCode.PINNED_LIMIT_EXCEEDED);
  }

  @Test
  @DisplayName("중복 id 를 보내면 거절한다")
  void duplicateIdsRejected() {
    assertThatThrownBy(
            () -> pinnedNoticeService.replacePinned(new PinnedUpdateRequest(List.of(5L, 5L)), 1L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(NoticeErrorCode.PINNED_NOT_ELIGIBLE);
  }

  @Test
  @DisplayName("없거나 삭제된 공지를 보내면 거절한다")
  void missingNoticeRejected() {
    when(noticeBoardRepository.findAllByIdIn(List.of(12L, 999L)))
        .thenReturn(List.of(published(12L)));

    assertThatThrownBy(
            () ->
                pinnedNoticeService.replacePinned(new PinnedUpdateRequest(List.of(12L, 999L)), 1L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(NoticeErrorCode.PINNED_NOT_ELIGIBLE);
  }

  @Test
  @DisplayName("미공개 공지는 고정할 수 없다")
  void unpublishedNoticeRejected() {
    NoticeBoard hidden =
        NoticeBoard.create("숨김", "본문", NoticeCategory.OPERATION, false, 1L, "홍길동");
    when(noticeBoardRepository.findAllByIdIn(List.of(12L))).thenReturn(List.of(hidden));

    assertThatThrownBy(
            () -> pinnedNoticeService.replacePinned(new PinnedUpdateRequest(List.of(12L)), 1L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(NoticeErrorCode.PINNED_NOT_ELIGIBLE);
  }

  /** id 를 심는다. 재정렬 검증에 필수이며, 이 리포의 기존 테스트 3곳이 쓰는 방식이다. */
  private NoticeBoard published(Long id) {
    NoticeBoard notice =
        NoticeBoard.create("제목" + id, "본문", NoticeCategory.OPERATION, true, 1L, "홍길동");
    ReflectionTestUtils.setField(notice, "id", id);
    return notice;
  }
}
