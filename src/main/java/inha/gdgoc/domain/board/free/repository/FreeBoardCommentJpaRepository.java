package inha.gdgoc.domain.board.free.repository;

import inha.gdgoc.domain.board.free.entity.FreeBoardComment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FreeBoardCommentJpaRepository extends JpaRepository<FreeBoardComment, Long> {

  /**
   * 글 하나의 댓글을 전부 작성순으로 준다. 삭제된 것도 포함한다 — 자식이 달린 삭제 댓글은 자리를 남겨야 트리가
   * 끊기지 않는다. 걸러내는 판단은 서비스가 한다.
   *
   * <p>parent 를 함께 가져온다. 트리를 세울 때 부모 id 를 읽는데, 지연 로딩이면 댓글 수만큼 쿼리가 나간다.
   */
  @Query(
      "SELECT c FROM FreeBoardComment c LEFT JOIN FETCH c.parent"
          + " WHERE c.freeBoard.id = :postId ORDER BY c.createdAt ASC")
  List<FreeBoardComment> findAllByPostId(@Param("postId") Long postId);

  Optional<FreeBoardComment> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByParentIdAndDeletedAtIsNull(Long parentId);
}
