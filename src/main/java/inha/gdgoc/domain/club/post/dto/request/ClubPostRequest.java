package inha.gdgoc.domain.club.post.dto.request;

import inha.gdgoc.domain.club.post.enums.ClubPostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 팀 일반 게시글 작성·수정. 수정도 화면 전체를 다시 보낸다. 완주에는 반영하지 않는다. */
public record ClubPostRequest(
    @NotNull ClubPostCategory category,
    @NotBlank @Size(max = 5000) String content,
    @Size(max = 4) List<@NotBlank @Size(max = 500) String> imageUrls) {}
