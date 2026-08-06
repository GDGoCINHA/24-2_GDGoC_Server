package inha.gdgoc.domain.board.notice.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 모달에서 빼기·넣기·정렬을 모두 끝낸 최종 상태. 배열 순서가 곧 display_order 다. */
public record PinnedUpdateRequest(@NotNull List<Long> noticeIds) {}
