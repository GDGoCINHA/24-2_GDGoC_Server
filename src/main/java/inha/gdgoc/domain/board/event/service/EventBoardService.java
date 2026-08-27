package inha.gdgoc.domain.board.event.service;

import inha.gdgoc.domain.board.common.dto.AttachmentResponse;
import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.common.service.AttachmentPolicy;
import inha.gdgoc.domain.board.event.dto.request.EventBoardCreateRequest;
import inha.gdgoc.domain.board.event.dto.request.EventBoardUpdateRequest;
import inha.gdgoc.domain.board.event.dto.response.DeletedEventBoardSummaryResponse;
import inha.gdgoc.domain.board.event.dto.response.EventBoardDetailResponse;
import inha.gdgoc.domain.board.event.dto.response.EventBoardSummaryResponse;
import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.board.event.enums.EventBoardStatus;
import inha.gdgoc.domain.board.event.repository.EventBoardRepository;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
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

  private final EventBoardRepository eventBoardRepository;
  private final UserRepository userRepository;
  private final EventApplicationFormRepository eventApplicationFormRepository;
  private final S3Service s3Service;
  private final AttachmentPolicy attachmentPolicy;

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

    attachmentPolicy.apply(board, req.attachments());

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
      attachmentPolicy.apply(board, req.attachments());
    }

    // 신청 폼은 행사명·기간의 복사본을 들고 있다. 게시글이 사라져도 신청 데이터가 살아 있게 하려는
    // 것이지, 원본과 갈라지라는 뜻은 아니다. 평소에는 게시글이 원본이므로 여기서 맞춰준다.
    eventApplicationFormRepository
        .findByEventBoardId(id)
        .ifPresent(
            form ->
                form.syncEventInfo(
                    board.getTitle(), board.getEventStartDate(), board.getEventEndDate()));
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

    List<AttachmentResponse> attachments = attachmentPolicy.toResponses(board.getAttachments());

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
