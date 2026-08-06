package inha.gdgoc.domain.board.notice.service;

import inha.gdgoc.domain.board.notice.dto.request.PinnedUpdateRequest;
import inha.gdgoc.domain.board.notice.dto.response.NoticeSummaryResponse;
import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.board.notice.entity.PinnedNotice;
import inha.gdgoc.domain.board.notice.exception.NoticeErrorCode;
import inha.gdgoc.domain.board.notice.repository.NoticeBoardRepository;
import inha.gdgoc.domain.board.notice.repository.PinnedNoticeRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상단 고정 공지.
 *
 * <p>개별 pin/unpin/reorder 로 나누지 않는다. display_order 가 UNIQUE 이고 CHECK(1~3)이 중간 값을 막아, 1↔2
 * 교체 같은 조작이 DEFERRABLE 제약을 요구하기 때문이다. 전체 교체는 삭제 후 삽입이라 중간 상태 자체가 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PinnedNoticeService {

  private static final int MAX_PINNED = 3;

  private final PinnedNoticeRepository pinnedNoticeRepository;
  private final NoticeBoardRepository noticeBoardRepository;
  private final NoticeBoardService noticeBoardService;

  public List<NoticeSummaryResponse> getPinned() {
    return pinnedNoticeRepository.findAllOrdered().stream()
        .map(p -> noticeBoardService.toSummaryResponse(p.getNoticeBoard()))
        .toList();
  }

  @Transactional
  public void replacePinned(PinnedUpdateRequest req, Long userId) {
    List<Long> ids = req.noticeIds();

    if (ids.size() > MAX_PINNED) {
      throw new BusinessException(NoticeErrorCode.PINNED_LIMIT_EXCEEDED);
    }
    if (new HashSet<>(ids).size() != ids.size()) {
      throw new BusinessException(NoticeErrorCode.PINNED_NOT_ELIGIBLE);
    }

    List<NoticeBoard> notices = ids.isEmpty() ? List.of() : noticeBoardRepository.findAllByIdIn(ids);

    // 요청한 ID 중 하나라도 조회되지 않으면(없거나 삭제됨) 거절한다.
    if (notices.size() != ids.size()) {
      throw new BusinessException(NoticeErrorCode.PINNED_NOT_ELIGIBLE);
    }
    if (notices.stream().anyMatch(n -> !n.isPublished())) {
      throw new BusinessException(NoticeErrorCode.PINNED_NOT_ELIGIBLE);
    }

    // IN 절의 결과 순서는 DB 가 보장하지 않는다. 요청 배열 순서로 되돌린 뒤 display_order 를 매긴다.
    Map<Long, NoticeBoard> byId =
        notices.stream().collect(Collectors.toMap(NoticeBoard::getId, n -> n));
    List<NoticeBoard> ordered = ids.stream().map(byId::get).toList();

    pinnedNoticeRepository.deleteAllInBatch();

    List<PinnedNotice> pinned = new ArrayList<>();
    for (int i = 0; i < ordered.size(); i++) {
      pinned.add(PinnedNotice.create(ordered.get(i), i + 1, userId));
    }
    pinnedNoticeRepository.saveAll(pinned);
  }
}
