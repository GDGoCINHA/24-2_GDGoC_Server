package inha.gdgoc.domain.study.controller;

import static inha.gdgoc.domain.study.controller.message.StudyAttendeeMessage.STUDY_ATTENDEES_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.study.controller.message.StudyAttendeeMessage.STUDY_ATTENDEE_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.study.controller.message.StudyAttendeeMessage.STUDY_ATTENDEE_SAVE_SUCCESS;
import static inha.gdgoc.domain.study.controller.message.StudyAttendeeMessage.STUDY_ATTENDEE_UPDATE_SUCCESS;

import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.study.dto.StudyAttendeeListWithMetaDto;
import inha.gdgoc.domain.study.dto.request.AttendeeCreateRequest;
import inha.gdgoc.domain.study.dto.request.AttendeeUpdateRequest;
import inha.gdgoc.domain.study.dto.response.GetStudyAttendeeListResponse;
import inha.gdgoc.domain.study.dto.response.GetStudyAttendeeResponse;
import inha.gdgoc.domain.study.dto.response.PageResponse;
import inha.gdgoc.domain.study.service.StudyAttendeeService;
import inha.gdgoc.global.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/study/{studyId}/attendee")
@RequiredArgsConstructor
public class StudyAttendeeController {

    private final StudyAttendeeService studyAttendeeService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<GetStudyAttendeeListResponse, PageResponse>> getAttendeeList(
            @PathVariable("studyId") Long studyId,
            @RequestParam("page") Optional<Long> page
    ) {
        StudyAttendeeListWithMetaDto result = studyAttendeeService.getStudyAttendeeList(
                studyId,
                page
        );
        PageResponse meta = new PageResponse(
                result.getPage().intValue(),
                result.getPageCount().intValue()
        );
        GetStudyAttendeeListResponse response = new GetStudyAttendeeListResponse(
                result.getAttendees()
        );

        return ResponseEntity.ok(ApiResponse.ok(STUDY_ATTENDEES_RETRIEVED_SUCCESS, response, meta));
    }

    @GetMapping("/{attendeeId}")
    public ResponseEntity<ApiResponse<GetStudyAttendeeResponse, Void>> getStudyAttendee(
            Authentication authentication,
            @PathVariable Long studyId,
            @PathVariable Long attendeeId
    ) {
        GetStudyAttendeeResponse response = studyAttendeeService.getStudyAttendee(
                authService.getAuthenticationUserId(authentication),
                studyId,
                attendeeId
        );

        return ResponseEntity.ok(ApiResponse.ok(STUDY_ATTENDEE_RETRIEVED_SUCCESS, response));
    }

    // TODO 이거 언제 쓰이는 거지
    @PostMapping
    public ResponseEntity<ApiResponse<GetStudyAttendeeResponse, Void>> createAttendee(
            Authentication authentication,
            @PathVariable Long studyId,
            @RequestBody AttendeeCreateRequest attendeeCreateRequest
    ) {
        Long userId = authService.getAuthenticationUserId(authentication);
        GetStudyAttendeeResponse response = studyAttendeeService.createAttendee(
                userId,
                studyId,
                attendeeCreateRequest
        );

        return ResponseEntity.ok(ApiResponse.ok(STUDY_ATTENDEE_SAVE_SUCCESS, response));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<Boolean, Void>> updateAttendee(
            Authentication authentication,
            @PathVariable("studyId") Long studyId,
            @RequestBody AttendeeUpdateRequest request
    ) {
        Long userId = authService.getAuthenticationUserId(authentication);
        studyAttendeeService.updateAttendee(userId, studyId, request);

        return ResponseEntity.ok(ApiResponse.ok(STUDY_ATTENDEE_UPDATE_SUCCESS, true));
    }

}
