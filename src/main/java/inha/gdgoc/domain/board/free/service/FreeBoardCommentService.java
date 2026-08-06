package inha.gdgoc.domain.board.free.service;

import inha.gdgoc.domain.board.free.dto.request.FreeBoardCommentCreateRequest;
import inha.gdgoc.domain.board.free.dto.request.FreeBoardCommentUpdateRequest;
import inha.gdgoc.domain.board.free.dto.response.FreeBoardCommentResponse;
import inha.gdgoc.domain.board.free.entity.FreeBoard;
import inha.gdgoc.domain.board.free.entity.FreeBoardComment;
import inha.gdgoc.domain.board.free.repository.FreeBoardCommentJpaRepository;
import inha.gdgoc.domain.board.free.repository.FreeBoardRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FreeBoardCommentService {

  private final FreeBoardCommentJpaRepository commentRepository;
  private final FreeBoardRepository freeBoardRepository;
  private final UserRepository userRepository;

  /**
   * 글 하나의 댓글을 트리로 준다. 페이지네이션은 없다 — 자유게시판 한 글의 댓글이 한 화면에 다 들어가지 않을 만큼
   * 쌓이면 그때 넣는다.
   *
   * <p>삭제된 댓글의 처리가 이 메서드의 핵심이다. 자식이 남은 최상위 댓글은 지워도 자리를 남긴다(내용은 감춘다).
   * 자식이 없으면 목록에서 아예 뺀다. 대댓글은 자식이 없으므로 지우면 항상 사라진다.
   */
  public List<FreeBoardCommentResponse> listComments(Long postId) {
    requireVisiblePost(postId);

    List<FreeBoardComment> all = commentRepository.findAllByPostId(postId);

    Map<Long, List<FreeBoardComment>> childrenByParent = new LinkedHashMap<>();
    List<FreeBoardComment> roots = new ArrayList<>();
    for (FreeBoardComment comment : all) {
      if (comment.isReply()) {
        childrenByParent
            .computeIfAbsent(comment.getParent().getId(), key -> new ArrayList<>())
            .add(comment);
      } else {
        roots.add(comment);
      }
    }

    List<FreeBoardCommentResponse> result = new ArrayList<>();
    for (FreeBoardComment root : roots) {
      List<FreeBoardCommentResponse> replies =
          childrenByParent.getOrDefault(root.getId(), List.of()).stream()
              .filter(reply -> !reply.isDeleted())
              .map(reply -> FreeBoardCommentResponse.of(reply, List.of()))
              .toList();

      // 지워졌는데 남은 대댓글도 없으면 보여줄 것이 없다.
      if (root.isDeleted() && replies.isEmpty()) continue;

      result.add(FreeBoardCommentResponse.of(root, replies));
    }
    return result;
  }

  @Transactional
  public Long createComment(Long postId, FreeBoardCommentCreateRequest req, Long authorId) {
    FreeBoard post = requireVisiblePost(postId);

    // 부모를 먼저 본다. 어차피 거절할 요청이면 작성자 조회를 할 이유가 없다.
    FreeBoardComment parent = req.parentId() == null ? null : requireParent(req.parentId(), postId);

    User author =
        userRepository
            .findById(authorId)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    FreeBoardComment comment =
        FreeBoardComment.create(post, parent, req.content(), authorId, author.getName());

    return commentRepository.save(comment).getId();
  }

  @Transactional
  public void updateComment(
      Long commentId, FreeBoardCommentUpdateRequest req, Long userId, UserRole userRole) {
    FreeBoardComment comment = requireVisibleComment(commentId);
    requireAuthorOrOrganizer(comment, userId, userRole);
    comment.update(req.content());
  }

  @Transactional
  public void deleteComment(Long commentId, Long userId, UserRole userRole) {
    FreeBoardComment comment = requireVisibleComment(commentId);
    requireAuthorOrOrganizer(comment, userId, userRole);
    comment.softDelete();
  }

  private FreeBoard requireVisiblePost(Long postId) {
    return freeBoardRepository
        .findById(postId)
        .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
  }

  private FreeBoardComment requireVisibleComment(Long commentId) {
    return commentRepository
        .findByIdAndDeletedAtIsNull(commentId)
        .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
  }

  /**
   * 부모 댓글은 살아 있어야 하고 같은 글에 속해야 한다.
   *
   * <p>글 검사를 빼면 A 글의 댓글에 B 글의 댓글을 답글로 달 수 있다. id 만 바꿔 부르면 되므로 화면에서 막는 것으로는
   * 부족하다.
   */
  private FreeBoardComment requireParent(Long parentId, Long postId) {
    FreeBoardComment parent = requireVisibleComment(parentId);
    if (!parent.getFreeBoard().getId().equals(postId)) {
      throw new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }
    return parent;
  }

  /** 글과 같은 규칙이다. 작성자 본인이거나 ORGANIZER 이상이어야 한다. */
  private void requireAuthorOrOrganizer(
      FreeBoardComment comment, Long userId, UserRole userRole) {
    if (UserRole.hasAtLeast(userRole, UserRole.ORGANIZER)) return;
    if (!comment.getAuthorId().equals(userId)) {
      throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
    }
  }
}
