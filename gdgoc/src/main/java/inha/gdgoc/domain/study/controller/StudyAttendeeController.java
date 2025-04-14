package inha.gdgoc.domain.study.controller;

import inha.gdgoc.domain.study.service.StudyAttendeeService;
import inha.gdgoc.domain.study.service.StudyService;
import inha.gdgoc.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/study/{studyId}/attendee")
@RequiredArgsConstructor
public class StudyAttendeeController {

    private final StudyService studyService;
    private final StudyAttendeeService studyAttendeeService;

    @GetMapping("/")
    public ResponseEntity<ApiResponse<Object>> getAttendeeList(@PathVariable("studyId") Long id) {
        return ResponseEntity.ok(ApiResponse.of(studyAttendeeService.getAttendeeList()));
    }

    @PostMapping("/")
    public ResponseEntity<ApiResponse<Object>> createAttendee(@PathVariable("studyId") Long id) {
        return ResponseEntity.ok(ApiResponse.of(studyAttendeeService.createAttendee()));
    }

    @PatchMapping("/")
    public ResponseEntity<ApiResponse<Object>> updateAttendee(@PathVariable("studyId") Long id) {
        return ResponseEntity.ok(ApiResponse.of(studyAttendeeService.updateAttendee()));
    }

}
