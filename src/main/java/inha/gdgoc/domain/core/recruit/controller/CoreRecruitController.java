package inha.gdgoc.domain.core.recruit.controller;

import inha.gdgoc.domain.core.recruit.dto.request.CoreRecruitApplicationRequest;
import inha.gdgoc.domain.core.recruit.dto.response.CoreRecruitApplicantDetailResponse;
import inha.gdgoc.domain.core.recruit.dto.response.CoreRecruitApplicantSummaryResponse;
import inha.gdgoc.domain.core.recruit.service.CoreRecruitApplicationService;
import inha.gdgoc.domain.core.recruit.entity.CoreRecruitApplication;
import inha.gdgoc.domain.core.recruit.controller.message.CoreRecruitApplicationMessage;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Core Recruit - Applicants", description = "코어 리쿠르트 지원자 조회 API")
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

    @Operation(
        summary = "코어 리쿠르트 지원자 목록 조회",
        description = "전체 목록 또는 이름 검색 결과를 반환합니다.",
        security = { @SecurityRequirement(name = "BearerAuth") }
    )
    @PreAuthorize("hasAnyRole('LEAD', 'ORGANIZER', 'ADMIN')")
    @GetMapping("/applicants")
    public ResponseEntity<ApiResponse<java.util.List<CoreRecruitApplicantSummaryResponse>, PageMeta>> getApplicants(
        @Parameter(description = "검색어(이름 부분 일치). 없으면 전체 조회", example = "홍길동")
        @RequestParam(required = false) String question,

        @Parameter(description = "페이지(0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size,

        @Parameter(description = "정렬 필드", example = "createdAt")
        @RequestParam(defaultValue = "createdAt") String sort,

        @Parameter(description = "정렬 방향 ASC/DESC", example = "DESC")
        @RequestParam(defaultValue = "DESC") String dir
    ) {
        Direction direction = "ASC".equalsIgnoreCase(dir) ? Direction.ASC : Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<CoreRecruitApplication> pageResult = service.findApplicantsPage(question, pageable);

        java.util.List<CoreRecruitApplicantSummaryResponse> list = pageResult
            .map(CoreRecruitApplicantSummaryResponse::from)
            .getContent();
        PageMeta meta = PageMeta.of(pageResult);

        return ResponseEntity.ok(
            ApiResponse.ok(CoreRecruitApplicationMessage.APPLICANT_LIST_RETRIEVED_SUCCESS, list, meta)
        );
    }

    @Operation(
        summary = "코어 리쿠르트 지원자 상세 조회",
        security = { @SecurityRequirement(name = "BearerAuth") }
    )
    @PreAuthorize("hasAnyRole('LEAD', 'ORGANIZER', 'ADMIN')")
    @GetMapping("/applicants/{id}")
    public ResponseEntity<ApiResponse<CoreRecruitApplicantDetailResponse, Void>> getApplicant(
        @PathVariable Long id
    ) {
        CoreRecruitApplicantDetailResponse response = service.getApplicantDetail(id);
        return ResponseEntity.ok(
            ApiResponse.ok(CoreRecruitApplicationMessage.APPLICANT_RETRIEVED_SUCCESS, response)
        );
    }
}


