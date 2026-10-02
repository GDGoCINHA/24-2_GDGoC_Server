package inha.gdgoc.domain.club.club.repository;

import inha.gdgoc.domain.club.club.entity.ClubTerm;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubTermRepository extends JpaRepository<ClubTerm, Long> {

  /** 기수에는 날짜가 없으므로 「가장 최근」 = 가장 나중에 만든 기수다. */
  Optional<ClubTerm> findTopByOrderByIdDesc();
}
