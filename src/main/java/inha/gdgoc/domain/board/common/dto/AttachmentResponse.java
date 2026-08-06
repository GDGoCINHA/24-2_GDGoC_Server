package inha.gdgoc.domain.board.common.dto;

import inha.gdgoc.domain.board.common.enums.AttachmentKind;

/**
 * 첨부 응답 한 건.
 *
 * <p>{@code kind == LINK} 이면 {@code url} 만 채워지고 파일 관련 넷은 모두 null 이다. {@code kind == FILE} 이면
 * 반대로 {@code url} 이 null 이다.
 */
public record AttachmentResponse(
    Long id,
    AttachmentKind kind,
    String fileKey,
    String fileUrl,
    String fileName,
    Long fileSize,
    String url) {}
