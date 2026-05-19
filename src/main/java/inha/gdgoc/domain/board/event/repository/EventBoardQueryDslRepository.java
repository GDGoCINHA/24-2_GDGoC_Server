package inha.gdgoc.domain.board.event.repository;

import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.board.event.enums.SearchType;
import inha.gdgoc.domain.user.enums.TeamType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventBoardQueryDslRepository {

  Page<EventBoard> findVisibleBoards(
      TeamType userTeam, SearchType searchType, String keyword, Pageable pageable);
}
