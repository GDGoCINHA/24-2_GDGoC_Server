package inha.gdgoc.domain.board.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.board.common.dto.AttachmentEntry;
import inha.gdgoc.domain.board.common.enums.AttachmentKind;
import inha.gdgoc.domain.board.common.service.AttachmentPolicy;
import inha.gdgoc.domain.board.event.dto.request.EventBoardCreateRequest;
import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.board.event.repository.EventBoardRepository;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 첨부는 S3에 실제로 올라간 것만 저장되며, 개수 상한이 있다. */
@ExtendWith(MockitoExtension.class)
class EventBoardAttachmentServiceTest {

  @Mock private EventBoardRepository eventBoardRepository;
  @Mock private UserRepository userRepository;
  @Mock private S3Service s3Service;

  @Mock private EventApplicationFormRepository eventApplicationFormRepository;

  private EventBoardService eventBoardService;

  @BeforeEach
  void setUp() {
    eventBoardService =
        new EventBoardService(
            eventBoardRepository,
                userRepository,
                eventApplicationFormRepository,
                s3Service,
                new AttachmentPolicy(s3Service));
  }

  @Test
  @DisplayName("S3에 없는 키를 첨부하면 400으로 거절한다")
  void rejectsAttachmentWhoseObjectIsMissing() {
    givenAuthor();
    when(s3Service.getObjectSize("user/1/event/missing.pdf")).thenReturn(null);

    EventBoardCreateRequest request =
        requestWithAttachments(List.of(fileEntry("user/1/event/missing.pdf", "missing.pdf")));

    assertThatThrownBy(() -> eventBoardService.createEventBoard(request, 1L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(GlobalErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("첨부가 11개면 400으로 거절한다")
  void rejectsMoreThanTenAttachments() {
    givenAuthor();
    lenient().when(s3Service.getObjectSize(anyString())).thenReturn(1L);

    List<AttachmentEntry> entries =
        IntStream.range(0, 11)
            .mapToObj(i -> fileEntry("user/1/event/f" + i + ".pdf", "f" + i + ".pdf"))
            .toList();

    assertThatThrownBy(
            () -> eventBoardService.createEventBoard(requestWithAttachments(entries), 1L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(GlobalErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("존재하는 파일은 S3가 알려준 크기로 저장된다")
  void storesSizeFromS3() {
    givenAuthor();
    when(s3Service.getObjectSize("user/1/event/a.pdf")).thenReturn(2048L);
    when(eventBoardRepository.save(org.mockito.ArgumentMatchers.any(EventBoard.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    EventBoardCreateRequest request =
        requestWithAttachments(List.of(fileEntry("user/1/event/a.pdf", "a.pdf")));

    eventBoardService.createEventBoard(request, 1L);

    org.mockito.ArgumentCaptor<EventBoard> captor =
        org.mockito.ArgumentCaptor.forClass(EventBoard.class);
    org.mockito.Mockito.verify(eventBoardRepository).save(captor.capture());
    assertThat(captor.getValue().getAttachments())
        .singleElement()
        .satisfies(
            a -> {
              assertThat(a.getFileSize()).isEqualTo(2048L);
              assertThat(a.getSortOrder()).isZero();
            });
  }

  @Test
  @DisplayName("상세 응답의 파일 첨부는 fileKey 와 fileUrl 을 모두 담는다")
  void detailResponseExposesFileKey() {
    EventBoard board = publishedBoard();
    board.addFileAttachment("user/1/event/a.pdf", "a.pdf", 2048L, 0);
    board.addLinkAttachment("https://example.com", 1);

    when(eventBoardRepository.findById(1L)).thenReturn(Optional.of(board));
    when(s3Service.getS3FileUrl("user/1/event/a.pdf"))
        .thenReturn("https://bucket.s3.ap-northeast-2.amazonaws.com/user/1/event/a.pdf");

    var response = eventBoardService.getEventBoard(1L, TeamType.PR_DESIGN, UserRole.CORE);

    assertThat(response.attachments()).hasSize(2);
    assertThat(response.attachments().get(0).fileKey()).isEqualTo("user/1/event/a.pdf");
    assertThat(response.attachments().get(0).fileUrl()).contains("user/1/event/a.pdf");
    assertThat(response.attachments().get(0).fileSize()).isEqualTo(2048L);
    assertThat(response.attachments().get(1).kind()).isEqualTo(AttachmentKind.LINK);
    assertThat(response.attachments().get(1).url()).isEqualTo("https://example.com");
    assertThat(response.attachments().get(1).fileUrl()).isNull();
  }

  @Test
  @DisplayName("상세 응답의 첨부는 삽입 순서가 아니라 sortOrder 순으로 나온다")
  void detailResponseSortsAttachmentsBySortOrder() {
    EventBoard board = publishedBoard();
    board.addLinkAttachment("https://second.example.com", 1);
    board.addLinkAttachment("https://first.example.com", 0);

    when(eventBoardRepository.findById(1L)).thenReturn(Optional.of(board));

    var response = eventBoardService.getEventBoard(1L, TeamType.PR_DESIGN, UserRole.CORE);

    assertThat(response.attachments())
        .extracting(a -> a.url())
        .containsExactly("https://first.example.com", "https://second.example.com");
  }

  private EventBoard publishedBoard() {
    return EventBoard.create(
        "제목",
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 2),
        TeamType.PR_DESIGN,
        null,
        "본문",
        true,
        1L,
        "홍길동");
  }

  private void givenAuthor() {
    User author = org.mockito.Mockito.mock(User.class);
    lenient().when(author.getName()).thenReturn("홍길동");
    lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(author));
  }

  private AttachmentEntry fileEntry(String key, String name) {
    return new AttachmentEntry(key, name, null);
  }

  private EventBoardCreateRequest requestWithAttachments(List<AttachmentEntry> attachments) {
    return new EventBoardCreateRequest(
        "제목",
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 2),
        TeamType.PR_DESIGN,
        null,
        "본문",
        true,
        attachments);
  }
}
