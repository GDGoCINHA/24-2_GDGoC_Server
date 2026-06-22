package inha.gdgoc.domain.board.notice.service;

import inha.gdgoc.domain.board.notice.dto.request.PinnedUpdateRequest;
import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.board.notice.enums.ArticleStatusEnum;
import inha.gdgoc.domain.board.notice.exception.NoticeErrorCode;
import inha.gdgoc.domain.board.notice.repository.NoticeBoardRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PinnedService {

    private final NoticeBoardRepository noticeBoardRepository;

    @Transactional
    public void updatePinnedNotices(String boardType, PinnedUpdateRequest req) {
        if (!"notices".equalsIgnoreCase(boardType)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST);
        }

        List<UUID> pinnedIds = req.pinnedArticleIds();
        if (pinnedIds == null || pinnedIds.size() > 3) {
            throw new BusinessException(NoticeErrorCode.NOTICE_PIN_LIMIT_EXCEEDED);
        }

        noticeBoardRepository.clearAllPinned();

        if (pinnedIds != null && !pinnedIds.isEmpty()) {
            for (UUID id : pinnedIds) {
                NoticeBoard board = noticeBoardRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

                if (board.getStatus() != ArticleStatusEnum.PUBLISHED) {
                    throw new BusinessException(GlobalErrorCode.BAD_REQUEST);
                }

                board.update(null, null, null, true, null);
            }
        }
    }
}
