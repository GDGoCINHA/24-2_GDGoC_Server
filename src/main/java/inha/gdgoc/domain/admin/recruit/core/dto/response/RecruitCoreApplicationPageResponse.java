package inha.gdgoc.domain.admin.recruit.core.dto.response;

import java.util.List;

public record RecruitCoreApplicationPageResponse(
    List<RecruitCoreApplicantSummaryResponse> content,
    Pageable pageable,
    long totalElements,
    int totalPages,
    boolean last
) {

    public static RecruitCoreApplicationPageResponse from(
        List<RecruitCoreApplicantSummaryResponse> items,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
    ) {
        return new RecruitCoreApplicationPageResponse(
            items,
            new Pageable(pageNumber, pageSize),
            totalElements,
            totalPages,
            last
        );
    }

    public record Pageable(int pageNumber, int pageSize) {}
}
