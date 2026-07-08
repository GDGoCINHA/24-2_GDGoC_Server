package inha.gdgoc.domain.board.free.repository;

import inha.gdgoc.domain.board.free.entity.Post;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 목록: 최신순 + 작성자 즉시 로딩(N+1 방지)
    @EntityGraph(attributePaths = "author")
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 단건: 작성자 함께 로딩
    @EntityGraph(attributePaths = "author")
    Optional<Post> findWithAuthorById(Long id);
}
