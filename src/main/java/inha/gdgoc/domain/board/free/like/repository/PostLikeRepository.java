package inha.gdgoc.domain.board.free.like.repository;

import inha.gdgoc.domain.board.free.like.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    void deleteAllByPostId(Long postId);
}
