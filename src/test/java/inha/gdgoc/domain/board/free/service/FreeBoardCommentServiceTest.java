package inha.gdgoc.domain.board.free.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.board.free.dto.request.FreeBoardCommentCreateRequest;
import inha.gdgoc.domain.board.free.dto.request.FreeBoardCommentUpdateRequest;
import inha.gdgoc.domain.board.free.dto.response.FreeBoardCommentResponse;
import inha.gdgoc.domain.board.free.entity.FreeBoard;
import inha.gdgoc.domain.board.free.entity.FreeBoardComment;
import inha.gdgoc.domain.board.free.repository.FreeBoardCommentJpaRepository;
import inha.gdgoc.domain.board.free.repository.FreeBoardRepository;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** 댓글 트리 구성과 삭제 처리, 권한 경계. */
@ExtendWith(MockitoExtension.class)
class FreeBoardCommentServiceTest {

  @Mock private FreeBoardCommentJpaRepository commentRepository;
  @Mock private FreeBoardRepository freeBoardRepository;
  @Mock private UserRepository userRepository;

  private FreeBoardCommentService service;
  private FreeBoard post;

  @BeforeEach
  void setUp() {
    service = new FreeBoardCommentService(commentRepository, freeBoardRepository, userRepository);
    post = FreeBoard.create("제목", "본문", 1L, "글쓴이");
    ReflectionTestUtils.setField(post, "id", 1L);
  }

  @Test
  @DisplayName("대댓글에 답글을 달면 같은 최상위 댓글에 붙는다 — 깊이는 1단계다")
  void replyToReplyIsFlattenedToTopLevel() {
    FreeBoardComment root = comment(10L, null, "댓글", 1L);
    FreeBoardComment reply = comment(11L, root, "대댓글", 2L);

    FreeBoardComment replyToReply = FreeBoardComment.create(post, reply, "답글의 답글", 3L, "셋째");

    assertThat(replyToReply.getParent()).isSameAs(root);
    assertThat(replyToReply.isReply()).isTrue();
  }

  @Test
  @DisplayName("삭제된 댓글도 대댓글이 남아 있으면 자리를 남기되 내용과 작성자를 감춘다")
  void deletedRootWithRepliesBecomesTombstone() {
    FreeBoardComment root = comment(10L, null, "지워질 댓글", 1L);
    root.softDelete();
    FreeBoardComment reply = comment(11L, root, "살아있는 대댓글", 2L);

    when(freeBoardRepository.findById(1L)).thenReturn(Optional.of(post));
    when(commentRepository.findAllByPostId(1L)).thenReturn(List.of(root, reply));

    List<FreeBoardCommentResponse> result = service.listComments(1L);

    assertThat(result).hasSize(1);
    FreeBoardCommentResponse tombstone = result.get(0);
    assertThat(tombstone.deleted()).isTrue();
    assertThat(tombstone.content()).isNull();
    assertThat(tombstone.authorId()).isNull();
    assertThat(tombstone.authorName()).isNull();
    assertThat(tombstone.replies()).hasSize(1);
    assertThat(tombstone.replies().get(0).content()).isEqualTo("살아있는 대댓글");
  }

  @Test
  @DisplayName("삭제된 댓글에 남은 대댓글이 없으면 목록에서 아예 빠진다")
  void deletedRootWithoutRepliesDisappears() {
    FreeBoardComment root = comment(10L, null, "지워질 댓글", 1L);
    root.softDelete();
    FreeBoardComment reply = comment(11L, root, "같이 지워진 대댓글", 2L);
    reply.softDelete();

    when(freeBoardRepository.findById(1L)).thenReturn(Optional.of(post));
    when(commentRepository.findAllByPostId(1L)).thenReturn(List.of(root, reply));

    assertThat(service.listComments(1L)).isEmpty();
  }

  @Test
  @DisplayName("남의 댓글은 ORGANIZER 미만이 수정할 수 없다")
  void othersCommentCannotBeUpdatedByNonOrganizer() {
    FreeBoardComment comment = comment(10L, null, "남의 댓글", 1L);
    when(commentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(comment));

    assertThatThrownBy(
            () ->
                service.updateComment(
                    10L, new FreeBoardCommentUpdateRequest("고쳐본다"), 99L, UserRole.CORE))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(GlobalErrorCode.FORBIDDEN_USER);
  }

  @Test
  @DisplayName("ORGANIZER 는 남의 댓글도 삭제할 수 있다")
  void organizerCanDeleteOthersComment() {
    FreeBoardComment comment = comment(10L, null, "남의 댓글", 1L);
    when(commentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(comment));

    service.deleteComment(10L, 99L, UserRole.ORGANIZER);

    assertThat(comment.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("다른 글의 댓글을 부모로 지정하면 404 다")
  void parentFromAnotherPostIsRejected() {
    FreeBoard otherPost = FreeBoard.create("다른 글", "본문", 1L, "글쓴이");
    ReflectionTestUtils.setField(otherPost, "id", 2L);

    FreeBoardComment foreignParent = FreeBoardComment.create(otherPost, null, "남의 글 댓글", 1L, "갑");
    ReflectionTestUtils.setField(foreignParent, "id", 20L);

    when(freeBoardRepository.findById(1L)).thenReturn(Optional.of(post));
    when(commentRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(foreignParent));

    assertThatThrownBy(
            () -> service.createComment(1L, new FreeBoardCommentCreateRequest("답글", 20L), 3L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(GlobalErrorCode.RESOURCE_NOT_FOUND);
  }

  private FreeBoardComment comment(Long id, FreeBoardComment parent, String content, Long authorId) {
    FreeBoardComment comment =
        FreeBoardComment.create(post, parent, content, authorId, "작성자" + authorId);
    ReflectionTestUtils.setField(comment, "id", id);
    return comment;
  }
}
