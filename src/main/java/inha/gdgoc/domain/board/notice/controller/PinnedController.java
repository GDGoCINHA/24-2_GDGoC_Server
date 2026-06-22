package inha.gdgoc.domain.board.notice.controller;

import static inha.gdgoc.domain.board.notice.controller.message.NoticeMessage.PINNED_NOTICE_UPDATED;

import inha.gdgoc.domain.board.notice.dto.request.PinnedUpdateRequest;
import inha.gdgoc.domain.board.notice.service.PinnedService;
import inha.gdgoc.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/board/pinned")
@RequiredArgsConstructor
public class PinnedController {

    private static final String LEAD_OR_HIGHER_RULE =
            "@accessGuard.check(authentication,"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).atLeast("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).LEAD))";

    private final PinnedService pinnedService;

    @PreAuthorize(LEAD_OR_HIGHER_RULE)
    @PutMapping("/{boardType}")
    public ResponseEntity<ApiResponse<Void, Void>> updatePinnedNotices(
            @PathVariable String boardType,
            @Valid @RequestBody PinnedUpdateRequest req) {
        pinnedService.updatePinnedNotices(boardType, req);
        return ResponseEntity.ok(ApiResponse.ok(PINNED_NOTICE_UPDATED));
    }
}
