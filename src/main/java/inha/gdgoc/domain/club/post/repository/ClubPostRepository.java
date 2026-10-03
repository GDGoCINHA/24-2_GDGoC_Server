package inha.gdgoc.domain.club.post.repository;

import inha.gdgoc.domain.club.post.entity.ClubPost;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubPostRepository extends JpaRepository<ClubPost, Long> {

  /** 피드 한 페이지의 게시글. 순서는 부르는 쪽이 맞춘다. */
  @Query("select p from ClubPost p join fetch p.author where p.id in :ids")
  List<ClubPost> findAllWithAuthor(@Param("ids") Collection<Long> ids);
}
