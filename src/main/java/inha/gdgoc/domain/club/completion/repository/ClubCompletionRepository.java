package inha.gdgoc.domain.club.completion.repository;

import inha.gdgoc.domain.club.completion.entity.ClubCompletion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubCompletionRepository extends JpaRepository<ClubCompletion, Long> {

  Optional<ClubCompletion> findByClubId(Long clubId);
}
