package inha.gdgoc.domain.study.controller;

import static inha.gdgoc.domain.study.controller.message.StudyMessage.APPLIED_STUDY_LIST_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.study.controller.message.StudyMessage.MY_STUDY_LIST_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.study.controller.message.StudyMessage.STUDY_CREATE_SUCCESS;
import static inha.gdgoc.domain.study.controller.message.StudyMessage.STUDY_LIST_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.study.controller.message.StudyMessage.STUDY_RETRIEVED_SUCCESS;

import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.study.dto.StudyAttendeeResultDto;
import inha.gdgoc.domain.study.dto.StudyDto;
import inha.gdgoc.domain.study.dto.StudyListWithMetaDto;
import inha.gdgoc.domain.study.dto.request.StudyCreateRequest;
import inha.gdgoc.domain.study.dto.response.GetDetailedStudyResponse;
import inha.gdgoc.domain.study.dto.response.GetStudyAttendeeResultResponse;
import inha.gdgoc.domain.study.dto.response.MyStudyRecruitResponse;
import inha.gdgoc.domain.study.dto.response.PageResponse;
import inha.gdgoc.domain.study.dto.response.StudyListRequest;
import inha.gdgoc.domain.study.enums.CreatorType;
import inha.gdgoc.domain.study.enums.StudyStatus;
import inha.gdgoc.domain.study.service.StudyAttendeeService;
import inha.gdgoc.domain.study.service.StudyService;
import inha.gdgoc.global.dto.response.ApiResponse;
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

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;
    private final StudyAttendeeService studyAttendeeService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<StudyListRequest, PageResponse>> getStudyList(
            @RequestParam("page") Optional<Long> page,
            @RequestParam("status") Optional<StudyStatus> status,
            @RequestParam("creatorType") Optional<CreatorType> creatorType
    ) {
        StudyListWithMetaDto result = studyService.getStudyList(page, status, creatorType);
        PageResponse meta = new PageResponse(
                result.getPage().intValue(),
                result.getPageCount().intValue()
        );
        StudyListRequest response = new StudyListRequest(result.getStudyList());

        return ResponseEntity.ok(ApiResponse.ok(STUDY_LIST_RETRIEVED_SUCCESS, response, meta));
    }

    @GetMapping("/attendee/result")
    public ResponseEntity<ApiResponse<GetStudyAttendeeResultResponse, Void>> getStudyAttendeeResultList(
            Authentication authentication
    ) {
        Long userId = authService.getAuthenticationUserId(authentication);
        List<StudyAttendeeResultDto> attendeeDtoList = studyAttendeeService
                .getStudyAttendeeResultListByUserId(userId);
        GetStudyAttendeeResultResponse response = new GetStudyAttendeeResultResponse(
                attendeeDtoList
        );

        return ResponseEntity.ok(ApiResponse.ok(APPLIED_STUDY_LIST_RETRIEVED_SUCCESS, response));
    }


    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyStudyRecruitResponse, Void>> getMyStudyList(
            Authentication authentication
    ) {
        Long userId = authService.getAuthenticationUserId(authentication);
        MyStudyRecruitResponse response = studyService.getMyStudyList(userId);

        return ResponseEntity.ok(ApiResponse.ok(MY_STUDY_LIST_RETRIEVED_SUCCESS, response));
    }

    @GetMapping("/{studyId}")
    public ResponseEntity<ApiResponse<GetDetailedStudyResponse, Void>> getStudy(
            @PathVariable("studyId") Long id
    ) {
        GetDetailedStudyResponse response = studyService.getStudyById(id);

        return ResponseEntity.ok(ApiResponse.ok(STUDY_RETRIEVED_SUCCESS, response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudyDto, Void>> createStudy(
            Authentication authentication,
            @RequestBody StudyCreateRequest body
    ) {
        Long userId = authService.getAuthenticationUserId(authentication);
        StudyDto createdStudy = studyService.createStudy(userId, body);

        return ResponseEntity.ok(ApiResponse.ok(STUDY_CREATE_SUCCESS, createdStudy));
    }

}
