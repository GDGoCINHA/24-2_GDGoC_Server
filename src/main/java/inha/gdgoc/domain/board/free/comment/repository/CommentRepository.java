package inha.gdgoc.domain.board.free.comment.repository;

import inha.gdgoc.domain.board.free.comment.entity.Comment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = "author")
    Page<Comment> findAllByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Optional<Comment> findWithAuthorById(Long id);

    void deleteAllByPostId(Long postId);
}
