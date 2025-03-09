package inha.gdgoc.domain.game.repository;

import inha.gdgoc.domain.game.entity.GameUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameUserRepository extends JpaRepository<GameUser, Long> {
    List<GameUser> findAllByCreatedAtBetweenOrderByTypingSpeedAsc(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
