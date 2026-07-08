package inha.gdgoc.domain.board.free.like.service;

import inha.gdgoc.domain.board.free.entity.Post;
import inha.gdgoc.domain.board.free.exception.PostErrorCode;
import inha.gdgoc.domain.board.free.like.dto.response.LikeResponse;
import inha.gdgoc.domain.board.free.like.entity.PostLike;
import inha.gdgoc.domain.board.free.like.exception.LikeErrorCode;
import inha.gdgoc.domain.board.free.like.repository.PostLikeRepository;
import inha.gdgoc.domain.board.free.repository.PostRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /* ---------- 좋아요 ---------- */
    public LikeResponse like(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new BusinessException(LikeErrorCode.ALREADY_LIKED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(LikeErrorCode.USER_NOT_FOUND));

        postLikeRepository.save(new PostLike(post, user));
        return buildResponse(postId, userId);
    }

    /* ---------- 좋아요 취소 ---------- */
    public LikeResponse unlike(Long userId, Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(PostErrorCode.POST_NOT_FOUND);
        }
        if (!postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new BusinessException(LikeErrorCode.LIKE_NOT_FOUND);
        }
        postLikeRepository.deleteByPostIdAndUserId(postId, userId);
        return buildResponse(postId, userId);
    }

    /* ---------- 좋아요 수/상태 조회 ---------- */
    @Transactional(readOnly = true)
    public LikeResponse status(Long userId, Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(PostErrorCode.POST_NOT_FOUND);
        }
        return buildResponse(postId, userId);
    }

    // 현재 좋아요 수 + 요청자가 눌렀는지 계산
    private LikeResponse buildResponse(Long postId, Long userId) {
        long count = postLikeRepository.countByPostId(postId);
        boolean liked = userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);
        return new LikeResponse(postId, count, liked);
    }
}
