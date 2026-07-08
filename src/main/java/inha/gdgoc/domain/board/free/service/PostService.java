package inha.gdgoc.domain.board.free.service;

import inha.gdgoc.domain.board.free.comment.repository.CommentRepository;
import inha.gdgoc.domain.board.free.dto.request.PostCreateRequest;
import inha.gdgoc.domain.board.free.dto.request.PostUpdateRequest;
import inha.gdgoc.domain.board.free.dto.response.PostResponse;
import inha.gdgoc.domain.board.free.dto.response.PostSummaryResponse;
import inha.gdgoc.domain.board.free.entity.Post;
import inha.gdgoc.domain.board.free.exception.PostErrorCode;
import inha.gdgoc.domain.board.free.repository.PostRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    /* ---------- Create ---------- */
    public PostResponse create(Long authorId, PostCreateRequest req) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.AUTHOR_NOT_FOUND));

        Post saved = postRepository.save(new Post(author, req.title().trim(), req.content()));
        return PostResponse.from(saved);
    }

    /* ---------- Read ---------- */
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> list(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(PostSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long postId) {
        Post post = findOrThrow(postId);
        return PostResponse.from(post);
    }

    /* ---------- Update ---------- */
    public PostResponse update(Long requesterId, UserRole requesterRole, Long postId, PostUpdateRequest req) {
        Post post = findOrThrow(postId);
        requireEditable(post, requesterId, requesterRole);

        post.update(req.title().trim(), req.content());
        // 커밋 전에 flush를 강제해 @LastModifiedDate(updated_at)를 반영한 뒤 응답 생성
        postRepository.flush();
        return PostResponse.from(post);
    }

    /* ---------- Delete ---------- */
    public void delete(Long requesterId, UserRole requesterRole, Long postId) {
        Post post = findOrThrow(postId);
        requireEditable(post, requesterId, requesterRole);

        // 게시글의 댓글을 먼저 제거
        commentRepository.deleteAllByPostId(postId);
        postRepository.delete(post);
    }

    private Post findOrThrow(Long postId) {
        return postRepository.findWithAuthorById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
    }

    // 작성자 본인 또는 ADMIN 이상만 수정/삭제 가능
    private void requireEditable(Post post, Long requesterId, UserRole requesterRole) {
        boolean owner = post.isAuthoredBy(requesterId);
        boolean admin = UserRole.hasAtLeast(requesterRole, UserRole.ADMIN);
        if (!owner && !admin) {
            throw new BusinessException(PostErrorCode.NOT_POST_OWNER);
        }
    }
}
