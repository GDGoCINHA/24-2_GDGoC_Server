package inha.gdgoc.domain.board.free.dto.response;

import inha.gdgoc.domain.board.common.dto.AttachmentResponse;
import java.time.Instant;
import java.util.List;

/**
 * 상세 응답.
 *
 * <p>행사·공지와 달리 authorId 를 함께 준다. 수정·삭제는 '작성자 본인 또는 ORGANIZER 이상'인데, 이 값이 없으면
 * 프론트가 본인 여부를 판정할 수 없어 권한 없는 사용자에게도 버튼을 보여주고 눌러야 403 을 만나게 된다.
 */
public record FreeBoardDetailResponse(
    Long id,
    String title,
    String content,
    Long authorId,
    String authorName,
    int viewCount,
    List<AttachmentResponse> attachments,
    Instant createdAt,
    Instant updatedAt) {}
