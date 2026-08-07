package inha.gdgoc.domain.board.free.repository;

import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.free.entity.FreeBoard;
import inha.gdgoc.domain.user.enums.UserRole;
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

  /**
   * 삭제된 글을 삭제 시각 최신순으로 준다.
   *
   * <p>ORGANIZER 이상은 전부 보고, 그 아래는 자기가 쓴 글만 본다. 자유게시판은 MEMBER 도 글을 쓰므로 이 필터가 없으면
   * 휴지통이 남의 삭제글까지 드러낸다. 공지의 findDeletedNotices 와 같은 규칙이다.
   */
  Page<FreeBoard> findDeletedPosts(Long userId, UserRole userRole, Pageable pageable);
}
