package inha.gdgoc.domain.club.club.dto.request;

import inha.gdgoc.domain.club.club.enums.ClubCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 개설. 정원·기간은 검증하지 않는다 — 운영진 대시보드에 경고로만 뜬다. */
public record ClubCreateRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull ClubCategory category,
    @NotBlank @Size(max = 200) String summary,
    String description,
    String activityMethod,
    @Size(max = 500) String imageUrl,
    @Size(max = 500) String kakaoLink,
    Integer capacity,
    LocalDate startDate,
    LocalDate endDate,
    Long termId) {}
