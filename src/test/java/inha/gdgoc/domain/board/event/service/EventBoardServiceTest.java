package inha.gdgoc.domain.board.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.board.event.dto.request.EventBoardUpdateRequest;
import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.board.event.repository.EventBoardRepository;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 행사 게시판의 권한·가시성 규칙을 검증한다.
 *
 * <p>미공개 글은 존재 자체를 숨겨야 하므로 404, 수정·삭제 권한 부족은 403으로 구분된다.
 */
@ExtendWith(MockitoExtension.class)
class EventBoardServiceTest {

    private static final Long BOARD_ID = 1L;

    @Mock
    private EventBoardRepository eventBoardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private EventBoardService eventBoardService;

    // --- 미공개 글 가시성 ---

    @Test
    void getEventBoard_hidesUnpublishedBoardFromOtherTeam() {
        givenBoard(unpublishedBoard(TeamType.PR_DESIGN));

        assertThatThrownBy(() -> eventBoardService.getEventBoard(BOARD_ID, TeamType.HR, UserRole.CORE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void getEventBoard_hidesUnpublishedBoardFromAnonymous() {
        givenBoard(unpublishedBoard(TeamType.PR_DESIGN));

        assertThatThrownBy(() -> eventBoardService.getEventBoard(BOARD_ID, null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getEventBoard_allowsOwningTeamToReadUnpublishedBoard() {
        givenBoard(unpublishedBoard(TeamType.PR_DESIGN));

        assertThat(eventBoardService.getEventBoard(BOARD_ID, TeamType.PR_DESIGN, UserRole.CORE).isPublished())
                .isFalse();
    }

    @Test
    void getEventBoard_allowsOrganizerToReadUnpublishedBoardOfAnyTeam() {
        givenBoard(unpublishedBoard(TeamType.PR_DESIGN));

        assertThatCode(() -> eventBoardService.getEventBoard(BOARD_ID, TeamType.HR, UserRole.ORGANIZER))
                .doesNotThrowAnyException();
    }

    @Test
    void getEventBoard_allowsAnonymousToReadPublishedBoard() {
        givenBoard(publishedBoard(TeamType.PR_DESIGN));

        assertThatCode(() -> eventBoardService.getEventBoard(BOARD_ID, null, null))
                .doesNotThrowAnyException();
    }

    // --- 수정·삭제 권한 ---

    @Test
    void updateEventBoard_rejectsOtherTeam() {
        givenBoard(publishedBoard(TeamType.PR_DESIGN));

        assertThatThrownBy(() -> eventBoardService.updateEventBoard(
                BOARD_ID, emptyUpdate(), UserRole.CORE, TeamType.HR))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.FORBIDDEN_USER);
    }

    @Test
    void updateEventBoard_allowsOwningTeam() {
        givenBoard(publishedBoard(TeamType.PR_DESIGN));

        assertThatCode(() -> eventBoardService.updateEventBoard(
                BOARD_ID, emptyUpdate(), UserRole.CORE, TeamType.PR_DESIGN))
                .doesNotThrowAnyException();
    }

    @Test
    void deleteEventBoard_rejectsOtherTeam() {
        givenBoard(publishedBoard(TeamType.PR_DESIGN));

        assertThatThrownBy(() -> eventBoardService.deleteEventBoard(BOARD_ID, UserRole.CORE, TeamType.HR))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.FORBIDDEN_USER);
    }

    @Test
    void deleteEventBoard_marksBoardAsDeletedForOwningTeam() {
        EventBoard board = publishedBoard(TeamType.PR_DESIGN);
        givenBoard(board);

        eventBoardService.deleteEventBoard(BOARD_ID, UserRole.CORE, TeamType.PR_DESIGN);

        assertThat(board.getDeletedAt()).isNotNull();
    }

    // --- fixtures ---

    private void givenBoard(EventBoard board) {
        when(eventBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
    }

    private EventBoard publishedBoard(TeamType organizingTeam) {
        return board(organizingTeam, true);
    }

    private EventBoard unpublishedBoard(TeamType organizingTeam) {
        return board(organizingTeam, false);
    }

    private EventBoard board(TeamType organizingTeam, boolean isPublished) {
        return EventBoard.create(
                "인하 GDGoC 행사",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                organizingTeam,
                null,
                "본문",
                isPublished,
                1L,
                "차준호");
    }

    private EventBoardUpdateRequest emptyUpdate() {
        return new EventBoardUpdateRequest(null, null, null, null, null, null, null, null);
    }
}
