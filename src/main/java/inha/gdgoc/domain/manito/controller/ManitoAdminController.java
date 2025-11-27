package inha.gdgoc.domain.manito.controller;

import inha.gdgoc.domain.manito.dto.request.ManitoSessionCreateRequest;
import inha.gdgoc.domain.manito.dto.response.ManitoSessionResponse;
import inha.gdgoc.domain.manito.service.ManitoAdminService;
import inha.gdgoc.global.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/manito")
@RequiredArgsConstructor
public class ManitoAdminController {

    private final ManitoAdminService manitoAdminService;

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<ManitoSessionResponse>, Void>> listSessions() {
        List<ManitoSessionResponse> body = manitoAdminService.listSessions();
        return ResponseEntity.ok(
                ApiResponse.ok("세션 목록 조회 성공", body)
        );
    }

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<ManitoSessionResponse, Void>> createSession(
            @RequestBody ManitoSessionCreateRequest request
    ) {
        ManitoSessionResponse body = manitoAdminService.createSession(request);
        return ResponseEntity.ok(
                ApiResponse.ok("세션 생성 성공", body)
        );
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> uploadCsv(@RequestParam("sessionCode") String sessionCode, @RequestParam("file") MultipartFile file) {
        manitoAdminService.importParticipantsCsv(sessionCode, file);
        String csv = manitoAdminService.buildAssignmentsCsv(sessionCode);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"manito-" + sessionCode + ".csv\"")
                .body(csv);
    }

    @PostMapping(value = "/upload-encrypted", consumes = "multipart/form-data")
    public ResponseEntity<Void> uploadEncrypted(@RequestParam("sessionCode") String sessionCode, @RequestParam("file") MultipartFile file) {
        manitoAdminService.importEncryptedCsv(sessionCode, file);
        return ResponseEntity.ok().build();
    }
}