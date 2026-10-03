package inha.gdgoc.domain.club.leader.repository;

import inha.gdgoc.domain.club.leader.entity.ClubLeaderGrant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClubLeaderGrantRepository extends JpaRepository<ClubLeaderGrant, Long> {

  boolean existsByUserIdAndRevokedAtIsNull(Long userId);

  Optional<ClubLeaderGrant> findFirstByUserIdAndRevokedAtIsNull(Long userId);

  @Query("select g from ClubLeaderGrant g join fetch g.user where g.revokedAt is null order by g.grantedAt desc")
  List<ClubLeaderGrant> findAllActiveWithUser();
}
