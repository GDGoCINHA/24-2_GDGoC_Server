package inha.gdgoc.domain.board.free.repository;

import inha.gdgoc.domain.board.free.entity.FreeBoard;
import java.util.Optional;

public interface FreeBoardRepository extends FreeBoardQueryDslRepository {

  Optional<FreeBoard> findById(Long id);

  FreeBoard save(FreeBoard post);

  void increaseViewCount(Long id);
}
