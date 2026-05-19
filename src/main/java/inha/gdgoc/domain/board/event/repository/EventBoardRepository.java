package inha.gdgoc.domain.board.event.repository;

import inha.gdgoc.domain.board.event.entity.EventBoard;
import java.util.Optional;

public interface EventBoardRepository extends EventBoardQueryDslRepository {

  Optional<EventBoard> findById(Long id);

  EventBoard save(EventBoard board);

  void delete(EventBoard board);
}
