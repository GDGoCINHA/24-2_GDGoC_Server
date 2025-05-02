package inha.gdgoc.domain.study.controller;

import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.study.service.StudyService;
import inha.gdgoc.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;
    private final AuthService authService;

    @GetMapping("/")
    public ResponseEntity<ApiResponse<Object>> getStudyList() {
        return ResponseEntity.ok(ApiResponse.of(studyService.getStudyList()));
    }

    @GetMapping("/{studyId}")
    public ResponseEntity<ApiResponse<Object>> getStudy(Authentication authentication, @PathVariable("studyId") Long id) {
        Long userId = authService.getAuthenticationUserId(authentication);
        return ResponseEntity.ok(ApiResponse.of(studyService.getStudyById(id)));
    }

    @PostMapping("/")
    public ResponseEntity<ApiResponse<Object>> createStudy() {
        return ResponseEntity.ok(ApiResponse.of(studyService.createStudy()));
    }

}
