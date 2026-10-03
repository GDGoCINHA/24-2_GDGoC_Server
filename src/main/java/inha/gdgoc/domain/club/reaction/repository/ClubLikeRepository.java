package inha.gdgoc.domain.club.reaction.repository;

import inha.gdgoc.domain.club.reaction.entity.ClubLike;
import inha.gdgoc.domain.club.reaction.enums.ClubTargetType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubLikeRepository extends JpaRepository<ClubLike, Long> {

  boolean existsByTargetTypeAndTargetIdAndUserId(
      ClubTargetType targetType, long targetId, Long userId);

  long countByTargetTypeAndTargetId(ClubTargetType targetType, long targetId);

  @Modifying
  @Query(
      "delete from ClubLike l "
          + "where l.targetType = :type and l.targetId = :targetId and l.user.id = :userId")
  int deleteMine(
      @Param("type") ClubTargetType type,
      @Param("targetId") long targetId,
      @Param("userId") Long userId);

  /** 목록 화면용 한 번에 세기. [targetId, count] */
  @Query(
      "select l.targetId, count(l) from ClubLike l "
          + "where l.targetType = :type and l.targetId in :ids group by l.targetId")
  List<Object[]> countByTargets(
      @Param("type") ClubTargetType type, @Param("ids") Collection<Long> ids);

  @Query(
      "select l.targetId from ClubLike l "
          + "where l.targetType = :type and l.targetId in :ids and l.user.id = :userId")
  List<Long> findLikedTargets(
      @Param("type") ClubTargetType type,
      @Param("ids") Collection<Long> ids,
      @Param("userId") Long userId);
}
