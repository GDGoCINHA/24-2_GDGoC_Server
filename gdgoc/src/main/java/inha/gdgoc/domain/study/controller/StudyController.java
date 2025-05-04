package inha.gdgoc.domain.study.controller;

import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.study.dto.StudyDto;
import inha.gdgoc.domain.study.dto.StudyListWithMetaDto;
import inha.gdgoc.domain.study.dto.request.StudyCreateRequest;
import inha.gdgoc.domain.study.dto.response.PageResponse;
import inha.gdgoc.domain.study.dto.response.StudyListRequest;
import inha.gdgoc.domain.study.enums.CreaterType;
import inha.gdgoc.domain.study.enums.StudyStatus;
import inha.gdgoc.domain.study.service.StudyService;
import inha.gdgoc.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;
    private final AuthService authService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<StudyListRequest>> getStudyList(
            @RequestParam("page") Optional<Long> page,
            @RequestParam("status") Optional<StudyStatus> status,
            @RequestParam("creatorType") Optional<CreaterType> creatorType
    ) {
        StudyListWithMetaDto result = studyService.getStudyList(page, status, creatorType);
        PageResponse meta = new PageResponse(
                result.getPage().intValue(),
                result.getPageCount().intValue()
        );
        return ResponseEntity.ok(ApiResponse.of(new StudyListRequest(result.getStudyList()), meta));
    }

    @GetMapping("/{studyId}")
    public ResponseEntity<ApiResponse<StudyDto>> getStudy(@PathVariable("studyId") Long id) {
        return ResponseEntity.ok(ApiResponse.of(studyService.getStudyById(id)));
    }

    @PostMapping("/")
    public ResponseEntity<ApiResponse<StudyDto>> createStudy(
            Authentication authentication,
            @RequestBody StudyCreateRequest body
    ) {
        Long userId = authService.getAuthenticationUserId(authentication);
        StudyDto createdStudy = studyService.createStudy(userId, body);
        return ResponseEntity.ok(ApiResponse.of(createdStudy));
    }

}
