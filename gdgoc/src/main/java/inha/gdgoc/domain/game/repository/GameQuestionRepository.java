package inha.gdgoc.domain.game.repository;

import inha.gdgoc.domain.game.entity.GameQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameQuestionRepository extends JpaRepository<GameQuestion, Long> {
    List<GameQuestion> findByLanguage(String language);
}
