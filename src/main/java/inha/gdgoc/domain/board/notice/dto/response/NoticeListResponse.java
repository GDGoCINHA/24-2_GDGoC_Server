package inha.gdgoc.domain.board.notice.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 공지 목록 화면.
 *
 * <p>고정 3건은 size=15 와 별개다. content 에 섞으면 첫 페이지만 18건이 되고 totalElements 의미가 흐려진다.
 * 고정된 글이 posts 에도 나올 수 있으며 의도된 동작이다.
 */
public record NoticeListResponse(List<NoticeSummaryResponse> pinned, Page<NoticeSummaryResponse> posts) {}
