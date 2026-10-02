package inha.gdgoc.domain.club.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 보완 요청. 사유는 필수다 — 리더가 무엇을 고칠지 알아야 다시 낸다. */
public record ClubRevisionRequest(@NotBlank @Size(max = 1000) String reason) {}
