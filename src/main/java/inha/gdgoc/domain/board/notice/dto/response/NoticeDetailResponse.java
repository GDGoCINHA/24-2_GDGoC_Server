package inha.gdgoc.domain.board.notice.dto.response;

import inha.gdgoc.domain.board.common.dto.AttachmentResponse;
import inha.gdgoc.domain.board.notice.enums.NoticeCategory;
import java.time.Instant;
import java.util.List;

/**
 * 상세 응답.
 *
 * <p>authorId 를 함께 준다. 수정·삭제는 '작성자 본인 또는 ORGANIZER 이상'인데({@code
 * NoticeBoardService.requireAuthorOrOrganizer}), 이 값이 없으면 프론트가 본인 여부를 판정할 수 없어 권한 없는
 * CORE 에게도 버튼을 보여주고 눌러야 403 을 만나게 된다.
 *
 * <p>행사 게시판에는 같은 필드를 두지 않았다. 그쪽 규칙은 '주최 팀 일치 또는 ORGANIZER 이상'이라 판정 기준이
 * organizingTeam 이고 그 값은 이미 응답에 있다. 행사 상세는 공개 API 라 사용자 PK 를 비로그인에게 흘릴 이유도 없다.
 */
public record NoticeDetailResponse(
    Long id,
    NoticeCategory category,
    String title,
    String content,
    Long authorId,
    String authorName,
    int viewCount,
    boolean isPublished,
    List<AttachmentResponse> attachments,
    Instant createdAt,
    Instant updatedAt) {}
