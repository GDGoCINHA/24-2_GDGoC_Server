package inha.gdgoc.domain.core.attendance.controller;

import inha.gdgoc.domain.core.attendance.controller.message.CoreAttendanceMessage;
import inha.gdgoc.domain.core.attendance.dto.request.CreateDateRequest;
import inha.gdgoc.domain.core.attendance.dto.request.SetAttendanceRequest;
import inha.gdgoc.domain.core.attendance.dto.response.DateListResponse;
import inha.gdgoc.domain.core.attendance.dto.response.DaySummaryResponse;
import inha.gdgoc.domain.core.attendance.dto.response.TeamResponse;
import inha.gdgoc.domain.core.attendance.service.CoreAttendanceService;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
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
@PreAuthorize("hasAnyRole('LEAD','ORGANIZER','ADMIN')")
public class CoreAttendanceController {

    private final CoreAttendanceService service;

    /* ===== helpers ===== */
    private static TeamType requiredTeamFrom(CustomUserDetails me) {
        if (me.getTeam() == null) throw new IllegalArgumentException("LEAD 권한 토큰에 team 정보가 없습니다.");
        return me.getTeam();
    }

    private static ResponseEntity<ApiResponse<Map<String, Object>, Void>> okUpdated(long updated, List<Long> ignored) {
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.ATTENDANCE_ALL_SET_SUCCESS, Map.of("updated", updated, "ignoredUserIds", ignored)));
    }

    /* ===== Meetings(날짜) 목록 ===== */
    @GetMapping
    public ResponseEntity<ApiResponse<DateListResponse, Void>> listDates() {
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.DATE_LIST_RETRIEVED_SUCCESS, new DateListResponse(service.getDates())));
    }

    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<DateListResponse, Void>> createDate(@Valid @RequestBody CreateDateRequest request) {
        service.addDate(request.getDate());
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.DATE_CREATED_SUCCESS, new DateListResponse(service.getDates())));
    }

    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @DeleteMapping("/{date}")
    public ResponseEntity<ApiResponse<DateListResponse, Void>> deleteDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        service.deleteDate(date.toString());
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.DATE_DELETED_SUCCESS, new DateListResponse(service.getDates())));
    }

    /* ===== 팀 목록 (리드=본인 팀만 / 관리자=전체) ===== */
    @GetMapping("/teams")
    public ResponseEntity<ApiResponse<List<TeamResponse>, PageMeta>> getTeams(@AuthenticationPrincipal CustomUserDetails me) {
        List<TeamResponse> list = (me.getRole() == UserRole.LEAD && me.getTeam() != TeamType.HR) ? service.getTeamsForLead(requiredTeamFrom(me)) : service.getTeamsForOrganizerOrAdmin();

        var page = new PageImpl<>(list, PageRequest.of(0, Math.max(1, list.size()), Sort.by(Sort.Direction.DESC, "createdAt")), list.size());
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.TEAM_LIST_RETRIEVED_SUCCESS, list, PageMeta.of(page)));
    }

    /* ===== 특정 날짜의 팀원+현재 출석 상태 조회 (리드=본인 팀만) ===== */
    // 프론트가 체크박스 채우기 전에 필요한 목록/상태
    @GetMapping("/{date}/members")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>, Void>> membersOfMeeting(@AuthenticationPrincipal CustomUserDetails me, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) TeamType team // 관리자만 사용, 리드는 무시
    ) {
        TeamType effectiveTeam = (me.getRole() == UserRole.LEAD && me.getTeam() != TeamType.HR) ? requiredTeamFrom(me) : team;
        var list = service.getMembersWithPresence(date.toString(), effectiveTeam);
        // list 원소 예시: { "userId": "123", "name": "홍길동", "present": true, "lastModifiedAt": "..." }
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.TEAM_LIST_RETRIEVED_SUCCESS, list));
    }

    /* ===== 특정 날짜 출석 일괄 저장 (멱등 스냅샷) ===== */
    // Body: { "userIds": ["1","2",...], "present": true }  → presentUserIds만 보내는 구조로도 쉽게 변환 가능
    @PutMapping("/{date}/attendance")
    public ResponseEntity<ApiResponse<Map<String, Object>, Void>> saveAttendanceSnapshot(@AuthenticationPrincipal CustomUserDetails me, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestBody @Valid SetAttendanceRequest req) {
        var userIds = req.safeUserIds();

        // LEAD → 본인 팀 검증
        if (me.getRole() == UserRole.LEAD && me.getTeam() != TeamType.HR) {
            TeamType myTeam = requiredTeamFrom(me);
            var validation = service.filterUserIdsNotInTeam(myTeam, userIds);
            if (validation.validIds().isEmpty()) {
                return okUpdated(0L, validation.invalidIds());
            }
            long updated = service.setAttendance(date.toString(), validation.validIds(), req.presentValue());
            return okUpdated(updated, validation.invalidIds());
        }

        // ORGANIZER / ADMIN → 팀 추론/검증 없이 바로 업서트
        long updated = service.setAttendance(date.toString(), userIds, req.presentValue());
        return okUpdated(updated, List.of());
    }

    /* ===== 날짜 요약(JSON) ===== */
    @GetMapping("/{date}/summary")
    public ResponseEntity<ApiResponse<DaySummaryResponse, Void>> summary(@AuthenticationPrincipal CustomUserDetails me, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) TeamType team) {
        DaySummaryResponse body = (me.getRole() == UserRole.LEAD && me.getTeam() != TeamType.HR) ? service.summary(date.toString(), requiredTeamFrom(me)) : service.summary(date.toString(), team);
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.SUMMARY_RETRIEVED_SUCCESS, body));
    }

    /* ===== 날짜 요약(CSV) ===== */
    @GetMapping(value = "/{date}/summary.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> summaryCsv(@AuthenticationPrincipal CustomUserDetails me, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) TeamType team) {
        TeamType effective = (me.getRole() == UserRole.LEAD && me.getTeam() != TeamType.HR) ? requiredTeamFrom(me) : team;
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
        // LEAD & not HR → 자신의 팀만
        TeamType effective = (me.getRole() == UserRole.LEAD && me.getTeam() != TeamType.HR)
                ? requiredTeamFrom(me)
                : team;

        String csv = service.buildFullMatrixCsv(effective);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"attendance-summary.csv\"")
                .body(csv);
    }

}