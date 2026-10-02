package inha.gdgoc.domain.club.member.repository;

import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

  /** 한 번이라도 합류한 적 있는 행. 지난 명단 계산용이라 탈퇴·강퇴한 행도 포함한다. */
  List<ClubMember> findAllByClubIdAndJoinedAtIsNotNull(Long clubId);

  boolean existsByClubIdAndUserIdAndStatus(Long clubId, Long userId, ClubMemberStatus status);

  boolean existsByClubIdAndUserIdAndStatusIn(
      Long clubId, Long userId, Collection<ClubMemberStatus> statuses);

  long countByClubIdAndStatus(Long clubId, ClubMemberStatus status);

  Optional<ClubMember> findFirstByClubIdAndUserIdAndStatusIn(
      Long clubId, Long userId, Collection<ClubMemberStatus> statuses);

  @Query(
      "select m from ClubMember m join fetch m.user "
          + "where m.club.id = :clubId and m.status = :status order by m.appliedAt asc")
  List<ClubMember> findAllWithUser(
      @Param("clubId") Long clubId, @Param("status") ClubMemberStatus status);

  @Query(
      "select m from ClubMember m join fetch m.club c join fetch c.leader "
          + "where m.user.id = :userId and m.status in :statuses order by m.appliedAt desc")
  List<ClubMember> findMine(
      @Param("userId") Long userId, @Param("statuses") Collection<ClubMemberStatus> statuses);

  /** 목록 카드의 인원 수를 한 번에 센다. [clubId, count] */
  @Query(
      "select m.club.id, count(m) from ClubMember m "
          + "where m.club.id in :clubIds and m.status = :status group by m.club.id")
  List<Object[]> countByClubIds(
      @Param("clubIds") Collection<Long> clubIds, @Param("status") ClubMemberStatus status);
}
