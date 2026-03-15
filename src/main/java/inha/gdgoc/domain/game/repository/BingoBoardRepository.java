package inha.gdgoc.domain.game.repository;

import inha.gdgoc.domain.game.entity.BingoBoard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BingoBoardRepository extends JpaRepository<BingoBoard, Long> {
    Optional<BingoBoard> findByTeamNumber(Integer teamNumber);
    List<BingoBoard> findAllByOrderByTeamNumberAsc();
}
