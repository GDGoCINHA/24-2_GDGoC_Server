package inha.gdgoc.domain.club.completion.repository;

import inha.gdgoc.domain.club.completion.entity.ClubRestWeek;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRestWeekRepository extends JpaRepository<ClubRestWeek, Long> {

  List<ClubRestWeek> findAllByClubIdOrderByWeekStart(Long clubId);
}
