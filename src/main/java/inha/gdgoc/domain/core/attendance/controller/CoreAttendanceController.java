package inha.gdgoc.domain.core.attendance.controller;

import inha.gdgoc.domain.core.attendance.controller.message.CoreAttendanceMessage;
import inha.gdgoc.domain.core.attendance.dto.request.CreateDateRequest;
import inha.gdgoc.domain.core.attendance.dto.response.DateListResponse;
import inha.gdgoc.domain.core.attendance.dto.response.DaySummaryResponse;
import inha.gdgoc.domain.core.attendance.dto.response.TeamResponse;
import inha.gdgoc.domain.core.attendance.service.CoreAttendanceService;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TODO: 임시 운용판(내일까지)
 *  - 권한 제거
 *  - 팀 구분은 query parameter로 처리 (?leadName=김정민 또는 ?teamId=team_xxx)
 *  - present 값은 query로 받음 (true/false), 바디 없이 호출 가능
 *  - 추후 리팩토링 시 Security/JWT/Service 분리 강화 예정
 */
@RestController
@RequestMapping("/api/v1/core-attendance")
@RequiredArgsConstructor
public class CoreAttendanceController {

    private final CoreAttendanceService service;

    /* Dates */
    @GetMapping("/dates")
    public ResponseEntity<ApiResponse<DateListResponse, Void>> getDates() {
        return ResponseEntity.ok(
                ApiResponse.ok(CoreAttendanceMessage.DATE_LIST_RETRIEVED_SUCCESS,
                        new DateListResponse(service.getDates()))
        );
    }

    @PostMapping("/dates")
    public ResponseEntity<ApiResponse<DateListResponse, Void>> createDate(
            @Valid @RequestBody CreateDateRequest request
    ) {
        service.addDate(request.getDate());
        return ResponseEntity.ok(
                ApiResponse.ok(CoreAttendanceMessage.DATE_CREATED_SUCCESS,
                        new DateListResponse(service.getDates()))
        );
    }

    @DeleteMapping("/dates/{date}")
    public ResponseEntity<ApiResponse<DateListResponse, Void>> deleteDate(@PathVariable String date) {
        service.deleteDate(date);
        return ResponseEntity.ok(
                ApiResponse.ok(CoreAttendanceMessage.DATE_DELETED_SUCCESS,
                        new DateListResponse(service.getDates()))
        );
    }

    /* Teams – leadName/teamId 로 필터 */
    @GetMapping("/teams")
    public ResponseEntity<ApiResponse<List<TeamResponse>, PageMeta>> getTeams(
            @RequestParam(required = false) String leadName,
            @RequestParam(required = false) String teamId
    ) {
        List<TeamResponse> list = service.getTeams(leadName, teamId);

        // 임시 Page 객체 생성 (page=0, size=list.size())
        var page = new PageImpl<>(list, PageRequest.of(0, Math.max(1, list.size()),
                Sort.by(Sort.Direction.DESC, "createdAt")), list.size());

        return ResponseEntity.ok(
                ApiResponse.ok(CoreAttendanceMessage.TEAM_LIST_RETRIEVED_SUCCESS, list, PageMeta.of(page))
        );
    }

    /* Members – 간단한 쿼리 파라미터 방식 */
    @PostMapping("/members")
    public ResponseEntity<ApiResponse<String, Void>> addMember(
            @RequestParam String teamId,
            @RequestParam String name
    ) {
        service.addMember(teamId, name);
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.MEMBER_ADDED_SUCCESS, "OK"));
    }

    @PutMapping("/members")
    public ResponseEntity<ApiResponse<String, Void>> renameMember(
            @RequestParam String teamId,
            @RequestParam String memberId,
            @RequestParam String name
    ) {
        service.renameMember(teamId, memberId, name);
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.MEMBER_UPDATED_SUCCESS, "OK"));
    }

    @DeleteMapping("/members")
    public ResponseEntity<ApiResponse<String, Void>> deleteMember(
            @RequestParam String teamId,
            @RequestParam String memberId
    ) {
        service.removeMember(teamId, memberId);
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.MEMBER_DELETED_SUCCESS, "OK"));
    }

    /* Attendance – present 를 쿼리로, 바디 불필요 */
    @PutMapping("/records/one")
    public ResponseEntity<ApiResponse<String, Void>> setAttendance(
            @RequestParam String date,     // YYYY-MM-DD
            @RequestParam String teamId,
            @RequestParam String memberId,
            @RequestParam boolean present
    ) {
        service.setAttendance(date, teamId, memberId, present);
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.ATTENDANCE_SET_SUCCESS, "OK"));
    }

    @PutMapping("/records/all")
    public ResponseEntity<ApiResponse<Long, Void>> setAll(
            @RequestParam String date,     // YYYY-MM-DD
            @RequestParam String teamId,
            @RequestParam boolean present
    ) {
        long count = service.setAll(date, teamId, present);
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.ATTENDANCE_ALL_SET_SUCCESS, count));
    }

    /* Summary – leadName/teamId 필터 가능 */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DaySummaryResponse, Void>> summary(
            @RequestParam String date,     // YYYY-MM-DD
            @RequestParam(required = false) String leadName,
            @RequestParam(required = false) String teamId
    ) {
        DaySummaryResponse body = service.summary(date, leadName, teamId);
        return ResponseEntity.ok(ApiResponse.ok(CoreAttendanceMessage.SUMMARY_RETRIEVED_SUCCESS, body));
    }
}