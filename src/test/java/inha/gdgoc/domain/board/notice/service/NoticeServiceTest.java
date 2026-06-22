package inha.gdgoc.domain.board.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import inha.gdgoc.domain.board.notice.dto.request.NoticeCreateRequest;
import inha.gdgoc.domain.board.notice.dto.request.NoticeUpdateRequest;
import inha.gdgoc.domain.board.notice.dto.response.NoticeDetailResponse;
import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.board.notice.enums.ArticleStatusEnum;
import inha.gdgoc.domain.board.notice.enums.CategoryEnum;
import inha.gdgoc.domain.board.notice.repository.NoticeBoardRepository;
import inha.gdgoc.domain.resource.enums.S3KeyType;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeBoardRepository noticeBoardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private NoticeService noticeService;

    @Test
    void createNotice_success() throws IOException {
        User author = User.builder()
                .name("김철수")
                .major("컴퓨터공학과")
                .studentId("12201234")
                .phoneNumber("01012345678")
                .email("chulsu@inha.edu")
                .userRole(UserRole.CORE)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));

        NoticeCreateRequest req = new NoticeCreateRequest(
                CategoryEnum.OPERATION,
                "공지 제목",
                "공지 내용 <img src=\"image-0\">",
                false,
                ArticleStatusEnum.PUBLISHED,
                Collections.emptyList()
        );

        MockMultipartFile image = new MockMultipartFile("images", "test.png", "image/png", "image_data".getBytes());
        MockMultipartFile file = new MockMultipartFile("files", "doc.pdf", "application/pdf", "file_data".getBytes());

        when(s3Service.upload(eq(1L), eq(S3KeyType.notice), any(MultipartFile.class)))
                .thenReturn("s3-key-image", "s3-key-file");
        when(s3Service.getS3FileUrl("s3-key-image")).thenReturn("http://s3.url/image");
        when(s3Service.getS3FileUrl("s3-key-file")).thenReturn("http://s3.url/file");

        NoticeBoard savedBoard = NoticeBoard.create(
                req.title(),
                req.category(),
                req.content(),
                req.isPinned(),
                req.status(),
                1L,
                author.getName()
        );
        UUID articleId = UUID.randomUUID();
        setField(savedBoard, "articleId", articleId);
        when(noticeBoardRepository.save(any(NoticeBoard.class))).thenReturn(savedBoard);

        UUID resultId = noticeService.createNotice(req, new MultipartFile[]{file}, new MultipartFile[]{image}, 1L);

        assertThat(resultId).isEqualTo(articleId);
        verify(noticeBoardRepository).save(any(NoticeBoard.class));
    }

    @Test
    void createNotice_pinLimitExceeded() {
        User author = User.builder()
                .name("김철수")
                .major("컴퓨터공학과")
                .studentId("12201234")
                .phoneNumber("01012345678")
                .email("chulsu@inha.edu")
                .userRole(UserRole.CORE)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));

        // 이미 고정글이 3개 존재하는 상태 모킹
        List<NoticeBoard> pinnedBoards = List.of(
                NoticeBoard.create("고정1", CategoryEnum.OPERATION, "내용", true, ArticleStatusEnum.PUBLISHED, 1L, "김철수"),
                NoticeBoard.create("고정2", CategoryEnum.OPERATION, "내용", true, ArticleStatusEnum.PUBLISHED, 1L, "김철수"),
                NoticeBoard.create("고정3", CategoryEnum.OPERATION, "내용", true, ArticleStatusEnum.PUBLISHED, 1L, "김철수")
        );
        when(noticeBoardRepository.findPinnedNotices()).thenReturn(pinnedBoards);

        NoticeCreateRequest req = new NoticeCreateRequest(
                CategoryEnum.OPERATION,
                "추가 고정 시도",
                "내용",
                true, // pinned = true 시도
                ArticleStatusEnum.PUBLISHED,
                Collections.emptyList()
        );

        assertThatThrownBy(() -> noticeService.createNotice(req, null, null, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getNotice_success() {
        NoticeBoard board = NoticeBoard.create("공지", CategoryEnum.OPERATION, "내용", false, ArticleStatusEnum.PUBLISHED, 1L, "김철수");
        UUID articleId = UUID.randomUUID();
        setField(board, "articleId", articleId);
        setField(board, "articleNumber", 10L);

        when(noticeBoardRepository.findById(articleId)).thenReturn(Optional.of(board));
        when(noticeBoardRepository.findPrevNotice(10L, CategoryEnum.OPERATION)).thenReturn(Optional.empty());
        when(noticeBoardRepository.findNextNotice(10L, CategoryEnum.OPERATION)).thenReturn(Optional.empty());

        NoticeDetailResponse response = noticeService.getNotice(articleId, UserRole.MEMBER, 2L);

        assertThat(response.articleId()).isEqualTo(articleId);
        assertThat(response.viewCount()).isEqualTo(1); // 상세 조회 시 조회수 1 증가 확인
    }

    @Test
    void updateNotice_forbidden() {
        NoticeBoard board = NoticeBoard.create("공지", CategoryEnum.OPERATION, "내용", false, ArticleStatusEnum.PUBLISHED, 1L, "김철수");
        UUID articleId = UUID.randomUUID();
        setField(board, "articleId", articleId);

        when(noticeBoardRepository.findById(articleId)).thenReturn(Optional.of(board));

        NoticeUpdateRequest req = new NoticeUpdateRequest(
                CategoryEnum.OPERATION,
                "수정 요청",
                "수정 내용",
                false,
                ArticleStatusEnum.PUBLISHED,
                Collections.emptyList(),
                Collections.emptyList()
        );

        // 작성자가 아닌(2L) 다른 일반 CORE 등급(LEAD 미만)이 수정을 시도하면 예외 발생
        assertThatThrownBy(() -> noticeService.updateNotice(articleId, req, null, null, UserRole.CORE, 2L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteNotice_success() {
        NoticeBoard board = NoticeBoard.create("공지", CategoryEnum.OPERATION, "내용", false, ArticleStatusEnum.PUBLISHED, 1L, "김철수");
        UUID articleId = UUID.randomUUID();
        setField(board, "articleId", articleId);

        when(noticeBoardRepository.findById(articleId)).thenReturn(Optional.of(board));

        // 작성자(1L) 본인이 직접 지우는 요청
        noticeService.deleteNotice(articleId, UserRole.CORE, 1L);

        assertThat(board.getStatus()).isEqualTo(ArticleStatusEnum.DELETED);
        assertThat(board.getDeletedAt()).isNotNull();
    }

    @Test
    void restoreNotice_success() {
        NoticeBoard board = NoticeBoard.create("삭제된 공지", CategoryEnum.OPERATION, "내용", false, ArticleStatusEnum.DELETED, 1L, "김철수");
        UUID articleId = UUID.randomUUID();
        setField(board, "articleId", articleId);
        setField(board, "deletedAt", java.time.Instant.now());

        when(noticeBoardRepository.findDeletedById(articleId)).thenReturn(Optional.of(board));

        // LEAD 이상 권한자가 복원 요청
        noticeService.restoreNotice(articleId, UserRole.LEAD, 3L);

        assertThat(board.getStatus()).isEqualTo(ArticleStatusEnum.PUBLISHED);
        assertThat(board.getDeletedAt()).isNull();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
