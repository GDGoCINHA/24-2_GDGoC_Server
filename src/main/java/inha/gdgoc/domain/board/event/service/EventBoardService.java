package inha.gdgoc.domain.board.event.service;

import inha.gdgoc.domain.board.event.dto.request.EventBoardCreateRequest;
import inha.gdgoc.domain.board.event.dto.request.EventBoardUpdateRequest;
import inha.gdgoc.domain.board.event.dto.response.EventBoardDetailResponse;
import inha.gdgoc.domain.board.event.dto.response.EventBoardDetailResponse.AttachmentResponse;
import inha.gdgoc.domain.board.event.dto.response.EventBoardSummaryResponse;
import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.board.event.enums.EventBoardStatus;
import inha.gdgoc.domain.board.event.enums.SearchType;
import inha.gdgoc.domain.board.event.repository.EventBoardRepository;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.enums.TeamType;
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

  private final EventBoardRepository eventBoardRepository;
  private final S3Service s3Service;

  public Page<EventBoardSummaryResponse> listEventBoards(
      int page, int size, SearchType searchType, String keyword, TeamType userTeam) {
    return eventBoardRepository
        .findVisibleBoards(userTeam, searchType, keyword, PageRequest.of(page, size))
        .map(this::toSummaryResponse);
  }

  public EventBoardDetailResponse getEventBoard(Long id, TeamType userTeam) {
    return toDetailResponse(findVisibleBoard(id, userTeam));
  }

  @Transactional
  public Long createEventBoard(EventBoardCreateRequest req, Long authorId) {
    EventBoard board =
        EventBoard.create(
            req.title(),
            req.eventStartDate(),
            req.eventEndDate(),
            req.organizingTeam(),
            req.thumbnailKey(),
            req.content(),
            req.isPublished(),
            authorId);

    if (req.attachments() != null) {
      req.attachments().forEach(a -> board.addAttachment(a.fileKey(), a.fileName()));
    }

    return eventBoardRepository.save(board).getId();
  }

  @Transactional
  public void updateEventBoard(Long id, EventBoardUpdateRequest req, TeamType userTeam) {
    EventBoard board =
        eventBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    if (userTeam == null || userTeam != board.getOrganizingTeam()) {
      throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
    }

    board.update(
        req.title(),
        req.eventStartDate(),
        req.eventEndDate(),
        req.organizingTeam(),
        req.thumbnailKey(),
        req.content(),
        req.isPublished());

    if (req.attachments() != null) {
      board.getAttachments().clear();
      req.attachments().forEach(a -> board.addAttachment(a.fileKey(), a.fileName()));
    }
  }

  @Transactional
  public void deleteEventBoard(Long id, TeamType userTeam) {
    EventBoard board =
        eventBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    if (userTeam == null || userTeam != board.getOrganizingTeam()) {
      throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
    }

    eventBoardRepository.delete(board);
  }

  private EventBoard findVisibleBoard(Long id, TeamType userTeam) {
    EventBoard board =
        eventBoardRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    if (!board.isPublished() && (userTeam == null || userTeam != board.getOrganizingTeam())) {
      throw new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }
    return board;
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
        thumbnailUrl,
        board.getContent(),
        board.isPublished(),
        EventBoardStatus.of(board.getEventStartDate(), board.getEventEndDate()),
        attachments,
        board.getCreatedAt(),
        board.getUpdatedAt());
  }
}
