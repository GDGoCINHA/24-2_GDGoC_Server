package inha.gdgoc.domain.board.common.service;

import inha.gdgoc.domain.board.common.dto.AttachmentEntry;
import inha.gdgoc.domain.board.common.dto.AttachmentResponse;
import inha.gdgoc.domain.board.common.entity.BoardAttachment;
import inha.gdgoc.domain.board.common.entity.BoardEntity;
import inha.gdgoc.domain.board.common.enums.AttachmentKind;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 세 게시판이 공유하는 첨부 규칙.
 *
 * <p>presigned PUT 은 5분 유효이고 서버는 업로드 성공 여부를 모른다. 클라이언트가 업로드에 실패하고도 키를 보내면 깨진
 * 다운로드 링크가 남으므로, 저장 전에 HeadObject 로 존재를 확인하고 크기를 함께 가져온다.
 */
@Component
@RequiredArgsConstructor
public class AttachmentPolicy {

  private static final int MAX_ATTACHMENTS = 10;

  private final S3Service s3Service;

  /** 요청 순서대로 sortOrder 를 부여하며 첨부를 게시글에 더한다. entries 가 null 이면 아무것도 하지 않는다. */
  public void apply(BoardEntity board, List<AttachmentEntry> entries) {
    if (entries == null) return;

    if (entries.size() > MAX_ATTACHMENTS) {
      throw new BusinessException(
          GlobalErrorCode.BAD_REQUEST, "첨부는 최대 " + MAX_ATTACHMENTS + "개까지 등록할 수 있습니다.");
    }

    for (int i = 0; i < entries.size(); i++) {
      AttachmentEntry entry = entries.get(i);

      if (entry.url() != null && !entry.url().isBlank()) {
        board.addLinkAttachment(entry.url(), i);
        continue;
      }

      Long size = s3Service.getObjectSize(entry.fileKey());
      if (size == null) {
        throw new BusinessException(
            GlobalErrorCode.BAD_REQUEST, "업로드되지 않은 파일입니다: " + entry.fileName());
      }
      board.addFileAttachment(entry.fileKey(), entry.fileName(), size, i);
    }
  }

  /** sortOrder 순으로 정렬해 응답으로 바꾼다. LINK 는 S3 URL 을 만들지 않는다. */
  public List<AttachmentResponse> toResponses(List<? extends BoardAttachment> attachments) {
    return attachments.stream()
        .sorted(Comparator.comparingInt(BoardAttachment::getSortOrder))
        .map(this::toResponse)
        .toList();
  }

  private AttachmentResponse toResponse(BoardAttachment a) {
    if (a.getKind() == AttachmentKind.LINK) {
      return new AttachmentResponse(a.getId(), a.getKind(), null, null, null, null, a.getUrl());
    }
    return new AttachmentResponse(
        a.getId(),
        a.getKind(),
        a.getFileKey(),
        s3Service.getS3FileUrl(a.getFileKey()),
        a.getFileName(),
        a.getFileSize(),
        null);
  }
}
