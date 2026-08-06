package inha.gdgoc.domain.board.free.repository;

import inha.gdgoc.domain.board.free.entity.FreeBoard;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FreeBoardJpaRepository extends JpaRepository<FreeBoard, Long> {

  Optional<FreeBoard> findByIdAndDeletedAtIsNull(Long id);

  @Modifying
  @Query("UPDATE FreeBoard f SET f.viewCount = f.viewCount + 1 WHERE f.id = :id")
  void increaseViewCount(@Param("id") Long id);
}
