package inha.gdgoc.domain.board.event.repository;

import inha.gdgoc.domain.board.event.entity.EventBoard;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventBoardJpaRepository extends JpaRepository<EventBoard, Long> {

  Optional<EventBoard> findByIdAndDeletedAtIsNull(Long id);

  Optional<EventBoard> findByIdAndDeletedAtIsNotNull(Long id);
}
