package inha.gdgoc.domain.core.attendance.controller;

import inha.gdgoc.domain.core.attendance.controller.message.CoreAttendanceMessage;
import inha.gdgoc.domain.core.attendance.dto.request.CreateDateRequest;
import inha.gdgoc.domain.core.attendance.dto.request.SetAttendanceRequest;
import inha.gdgoc.domain.core.attendance.dto.response.DateListResponse;
import inha.gdgoc.domain.core.attendance.dto.response.DaySummaryResponse;
import inha.gdgoc.domain.core.attendance.dto.response.TeamResponse;
import inha.gdgoc.domain.core.attendance.service.CoreAttendanceService;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/core-attendance/meetings")
@RequiredArgsConstructor
@PreAuthorize(CoreAttendanceController.LEAD_OR_HIGHER_RULE)
public class CoreAttendanceController {

    public static final String LEAD_OR_HIGHER_RULE =
            "@accessGuard.check(authentication,"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).atLeast("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).LEAD))";
    public static final String ORGANIZER_OR_HIGHER_RULE =
            "@accessGuard.check(authentication,"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).atLeast("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).ORGANIZER))";

    private final CoreAttendanceService service;

    private static ResponseEntity<ApiResponse<Map<String, Object>, Void>> okUpdated(long updated, List<Long> ignored) {
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.ATTENDANCE_ALL_SET_SUCCESS, Map.of("updated", updated, "ignoredUserIds", ignored)));
    }

    /* ===== Meetings(날짜) 목록 ===== */
    @GetMapping
    public ResponseEntity<ApiResponse<DateListResponse, Void>> listDates() {
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.DATE_LIST_RETRIEVED_SUCCESS, new DateListResponse(service.getDates())));
    }

    @PreAuthorize(ORGANIZER_OR_HIGHER_RULE)
    @PostMapping
    public ResponseEntity<ApiResponse<DateListResponse, Void>> createDate(@Valid @RequestBody CreateDateRequest request) {
        service.addDate(request.getDate());
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.DATE_CREATED_SUCCESS, new DateListResponse(service.getDates())));
    }

    @PreAuthorize(ORGANIZER_OR_HIGHER_RULE)
    @DeleteMapping("/{date}")
    public ResponseEntity<ApiResponse<DateListResponse, Void>> deleteDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        service.deleteDate(date.toString());
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.DATE_DELETED_SUCCESS, new DateListResponse(service.getDates())));
    }

    /* ===== 팀 목록 (리드=본인 팀만 / 관리자=전체) ===== */
    @GetMapping("/teams")
    public ResponseEntity<ApiResponse<List<TeamResponse>, PageMeta>> getTeams(@AuthenticationPrincipal CustomUserDetails me) {
        List<TeamResponse> list = service.isLeadScoped(me.getRole(), me.getTeam())
                ? service.getTeamsForLead(service.resolveEffectiveTeam(me.getRole(), me.getTeam(), null))
                : service.getTeamsForOrganizerOrAdmin();

        var page = new PageImpl<>(list, PageRequest.of(0, Math.max(1, list.size()), Sort.by(Sort.Direction.DESC, "createdAt")), list.size());
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.TEAM_LIST_RETRIEVED_SUCCESS, list, PageMeta.of(page)));
    }

    /* ===== 특정 날짜의 팀원+현재 출석 상태 조회 (리드=본인 팀만) ===== */
    // 프론트가 체크박스 채우기 전에 필요한 목록/상태
    @GetMapping("/{date}/members")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>, Void>> membersOfMeeting(@AuthenticationPrincipal CustomUserDetails me, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) TeamType team // 관리자만 사용, 리드는 무시
    ) {
        TeamType effectiveTeam = service.resolveEffectiveTeam(me.getRole(), me.getTeam(), team);
        var list = service.getMembersWithPresence(date.toString(), effectiveTeam);
        // list 원소 예시: { "userId": "123", "name": "홍길동", "present": true, "lastModifiedAt": "..." }
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.TEAM_LIST_RETRIEVED_SUCCESS, list));
    }

    /* ===== 특정 날짜 출석 일괄 저장 (멱등 스냅샷) ===== */
    // Body: { "userIds": ["1","2",...], "present": true }  → presentUserIds만 보내는 구조로도 쉽게 변환 가능
    @PutMapping("/{date}/attendance")
    public ResponseEntity<ApiResponse<Map<String, Object>, Void>> saveAttendanceSnapshot(@AuthenticationPrincipal CustomUserDetails me, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestBody @Valid SetAttendanceRequest req) {
        var userIds = req.safeUserIds();
        CoreAttendanceService.AttendanceUpdateResult result = service.saveAttendanceSnapshot(
                date.toString(),
                userIds,
                req.presentValue(),
                me.getRole(),
                me.getTeam()
        );
        return okUpdated(result.updatedCount(), result.ignoredUserIds());
    }

    /* ===== 날짜 요약(JSON) ===== */
    @GetMapping("/{date}/summary")
    public ResponseEntity<ApiResponse<DaySummaryResponse, Void>> summary(@AuthenticationPrincipal CustomUserDetails me, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) TeamType team) {
        TeamType effectiveTeam = service.resolveEffectiveTeam(me.getRole(), me.getTeam(), team);
        DaySummaryResponse body = service.summary(date.toString(), effectiveTeam);
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.SUMMARY_RETRIEVED_SUCCESS, body));
    }

    /* ===== 날짜 요약(CSV) ===== */
    @GetMapping(value = "/{date}/summary.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> summaryCsv(@AuthenticationPrincipal CustomUserDetails me, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) TeamType team) {
        TeamType effective = service.resolveEffectiveTeam(me.getRole(), me.getTeam(), team);
        String csv = service.buildSummaryCsv(date.toString(), effective);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"attendance-" + date + ".csv\"")
                .body(csv);
    }

    @GetMapping(value = "/summary.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> summaryCsvAll(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestParam(required = false) TeamType team
    ) {
        TeamType effective = service.resolveEffectiveTeam(me.getRole(), me.getTeam(), team);

        String csv = service.buildFullMatrixCsv(effective);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"attendance-summary.csv\"")
                .body(csv);
    }

}
