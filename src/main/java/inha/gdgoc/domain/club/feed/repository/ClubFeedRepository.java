package inha.gdgoc.domain.club.feed.repository;

import inha.gdgoc.domain.club.post.entity.ClubPost;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 팀 피드 한 페이지의 순서. 게시글과 활동 기록은 다른 테이블이라 JPQL 로는 합칠 수 없어(UNION 없음) 네이티브로 (종류, id) 만 뽑는다.
 * 본문·사진·반응 수는 그 id 묶음으로 따로 읽는다.
 *
 * <p>정렬은 올라온 시각(created_at) 최신순이다. 활동일이 아니라서, 지난 모임을 늦게 올려도 피드 맨 위에 보인다.
 */
public interface ClubFeedRepository extends Repository<ClubPost, Long> {

  /** [kind('POST'|'ACTIVITY'), id]. 같은 시각이면 id 큰 것부터. */
  @Query(
      value =
          """
          select f.kind, f.id from (
              select 'POST' as kind, p.id as id, p.created_at as created_at
                from club_post p
               where p.club_id = :clubId and p.deleted_at is null
              union all
              select 'ACTIVITY' as kind, a.id as id, a.created_at as created_at
                from club_activity a
               where a.club_id = :clubId
          ) f
          order by f.created_at desc, f.id desc
          limit :limit offset :offset
          """,
      nativeQuery = true)
  List<Object[]> findTeamFeedPage(
      @Param("clubId") Long clubId, @Param("limit") int limit, @Param("offset") long offset);

  @Query("select count(p) from ClubPost p where p.club.id = :clubId and p.deletedAt is null")
  long countPosts(@Param("clubId") Long clubId);

  @Query("select count(a) from ClubActivity a where a.club.id = :clubId")
  long countActivities(@Param("clubId") Long clubId);
}
