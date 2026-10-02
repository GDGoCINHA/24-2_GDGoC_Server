package inha.gdgoc.domain.club.reaction.repository;

import inha.gdgoc.domain.club.reaction.entity.ClubComment;
import inha.gdgoc.domain.club.reaction.enums.ClubTargetType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubCommentRepository extends JpaRepository<ClubComment, Long> {

  /** 지운 댓글은 뺀다. 오래된 것부터. */
  @Query(
      "select c from ClubComment c join fetch c.author "
          + "where c.targetType = :type and c.targetId = :targetId and c.deletedAt is null "
          + "order by c.createdAt asc, c.id asc")
  List<ClubComment> findAlive(
      @Param("type") ClubTargetType type, @Param("targetId") long targetId);

  /** 목록 화면용 한 번에 세기. 지운 댓글은 세지 않는다. [targetId, count] */
  @Query(
      "select c.targetId, count(c) from ClubComment c "
          + "where c.targetType = :type and c.targetId in :ids and c.deletedAt is null "
          + "group by c.targetId")
  List<Object[]> countByTargets(
      @Param("type") ClubTargetType type, @Param("ids") Collection<Long> ids);
}
