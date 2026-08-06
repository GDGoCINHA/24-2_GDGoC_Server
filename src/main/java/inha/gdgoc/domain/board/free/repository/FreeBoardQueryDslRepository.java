package inha.gdgoc.domain.board.free.repository;

import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.free.entity.FreeBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FreeBoardQueryDslRepository {

  /**
   * 삭제되지 않은 글을 최신순으로 준다.
   *
   * <p>공지의 findVisibleNotices 와 달리 userRole 을 받지 않는다. 자유게시판에는 임시저장이 없어 역할에 따라 보이는
   * 글이 달라지지 않는다 — 회원이면 모두 같은 목록을 본다.
   */
  Page<FreeBoard> findVisiblePosts(SearchType searchType, String keyword, Pageable pageable);
}
