package inha.gdgoc.domain.board.free.comment.service;

import inha.gdgoc.domain.board.free.comment.dto.request.CommentCreateRequest;
import inha.gdgoc.domain.board.free.comment.dto.request.CommentUpdateRequest;
import inha.gdgoc.domain.board.free.comment.dto.response.CommentResponse;
import inha.gdgoc.domain.board.free.comment.entity.Comment;
import inha.gdgoc.domain.board.free.comment.exception.CommentErrorCode;
import inha.gdgoc.domain.board.free.comment.repository.CommentRepository;
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
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /* ---------- Create ---------- */
    public CommentResponse create(Long authorId, Long postId, CommentCreateRequest req) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.AUTHOR_NOT_FOUND));

        Comment saved = commentRepository.save(new Comment(post, author, req.content()));
        return CommentResponse.from(saved);
    }

    /* ---------- Read ---------- */
    @Transactional(readOnly = true)
    public Page<CommentResponse> list(Long postId, Pageable pageable) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(PostErrorCode.POST_NOT_FOUND);
        }
        return commentRepository.findAllByPostIdOrderByCreatedAtAsc(postId, pageable)
                .map(CommentResponse::from);
    }

    /* ---------- Update ---------- */
    public CommentResponse update(
            Long requesterId, UserRole requesterRole, Long postId, Long commentId, CommentUpdateRequest req) {
        Comment comment = findInPostOrThrow(postId, commentId);
        requireEditable(comment, requesterId, requesterRole);

        comment.update(req.content());
        // 커밋 전에 flush를 강제해 @LastModifiedDate(updated_at)를 반영한 뒤 응답 생성
        commentRepository.flush();
        return CommentResponse.from(comment);
    }

    /* ---------- Delete ---------- */
    public void delete(Long requesterId, UserRole requesterRole, Long postId, Long commentId) {
        Comment comment = findInPostOrThrow(postId, commentId);
        requireEditable(comment, requesterId, requesterRole);

        commentRepository.delete(comment);
    }

    // 댓글을 조회 + postId에 실제로 속한 댓글인지 체크
    private Comment findInPostOrThrow(Long postId, Long commentId) {
        Comment comment = commentRepository.findWithAuthorById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
        if (!comment.belongsToPost(postId)) {
            throw new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }

    // 작성자 본인 또는 ADMIN 이상만 수정/삭제 가능
    private void requireEditable(Comment comment, Long requesterId, UserRole requesterRole) {
        boolean owner = comment.isAuthoredBy(requesterId);
        boolean admin = UserRole.hasAtLeast(requesterRole, UserRole.ADMIN);
        if (!owner && !admin) {
            throw new BusinessException(CommentErrorCode.NOT_COMMENT_OWNER);
        }
    }
}
