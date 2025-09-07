package inha.gdgoc.domain.core.recruit.controller;

import inha.gdgoc.domain.core.recruit.dto.request.CoreRecruitApplicationRequest;
import inha.gdgoc.domain.core.recruit.service.CoreRecruitApplicationService;
import inha.gdgoc.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/core-recruit")
@RequiredArgsConstructor
public class CoreRecruitController {

    private final CoreRecruitApplicationService service;

    private record CreateResponse(Long id, String status) {}

    @PostMapping
    public ResponseEntity<ApiResponse<CreateResponse, Void>> create(
        @Valid @RequestBody CoreRecruitApplicationRequest request
    ) {
        Long id = service.create(request);
        return ResponseEntity.ok(ApiResponse.ok("OK", new CreateResponse(id, "OK")));
    }
}


