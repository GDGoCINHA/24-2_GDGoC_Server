package inha.gdgoc.domain.board.free.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 댓글 수정. 내용만 바꾼다 — 부모는 옮기지 않는다. */
public record FreeBoardCommentUpdateRequest(@NotBlank @Size(max = 1000) String content) {}
