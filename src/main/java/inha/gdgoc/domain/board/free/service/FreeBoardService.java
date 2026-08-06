package inha.gdgoc.domain.board.free.service;

import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.common.service.AttachmentPolicy;
import inha.gdgoc.domain.board.free.dto.request.FreeBoardCreateRequest;
import inha.gdgoc.domain.board.free.dto.request.FreeBoardUpdateRequest;
import inha.gdgoc.domain.board.free.dto.response.FreeBoardDetailResponse;
import inha.gdgoc.domain.board.free.dto.response.FreeBoardSummaryResponse;
import inha.gdgoc.domain.board.free.entity.FreeBoard;
import inha.gdgoc.domain.board.free.repository.FreeBoardRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FreeBoardService {

  private final FreeBoardRepository freeBoardRepository;
  private final UserRepository userRepository;
  private final AttachmentPolicy attachmentPolicy;

  public Page<FreeBoardSummaryResponse> listPosts(
      int page, int size, SearchType searchType, String keyword) {
    return freeBoardRepository
        .findVisiblePosts(searchType, keyword, PageRequest.of(page, size))
        .map(this::toSummaryResponse);
  }

  /**
   * 상세를 조회하며 조회수를 1 올린다.
   *
   * <p>엔티티를 더티체킹으로 올리면 @LastModifiedDate 가 함께 갱신되어 "마지막으로 열어본 시각"이 수정 시각을 덮어써
   * 버린다. 그래서 조회수는 벌크 UPDATE 로 따로 올린다. DTO 를 먼저 만들고 그 다음에 벌크 UPDATE 를 호출하는 순서를
   * 지켜야 한다 — clearAutomatically 로 영속성 컨텍스트를 비우면 post 가 detach 되어 첨부 지연 로딩이
   * LazyInitializationException 으로 죽는다. (NoticeBoardService.getNotice 와 같은 이유다.)
   */
  @Transactional
  public FreeBoardDetailResponse getPost(Long id) {
    FreeBoard post =
        freeBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    // 벌크 UPDATE 는 로딩된 엔티티에 반영되지 않으므로, 이번 조회를 포함한 값을 보여주려면 +1 을 직접 넘긴다.
    FreeBoardDetailResponse response = toDetailResponse(post, post.getViewCount() + 1);
    freeBoardRepository.increaseViewCount(id);
    return response;
  }

  @Transactional
  public Long createPost(FreeBoardCreateRequest req, Long authorId) {
    User author =
        userRepository
            .findById(authorId)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    FreeBoard post = FreeBoard.create(req.title(), req.content(), authorId, author.getName());

    attachmentPolicy.apply(post, req.attachments());

    return freeBoardRepository.save(post).getId();
  }

  @Transactional
  public void updatePost(Long id, FreeBoardUpdateRequest req, Long userId, UserRole userRole) {
    FreeBoard post =
        freeBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    requireAuthorOrOrganizer(post, userId, userRole);

    post.update(req.title(), req.content());

    if (req.attachments() != null) {
      post.getAttachments().clear();
      attachmentPolicy.apply(post, req.attachments());
    }
  }

  /** soft delete 한다. 복구 화면은 아직 없으므로 되살리려면 DB 에서 deleted_at 을 비워야 한다. */
  @Transactional
  public void deletePost(Long id, Long userId, UserRole userRole) {
    FreeBoard post =
        freeBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    requireAuthorOrOrganizer(post, userId, userRole);

    post.softDelete();
  }

  /**
   * 자유게시판의 수정·삭제 경계다. 작성자 본인이거나 ORGANIZER 이상이어야 한다.
   *
   * <p>공지와 규칙이 같지만 뜻이 다르다. 공지는 CORE 이상만 글을 쓸 수 있어 '작성자'가 곧 운영진이고, 자유게시판은
   * MEMBER 도 쓸 수 있어 이 검사가 회원끼리의 경계가 된다.
   */
  private void requireAuthorOrOrganizer(FreeBoard post, Long userId, UserRole userRole) {
    if (UserRole.hasAtLeast(userRole, UserRole.ORGANIZER)) return;
    if (!post.getAuthorId().equals(userId)) {
      throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
    }
  }

  private FreeBoardSummaryResponse toSummaryResponse(FreeBoard post) {
    return new FreeBoardSummaryResponse(
        post.getId(),
        post.getTitle(),
        post.getAuthorName(),
        post.getViewCount(),
        post.getCreatedAt());
  }

  private FreeBoardDetailResponse toDetailResponse(FreeBoard post, int viewCount) {
    return new FreeBoardDetailResponse(
        post.getId(),
        post.getTitle(),
        post.getContent(),
        post.getAuthorId(),
        post.getAuthorName(),
        viewCount,
        attachmentPolicy.toResponses(post.getAttachments()),
        post.getCreatedAt(),
        post.getUpdatedAt());
  }
}
