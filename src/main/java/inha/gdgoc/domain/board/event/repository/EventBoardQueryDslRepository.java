package inha.gdgoc.domain.board.event.repository;

import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventBoardQueryDslRepository {

  Page<EventBoard> findVisibleBoards(
      TeamType userTeam, UserRole userRole, SearchType searchType, String keyword, Pageable pageable);

  Page<EventBoard> findDeletedBoards(TeamType userTeam, UserRole userRole, Pageable pageable);
}
