package inha.gdgoc.domain.club.club.dto.request;

import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubRecruitStatus;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 이끔이 수정. null 인 항목은 그대로 둔다. */
public record ClubUpdateRequest(
    @Size(max = 100) String name,
    ClubCategory category,
    @Size(max = 200) String summary,
    String description,
    String activityMethod,
    @Size(max = 500) String imageUrl,
    @Size(max = 500) String kakaoLink,
    Integer capacity,
    LocalDate startDate,
    LocalDate endDate,
    ClubRecruitStatus recruitStatus) {}
