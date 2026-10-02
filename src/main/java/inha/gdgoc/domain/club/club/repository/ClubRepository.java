package inha.gdgoc.domain.club.club.repository;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubRecruitStatus;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubRepository extends JpaRepository<Club, Long> {

  /** 게시판 목록. 숨김은 빼고, 비어 있는 조건은 무시한다. */
  @Query(
      value =
          "select c from Club c join fetch c.leader "
              + "where (:hidden is null or c.status <> :hidden) "
              + "and (:category is null or c.category = :category) "
              + "and (:recruitStatus is null or c.recruitStatus = :recruitStatus) "
              + "and (:keyword is null or lower(c.name) like lower(concat('%', :keyword, '%')))",
      countQuery =
          "select count(c) from Club c "
              + "where (:hidden is null or c.status <> :hidden) "
              + "and (:category is null or c.category = :category) "
              + "and (:recruitStatus is null or c.recruitStatus = :recruitStatus) "
              + "and (:keyword is null or lower(c.name) like lower(concat('%', :keyword, '%')))")
  Page<Club> search(
      @Param("hidden") ClubStatus hidden,
      @Param("category") ClubCategory category,
      @Param("recruitStatus") ClubRecruitStatus recruitStatus,
      @Param("keyword") String keyword,
      Pageable pageable);

  List<Club> findAllByLeaderIdIn(Collection<Long> leaderIds);
}
