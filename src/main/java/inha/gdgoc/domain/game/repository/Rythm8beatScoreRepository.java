package inha.gdgoc.domain.game.repository;

import inha.gdgoc.domain.game.entity.Rythm8beatScore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Rythm8beatScoreRepository extends JpaRepository<Rythm8beatScore, Long> {

    Optional<Rythm8beatScore> findByPhoneNumber(String phoneNumber);

    List<Rythm8beatScore> findAllByOrderByScoreDescUpdatedAtAsc();

    List<Rythm8beatScore> findTop3ByOrderByScoreDescUpdatedAtAsc();

    @Query("SELECT COUNT(r) FROM Rythm8beatScore r WHERE r.score > :score")
    long countByScoreGreaterThan(@Param("score") int score);
}
