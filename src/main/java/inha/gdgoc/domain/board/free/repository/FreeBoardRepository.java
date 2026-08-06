package inha.gdgoc.domain.board.free.repository;

import inha.gdgoc.domain.board.free.entity.FreeBoard;
import java.util.Optional;

public interface FreeBoardRepository extends FreeBoardQueryDslRepository {

  Optional<FreeBoard> findById(Long id);

  /** 복원 대상은 이미 삭제된 글이라 findById 로는 찾을 수 없다. */
  Optional<FreeBoard> findDeletedById(Long id);

  FreeBoard save(FreeBoard post);

  void increaseViewCount(Long id);
}
