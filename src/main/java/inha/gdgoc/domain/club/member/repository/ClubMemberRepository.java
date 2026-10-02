package inha.gdgoc.domain.club.member.repository;

import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

  /** 한 번이라도 합류한 적 있는 행. 지난 명단 계산용이라 탈퇴·강퇴한 행도 포함한다. */
  List<ClubMember> findAllByClubIdAndJoinedAtIsNotNull(Long clubId);

  boolean existsByClubIdAndUserIdAndStatus(Long clubId, Long userId, ClubMemberStatus status);

  long countByClubIdAndStatus(Long clubId, ClubMemberStatus status);
}
