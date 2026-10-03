package inha.gdgoc.domain.club.export.repository;

import inha.gdgoc.domain.club.club.entity.Club;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** 운영진 현황 표의 팀 목록. 소모임의 주인은 A 라 A 의 리포지토리를 늘리지 않고 C 쪽에 따로 둔다. */
public interface ClubAdminQueryRepository extends Repository<Club, Long> {

  /** 숨김 팀도 포함한다 — 운영진은 전부 봐야 한다. */
  @Query(
      "select c from Club c join fetch c.leader join fetch c.term "
          + "where c.term.id = :termId order by c.id asc")
  List<Club> findAllByTerm(@Param("termId") Long termId);
}
