package inha.gdgoc.domain.club.reaction.repository;

import inha.gdgoc.domain.club.post.entity.ClubPost;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 좋아요·댓글 대상이 있는지, 어느 팀 것인지. 대상 테이블은 B 의 것이라 B 의 리포지토리를 늘리지 않고 여기 둔다.
 *
 * <p>{@code club_like}·{@code club_comment} 의 target_id 에는 FK 가 없다(대상 종류가 둘이라). 없는 대상에 쌓이지 않게 쓰기 전에 여기서
 * 확인한다.
 */
public interface ClubReactionTargetRepository extends Repository<ClubPost, Long> {

  @Query("select p.club.id from ClubPost p where p.id = :id and p.deletedAt is null")
  Optional<Long> findPostClubId(@Param("id") Long id);

  @Query("select a.club.id from ClubActivity a where a.id = :id")
  Optional<Long> findActivityClubId(@Param("id") Long id);
}
