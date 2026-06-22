package inha.gdgoc.domain.board.notice.controller;

import inha.gdgoc.domain.board.notice.service.AttachmentService;
import inha.gdgoc.domain.board.notice.service.AttachmentService.AttachmentDownloadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/board/notices")
@RequiredArgsConstructor
public class AttachmentController {

    private static final String MEMBER_OR_HIGHER_RULE =
            "@accessGuard.check(authentication,"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).atLeast("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).MEMBER))";

    private final AttachmentService attachmentService;

    @PreAuthorize(MEMBER_OR_HIGHER_RULE)
    @GetMapping("/{articleId}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID articleId,
            @PathVariable UUID attachmentId) {
        AttachmentDownloadResult result = attachmentService.downloadAttachment(articleId, attachmentId);

        String encodedFileName = UriUtils.encode(result.originalName(), StandardCharsets.UTF_8);
        MediaType mediaType = MediaType.parseMediaType(result.mimeType() != null ? result.mimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(result.fileSize() != null ? result.fileSize() : 0)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(encodedFileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(result.inputStream()));
    }
}
