package inha.gdgoc.domain.study.controller;

import inha.gdgoc.domain.study.dto.StudyAttendeeListWithMetaDto;
import inha.gdgoc.domain.study.dto.request.AttendeeCreateRequest;
import inha.gdgoc.domain.study.dto.response.GetStudyAttendeeListResponse;
import inha.gdgoc.domain.study.dto.response.GetStudyAttendeeResponse;
import inha.gdgoc.domain.study.dto.response.PageResponse;
import inha.gdgoc.domain.study.service.StudyAttendeeService;
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
    private final StudyAttendeeService studyAttendeeService;

    @GetMapping
    public ResponseEntity<ApiResponse<GetStudyAttendeeListResponse>> getAttendeeList(
            @PathVariable("studyId") Long studyId,
            @RequestParam("page") Optional<Long> page
    ) {
        StudyAttendeeListWithMetaDto result = studyAttendeeService.getStudyAttendeeList(studyId, page);
        PageResponse meta = new PageResponse(
                result.getPage().intValue(),
                result.getPageCount().intValue()
        );
        return ResponseEntity.ok(ApiResponse.of(new GetStudyAttendeeListResponse(result.getAttendees()), meta));
    }

    @GetMapping("/{attendeeId}")
    public ResponseEntity<ApiResponse<GetStudyAttendeeResponse>> getStudyAttendee(
            @PathVariable Long studyId,
            @PathVariable Long attendeeId
    ) {
        return ResponseEntity.ok(ApiResponse.of(studyAttendeeService.getStudyAttendee(studyId, attendeeId)));
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
