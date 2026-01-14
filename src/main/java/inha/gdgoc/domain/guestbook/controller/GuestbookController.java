package inha.gdgoc.domain.guestbook.controller;

import inha.gdgoc.domain.guestbook.controller.message.GuestbookMessage;
import inha.gdgoc.domain.guestbook.dto.request.GuestbookCreateRequest;
import inha.gdgoc.domain.guestbook.dto.request.LuckyDrawRequest;
import inha.gdgoc.domain.guestbook.dto.response.GuestbookEntryResponse;
import inha.gdgoc.domain.guestbook.dto.response.LuckyDrawWinnerResponse;
import inha.gdgoc.domain.guestbook.service.GuestbookService;
import inha.gdgoc.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/guestbook")
@RequiredArgsConstructor
@PreAuthorize("@accessGuard.check(authentication,"
        + " T(inha.gdgoc.global.security.AccessGuard.AccessCondition).atLeast("
        + "T(inha.gdgoc.domain.user.enums.UserRole).LEAD))")
public class GuestbookController {

    private final GuestbookService service;

    /* ===== helpers ===== */
    private static ResponseEntity<ApiResponse<Map<String, Object>, Void>> okUpdated(String msg, long updated) {
        return ResponseEntity.ok(ApiResponse.ok(msg, Map.of("updated", updated)));
    }

    /* ===== 방명록: 등록(자동 응모) ===== */
    // 운영진 PC에서 손목밴드 번호 + 이름 입력 → 저장 + 자동 응모 상태로 들어감
    @PostMapping("/entries")
    public ResponseEntity<ApiResponse<GuestbookEntryResponse, Void>> createEntry(@Valid @RequestBody GuestbookCreateRequest req) {
        GuestbookEntryResponse saved = service.register(req.wristbandSerial(), req.name());
        return ResponseEntity.ok(ApiResponse.ok(GuestbookMessage.ENTRY_CREATED_SUCCESS, saved));
    }

    /* ===== 방명록: 목록 ===== */
    @GetMapping("/entries")
    public ResponseEntity<ApiResponse<List<GuestbookEntryResponse>, Void>> listEntries() {
        var result = service.listEntries();
        return ResponseEntity.ok(ApiResponse.ok(GuestbookMessage.ENTRY_LIST_RETRIEVED_SUCCESS, result));
    }

    /* ===== 방명록: 단건 조회(필요하면) ===== */
    @GetMapping("/entries/{entryId}")
    public ResponseEntity<ApiResponse<GuestbookEntryResponse, Void>> getEntry(@PathVariable Long entryId) {
        return ResponseEntity.ok(ApiResponse.ok(GuestbookMessage.ENTRY_RETRIEVED_SUCCESS, service.get(entryId)));
    }

    /* ===== 방명록: 삭제(운영 중 실수 입력 정정용) ===== */
    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<ApiResponse<Map<String, Object>, Void>> deleteEntry(@PathVariable Long entryId) {
        service.delete(entryId);
        return okUpdated(GuestbookMessage.ENTRY_DELETED_SUCCESS, 1L);
    }

    /* ===== 럭키드로우: 추첨 ===== */
    // 요청 예시: { "count": 3, "excludeWinnerIds": [1,2] } 같은 확장도 가능
    @PostMapping("/lucky-draw")
    public ResponseEntity<ApiResponse<List<LuckyDrawWinnerResponse>, Void>> drawWinners(@Valid @RequestBody LuckyDrawRequest req) {
        List<LuckyDrawWinnerResponse> winners = service.draw(req);
        return ResponseEntity.ok(ApiResponse.ok(GuestbookMessage.LUCKY_DRAW_SUCCESS, winners));
    }

    /* ===== 럭키드로우: 당첨자 목록 ===== */
    @GetMapping("/lucky-draw/winners")
    public ResponseEntity<ApiResponse<List<LuckyDrawWinnerResponse>, Void>> listWinners() {
        return ResponseEntity.ok(ApiResponse.ok(GuestbookMessage.WINNER_LIST_RETRIEVED_SUCCESS, service.listWinners()));
    }

    /* ===== 럭키드로우: 리셋(테스트/리허설용) ===== */
    @PostMapping("/lucky-draw/reset")
    public ResponseEntity<ApiResponse<Map<String, Object>, Void>> resetWinners() {
        long updated = service.resetWinners();
        return okUpdated(GuestbookMessage.WINNER_RESET_SUCCESS, updated);
    }
}
