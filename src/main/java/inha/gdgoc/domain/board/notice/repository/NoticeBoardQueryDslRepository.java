package inha.gdgoc.domain.board.notice.repository;

import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.user.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeBoardQueryDslRepository {

  Page<NoticeBoard> findVisibleNotices(
      UserRole userRole, SearchType searchType, String keyword, Pageable pageable);

  Page<NoticeBoard> findDeletedNotices(Long userId, UserRole userRole, Pageable pageable);
}
