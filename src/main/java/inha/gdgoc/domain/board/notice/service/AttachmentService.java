package inha.gdgoc.domain.board.notice.service;

import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.board.notice.entity.NoticeBoardAttachment;
import inha.gdgoc.domain.board.notice.enums.AttachmentTypeEnum;
import inha.gdgoc.domain.board.notice.repository.NoticeBoardRepository;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttachmentService {

    private final NoticeBoardRepository noticeBoardRepository;
    private final S3Service s3Service;

    public AttachmentDownloadResult downloadAttachment(UUID articleId, UUID attachmentId) {
        NoticeBoard board = noticeBoardRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        NoticeBoardAttachment attachment = board.getAttachments().stream()
                .filter(a -> a.getAttachmentId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        if (attachment.getAttachmentType() != AttachmentTypeEnum.FILE || attachment.getStoredName() == null) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST);
        }

        ResponseInputStream<GetObjectResponse> inputStream = s3Service.download(attachment.getStoredName());

        return new AttachmentDownloadResult(
                inputStream,
                attachment.getOriginalName(),
                attachment.getFileSize(),
                attachment.getMimeType()
        );
    }

    public record AttachmentDownloadResult(
            ResponseInputStream<GetObjectResponse> inputStream,
            String originalName,
            Long fileSize,
            String mimeType
    ) {}
}
