package inha.gdgoc.domain.board.notice.repository;

import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.board.notice.dto.request.NoticeSearchCondition;
import inha.gdgoc.domain.board.notice.enums.CategoryEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NoticeBoardQueryDslRepository {

    Page<NoticeBoard> findVisibleBoards(NoticeSearchCondition condition, Pageable pageable);

    List<NoticeBoard> findPinnedNotices();

    Page<NoticeBoard> findDeletedBoards(NoticeSearchCondition condition, Pageable pageable);

    Optional<NoticeBoard> findPrevNotice(Long currentArticleNumber, CategoryEnum category);

    Optional<NoticeBoard> findNextNotice(Long currentArticleNumber, CategoryEnum category);
}
