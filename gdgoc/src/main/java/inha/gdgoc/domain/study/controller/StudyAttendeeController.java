package inha.gdgoc.domain.study.controller;

import inha.gdgoc.domain.study.dto.request.AttendeeCreateRequest;
import inha.gdgoc.domain.study.dto.response.GetApplicationResponse;
import inha.gdgoc.domain.study.dto.response.GetAttendeeListResponse;
import inha.gdgoc.domain.study.service.StudyAttendeeService;
import inha.gdgoc.domain.study.service.StudyService;
import inha.gdgoc.global.common.ApiResponse;
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

    private final StudyService studyService;
    private final StudyAttendeeService studyAttendeeService;

    @GetMapping
    public ResponseEntity<ApiResponse<GetAttendeeListResponse>> getAttendeeList(
            @PathVariable("studyId") Long studyId,
            @RequestParam("page") Optional<Long> page
    ) {
        return ResponseEntity.ok(studyAttendeeService.getAttendeeList(studyId, page));
    }

    @GetMapping("/{attendeeId}")
    public ResponseEntity<ApiResponse<GetApplicationResponse>> getApplication(
            @PathVariable Long studyId,
            @PathVariable Long attendeeId
    ) {
        return ResponseEntity.ok(ApiResponse.of(studyAttendeeService.getApplication(studyId, attendeeId)));
    }


    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createAttendee(
            Authentication authentication,
            @PathVariable Long studyId,
            @RequestBody AttendeeCreateRequest attendeeCreateRequest
            ) {
        studyAttendeeService.createAttendee(authentication, studyId, attendeeCreateRequest);
        return ResponseEntity.ok(ApiResponse.of(null, null));
    }

    @PatchMapping("/")
    public ResponseEntity<ApiResponse<Object>> updateAttendee(@PathVariable("studyId") Long id) {
        return ResponseEntity.ok(ApiResponse.of(studyAttendeeService.updateAttendee()));
    }

}
