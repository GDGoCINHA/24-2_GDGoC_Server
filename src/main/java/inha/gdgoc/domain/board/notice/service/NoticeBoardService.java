package inha.gdgoc.domain.board.notice.service;

import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.common.service.AttachmentPolicy;
import inha.gdgoc.domain.board.notice.dto.request.NoticeCreateRequest;
import inha.gdgoc.domain.board.notice.dto.request.NoticeUpdateRequest;
import inha.gdgoc.domain.board.notice.dto.response.NoticeDetailResponse;
import inha.gdgoc.domain.board.notice.dto.response.NoticeSummaryResponse;
import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.board.notice.repository.NoticeBoardRepository;
import inha.gdgoc.domain.board.notice.repository.PinnedNoticeRepository;
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
public class NoticeBoardService {

  private final NoticeBoardRepository noticeBoardRepository;
  private final PinnedNoticeRepository pinnedNoticeRepository;
  private final UserRepository userRepository;
  private final AttachmentPolicy attachmentPolicy;

  public Page<NoticeSummaryResponse> listNotices(
      int page, int size, SearchType searchType, String keyword, UserRole userRole) {
    return noticeBoardRepository
        .findVisibleNotices(userRole, searchType, keyword, PageRequest.of(page, size))
        .map(this::toSummaryResponse);
  }

  /**
   * 상세를 조회하며 조회수를 1 올린다.
   *
   * <p>클래스 기본값이 readOnly = true 이므로 여기에 @Transactional 을 따로 붙여야 더티체킹이 반영된다.
   */
  @Transactional
  public NoticeDetailResponse getNotice(Long id, UserRole userRole) {
    NoticeBoard notice =
        noticeBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    if (!notice.isPublished() && !UserRole.hasAtLeast(userRole, UserRole.CORE)) {
      throw new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }

    notice.increaseViewCount();
    return toDetailResponse(notice);
  }

  @Transactional
  public Long createNotice(NoticeCreateRequest req, Long authorId) {
    User author =
        userRepository
            .findById(authorId)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    NoticeBoard notice =
        NoticeBoard.create(
            req.title(), req.content(), req.category(), req.isPublished(), authorId, author.getName());

    attachmentPolicy.apply(notice, req.attachments());

    return noticeBoardRepository.save(notice).getId();
  }

  @Transactional
  public void updateNotice(Long id, NoticeUpdateRequest req, Long userId, UserRole userRole) {
    NoticeBoard notice =
        noticeBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    requireAuthorOrOrganizer(notice, userId, userRole);

    notice.update(req.title(), req.content(), req.category(), req.isPublished());

    if (req.attachments() != null) {
      notice.getAttachments().clear();
      attachmentPolicy.apply(notice, req.attachments());
    }
  }

  /**
   * soft delete 한다.
   *
   * <p>deleted_at 만 세팅되므로 ON DELETE CASCADE 가 걸리지 않는다. 삭제된 글이 고정 슬롯을 점유한 채 남으면 그
   * 자리에 새 공지를 넣을 수 없으므로, pinned_notice 행은 실제로 지운다.
   */
  @Transactional
  public void deleteNotice(Long id, Long userId, UserRole userRole) {
    NoticeBoard notice =
        noticeBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    requireAuthorOrOrganizer(notice, userId, userRole);

    pinnedNoticeRepository.deleteByNoticeBoardId(id);
    notice.softDelete();
  }

  /** 복구해도 고정은 돌아오지 않는다. 그 사이 다른 글이 슬롯을 차지했을 수 있다. */
  @Transactional
  public void restoreNotice(Long id, Long userId, UserRole userRole) {
    NoticeBoard notice =
        noticeBoardRepository
            .findDeletedById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    requireAuthorOrOrganizer(notice, userId, userRole);
    notice.restore();
  }

  public Page<NoticeSummaryResponse> listDeletedNotices(
      int page, int size, Long userId, UserRole userRole) {
    return noticeBoardRepository
        .findDeletedNotices(userId, userRole, PageRequest.of(page, size))
        .map(this::toSummaryResponse);
  }

  private void requireAuthorOrOrganizer(NoticeBoard notice, Long userId, UserRole userRole) {
    if (UserRole.hasAtLeast(userRole, UserRole.ORGANIZER)) return;
    if (!notice.getAuthorId().equals(userId)) {
      throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
    }
  }

  /** 고정 서비스도 같은 모양의 요약을 쓰므로 public 이다. */
  public NoticeSummaryResponse toSummaryResponse(NoticeBoard notice) {
    return new NoticeSummaryResponse(
        notice.getId(),
        notice.getCategory(),
        notice.getTitle(),
        notice.getAuthorName(),
        notice.getViewCount(),
        notice.isPublished(),
        notice.getCreatedAt());
  }

  private NoticeDetailResponse toDetailResponse(NoticeBoard notice) {
    return new NoticeDetailResponse(
        notice.getId(),
        notice.getCategory(),
        notice.getTitle(),
        notice.getContent(),
        notice.getAuthorName(),
        notice.getViewCount(),
        notice.isPublished(),
        attachmentPolicy.toResponses(notice.getAttachments()),
        notice.getCreatedAt(),
        notice.getUpdatedAt());
  }
}
