package inha.gdgoc.domain.board.event.repository;

import inha.gdgoc.domain.board.event.entity.EventBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventBoardJpaRepository extends JpaRepository<EventBoard, Long> {}
