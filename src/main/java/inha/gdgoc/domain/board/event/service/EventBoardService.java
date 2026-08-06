package inha.gdgoc.domain.board.event.service;

import inha.gdgoc.domain.board.event.dto.request.EventBoardCreateRequest;
import inha.gdgoc.domain.board.event.dto.request.EventBoardCreateRequest.AttachmentEntry;
import inha.gdgoc.domain.board.event.dto.request.EventBoardUpdateRequest;
import inha.gdgoc.domain.board.event.dto.response.DeletedEventBoardSummaryResponse;
import inha.gdgoc.domain.board.event.dto.response.EventBoardDetailResponse;
import inha.gdgoc.domain.board.event.dto.response.EventBoardDetailResponse.AttachmentResponse;
import inha.gdgoc.domain.board.event.dto.response.EventBoardSummaryResponse;
import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.board.event.enums.EventBoardStatus;
import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.event.repository.EventBoardRepository;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventBoardService {

  private static final int MAX_ATTACHMENTS = 10;

  private final EventBoardRepository eventBoardRepository;
  private final UserRepository userRepository;
  private final S3Service s3Service;

  public Page<EventBoardSummaryResponse> listEventBoards(
      int page, int size, SearchType searchType, String keyword, TeamType userTeam, UserRole userRole) {
    return eventBoardRepository
        .findVisibleBoards(userTeam, userRole, searchType, keyword, PageRequest.of(page, size))
        .map(this::toSummaryResponse);
  }

  public Page<DeletedEventBoardSummaryResponse> listDeletedBoards(
      int page, int size, UserRole userRole, TeamType userTeam) {
    return eventBoardRepository
        .findDeletedBoards(userTeam, userRole, PageRequest.of(page, size))
        .map(b -> new DeletedEventBoardSummaryResponse(
            b.getId(), b.getTitle(), b.getEventStartDate(), b.getEventEndDate(),
            b.getOrganizingTeam(), b.getDeletedAt()));
  }

  public EventBoardDetailResponse getEventBoard(Long id, TeamType userTeam, UserRole userRole) {
    return toDetailResponse(findVisibleBoard(id, userTeam, userRole));
  }

  @Transactional
  public Long createEventBoard(EventBoardCreateRequest req, Long authorId) {
    User author = userRepository.findById(authorId)
        .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    EventBoard board =
        EventBoard.create(
            req.title(),
            req.eventStartDate(),
            req.eventEndDate(),
            req.organizingTeam(),
            req.thumbnailKey(),
            req.content(),
            req.isPublished(),
            authorId,
            author.getName());

    applyAttachments(board, req.attachments());

    return eventBoardRepository.save(board).getId();
  }

  @Transactional
  public void updateEventBoard(Long id, EventBoardUpdateRequest req, UserRole userRole, TeamType userTeam) {
    EventBoard board =
        eventBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    requireTeamAccess(board, userRole, userTeam);

    board.update(
        req.title(),
        req.eventStartDate(),
        req.eventEndDate(),
        req.organizingTeam(),
        req.thumbnailKey(),
        req.content(),
        req.isPublished());

    if (board.getEventEndDate().isBefore(board.getEventStartDate())) {
      throw new BusinessException(
          GlobalErrorCode.BAD_REQUEST, "행사 종료일은 시작일보다 앞설 수 없습니다.");
    }

    if (req.attachments() != null) {
      board.getAttachments().clear();
      applyAttachments(board, req.attachments());
    }
  }

  @Transactional
  public void deleteEventBoard(Long id, UserRole userRole, TeamType userTeam) {
    EventBoard board =
        eventBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    requireTeamAccess(board, userRole, userTeam);
    board.softDelete();
  }

  @Transactional
  public void restoreEventBoard(Long id, UserRole userRole, TeamType userTeam) {
    EventBoard board =
        eventBoardRepository
            .findDeletedById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    requireTeamAccess(board, userRole, userTeam);
    board.restore();
  }

  private void applyAttachments(EventBoard board, List<AttachmentEntry> entries) {
    if (entries == null) return;

    if (entries.size() > MAX_ATTACHMENTS) {
      throw new BusinessException(
          GlobalErrorCode.BAD_REQUEST, "첨부는 최대 " + MAX_ATTACHMENTS + "개까지 등록할 수 있습니다.");
    }

    for (int i = 0; i < entries.size(); i++) {
      AttachmentEntry entry = entries.get(i);

      if (entry.url() != null && !entry.url().isBlank()) {
        board.addLinkAttachment(entry.url(), i);
        continue;
      }

      Long size = s3Service.getObjectSize(entry.fileKey());
      if (size == null) {
        throw new BusinessException(
            GlobalErrorCode.BAD_REQUEST, "업로드되지 않은 파일입니다: " + entry.fileName());
      }
      board.addFileAttachment(entry.fileKey(), entry.fileName(), size, i);
    }
  }

  private EventBoard findVisibleBoard(Long id, TeamType userTeam, UserRole userRole) {
    EventBoard board =
        eventBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    if (!board.isPublished()) {
      if (!UserRole.hasAtLeast(userRole, UserRole.ORGANIZER)) {
        if (userTeam == null || userTeam != board.getOrganizingTeam()) {
          throw new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND);
        }
      }
    }
    return board;
  }

  private void requireTeamAccess(EventBoard board, UserRole userRole, TeamType userTeam) {
    if (UserRole.hasAtLeast(userRole, UserRole.ORGANIZER)) return;
    if (userTeam == null || userTeam != board.getOrganizingTeam()) {
      throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
    }
  }

  private EventBoardSummaryResponse toSummaryResponse(EventBoard board) {
    String thumbnailUrl =
        board.getThumbnailKey() != null ? s3Service.getS3FileUrl(board.getThumbnailKey()) : null;
    return new EventBoardSummaryResponse(
        board.getId(),
        board.getTitle(),
        thumbnailUrl,
        board.getEventStartDate(),
        board.getEventEndDate(),
        board.getOrganizingTeam(),
        board.getAuthorName(),
        EventBoardStatus.of(board.getEventStartDate(), board.getEventEndDate()));
  }

  private EventBoardDetailResponse toDetailResponse(EventBoard board) {
    String thumbnailUrl =
        board.getThumbnailKey() != null ? s3Service.getS3FileUrl(board.getThumbnailKey()) : null;

    List<AttachmentResponse> attachments =
        board.getAttachments().stream()
            .map(a -> new AttachmentResponse(a.getId(), s3Service.getS3FileUrl(a.getFileKey()), a.getFileName()))
            .toList();

    return new EventBoardDetailResponse(
        board.getId(),
        board.getTitle(),
        board.getEventStartDate(),
        board.getEventEndDate(),
        board.getOrganizingTeam(),
        board.getAuthorName(),
        thumbnailUrl,
        board.getContent(),
        board.isPublished(),
        EventBoardStatus.of(board.getEventStartDate(), board.getEventEndDate()),
        attachments,
        board.getCreatedAt(),
        board.getUpdatedAt());
  }
}
