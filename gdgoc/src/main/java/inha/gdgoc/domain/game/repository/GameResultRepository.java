package inha.gdgoc.domain.game.repository;

import inha.gdgoc.domain.game.entity.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {
}
