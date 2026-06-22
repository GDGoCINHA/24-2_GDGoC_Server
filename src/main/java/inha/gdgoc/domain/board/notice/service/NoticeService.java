package inha.gdgoc.domain.board.notice.service;

import inha.gdgoc.domain.board.notice.dto.request.NoticeCreateRequest;
import inha.gdgoc.domain.board.notice.dto.request.NoticeSearchCondition;
import inha.gdgoc.domain.board.notice.dto.request.NoticeUpdateRequest;
import inha.gdgoc.domain.board.notice.dto.request.NoticeUpdateRequest.KeepAttachmentEntry;
import inha.gdgoc.domain.board.notice.dto.response.DeletedNoticeListResponse;
import inha.gdgoc.domain.board.notice.dto.response.NoticeDetailResponse;
import inha.gdgoc.domain.board.notice.dto.response.NoticeDetailResponse.AttachmentEntry;
import inha.gdgoc.domain.board.notice.dto.response.NoticeListResponse;
import inha.gdgoc.domain.board.notice.dto.response.NoticeListResponse.NoticeSummaryEntry;
import inha.gdgoc.domain.board.notice.dto.response.NoticeSimpleResponse;
import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.board.notice.entity.NoticeBoardAttachment;
import inha.gdgoc.domain.board.notice.enums.ArticleStatusEnum;
import inha.gdgoc.domain.board.notice.enums.AttachmentTypeEnum;
import inha.gdgoc.domain.board.notice.exception.NoticeErrorCode;
import inha.gdgoc.domain.board.notice.repository.NoticeBoardRepository;
import inha.gdgoc.domain.resource.enums.S3KeyType;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeBoardRepository noticeBoardRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public NoticeListResponse listNotices(int page, int size, NoticeSearchCondition condition) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<NoticeBoard> articlesPage = noticeBoardRepository.findVisibleBoards(condition, pageRequest);

        List<NoticeSummaryEntry> pinnedEntries = Collections.emptyList();
        if (page == 0 && condition.category() == null && (condition.keyword() == null || condition.keyword().isBlank())) {
            pinnedEntries = noticeBoardRepository.findPinnedNotices().stream()
                    .map(this::toSummaryEntry)
                    .toList();
        }

        List<NoticeSummaryEntry> articlesEntries = articlesPage.getContent().stream()
                .map(this::toSummaryEntry)
                .toList();

        return new NoticeListResponse(
                articlesPage.getTotalElements(),
                articlesPage.getTotalPages(),
                articlesPage.getNumber(),
                pinnedEntries,
                articlesEntries
        );
    }

    @Transactional
    public NoticeDetailResponse getNotice(UUID articleId, UserRole role, Long userId) {
        NoticeBoard board = noticeBoardRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        if (board.getStatus() == ArticleStatusEnum.PENDING) {
            if (!UserRole.hasAtLeast(role, UserRole.LEAD) && !board.getPostedBy().equals(userId)) {
                throw new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND);
            }
        }

        board.incrementViewCount();

        Optional<NoticeBoard> prevOpt = noticeBoardRepository.findPrevNotice(board.getArticleNumber(), board.getCategory());
        Optional<NoticeBoard> nextOpt = noticeBoardRepository.findNextNotice(board.getArticleNumber(), board.getCategory());

        NoticeSimpleResponse prevResponse = prevOpt.map(this::toSimpleResponse).orElse(null);
        NoticeSimpleResponse nextResponse = nextOpt.map(this::toSimpleResponse).orElse(null);

        List<AttachmentEntry> attachmentEntries = board.getAttachments().stream()
                .map(this::toAttachmentEntry)
                .toList();

        return new NoticeDetailResponse(
                board.getArticleId(),
                board.getArticleNumber(),
                board.getCategory(),
                board.getTitle(),
                board.getPostedByName(),
                board.getStatus(),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                board.getDeletedAt(),
                board.getViewCount(),
                board.getContent(),
                attachmentEntries,
                prevResponse,
                nextResponse
        );
    }

    @Transactional
    public UUID createNotice(NoticeCreateRequest req, MultipartFile[] files, MultipartFile[] images, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        if (Boolean.TRUE.equals(req.isPinned())) {
            validatePinLimit(null);
        }

        NoticeBoard board = NoticeBoard.create(
                req.title(),
                req.category(),
                req.content(),
                Boolean.TRUE.equals(req.isPinned()),
                req.status(),
                userId,
                user.getName()
        );

        if (images != null && images.length > 0) {
            String processedContent = replaceImagePlaceholders(req.content(), images, userId);
            board.updateContent(processedContent);
        }

        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (!file.isEmpty()) {
                    try {
                        String key = s3Service.upload(userId, S3KeyType.notice, file);
                        String url = s3Service.getS3FileUrl(key);
                        board.addAttachment(
                                AttachmentTypeEnum.FILE,
                                file.getOriginalFilename(),
                                key,
                                url,
                                file.getSize(),
                                file.getContentType(),
                                null,
                                i + 1
                        );
                    } catch (IOException e) {
                        throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        }

        if (req.urlAttachments() != null) {
            req.urlAttachments().forEach(urlAtt -> board.addAttachment(
                    AttachmentTypeEnum.URL,
                    null,
                    null,
                    null,
                    null,
                    null,
                    urlAtt.linkUrl(),
                    urlAtt.displayOrder()
            ));
        }

        return noticeBoardRepository.save(board).getArticleId();
    }

    @Transactional
    public void updateNotice(UUID articleId, NoticeUpdateRequest req, MultipartFile[] files, MultipartFile[] images, UserRole role, Long userId) {
        NoticeBoard board = noticeBoardRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        requireAuthorOrLead(board, role, userId);

        if (req.isPinned() != null && req.isPinned()) {
            if (!board.isPinned()) {
                validatePinLimit(articleId);
            }
        }

        board.update(
                req.title(),
                req.category(),
                req.content(),
                req.isPinned(),
                req.status()
        );

        if (images != null && images.length > 0) {
            String targetContent = req.content() != null ? req.content() : board.getContent();
            String processedContent = replaceImagePlaceholders(targetContent, images, userId);
            board.updateContent(processedContent);
        }

        List<NoticeBoardAttachment> toRemove = new ArrayList<>();
        for (NoticeBoardAttachment existing : board.getAttachments()) {
            Optional<KeepAttachmentEntry> keepOpt = req.keepAttachmentIds() == null ? Optional.empty() :
                    req.keepAttachmentIds().stream()
                            .filter(k -> k.attachmentId().equals(existing.getAttachmentId()))
                            .findFirst();

            if (keepOpt.isPresent()) {
                existing.updateDisplayOrder(keepOpt.get().displayOrder());
            } else {
                toRemove.add(existing);
            }
        }
        board.getAttachments().removeAll(toRemove);

        int startOrder = board.getAttachments().size() + 1;
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (!file.isEmpty()) {
                    try {
                        String key = s3Service.upload(userId, S3KeyType.notice, file);
                        String url = s3Service.getS3FileUrl(key);
                        board.addAttachment(
                                AttachmentTypeEnum.FILE,
                                file.getOriginalFilename(),
                                key,
                                url,
                                file.getSize(),
                                file.getContentType(),
                                null,
                                startOrder + i
                        );
                    } catch (IOException e) {
                        throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        }

        if (req.urlAttachments() != null) {
            req.urlAttachments().forEach(urlAtt -> board.addAttachment(
                    AttachmentTypeEnum.URL,
                    null,
                    null,
                    null,
                    null,
                    null,
                    urlAtt.linkUrl(),
                    urlAtt.displayOrder()
            ));
        }
    }

    @Transactional
    public void deleteNotice(UUID articleId, UserRole role, Long userId) {
        NoticeBoard board = noticeBoardRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        requireAuthorOrLead(board, role, userId);
        board.softDelete();
    }

    @Transactional
    public void restoreNotice(UUID articleId, UserRole role, Long userId) {
        if (!UserRole.hasAtLeast(role, UserRole.LEAD)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
        }

        NoticeBoard board = noticeBoardRepository.findDeletedById(articleId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        board.restore();
    }

    public Page<DeletedNoticeListResponse> listDeletedNotices(int page, int size, NoticeSearchCondition condition) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return noticeBoardRepository.findDeletedBoards(condition, pageRequest)
                .map(this::toDeletedResponse);
    }

    private void requireAuthorOrLead(NoticeBoard board, UserRole role, Long userId) {
        if (UserRole.hasAtLeast(role, UserRole.LEAD)) {
            return;
        }
        if (!board.getPostedBy().equals(userId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
        }
    }

    private void validatePinLimit(UUID currentArticleId) {
        List<NoticeBoard> currentPinned = noticeBoardRepository.findPinnedNotices();
        long pinCount = currentPinned.stream()
                .filter(b -> currentArticleId == null || !b.getArticleId().equals(currentArticleId))
                .count();

        if (pinCount >= 3) {
            throw new BusinessException(NoticeErrorCode.NOTICE_PIN_LIMIT_EXCEEDED);
        }
    }

    private String replaceImagePlaceholders(String content, MultipartFile[] images, Long userId) {
        if (content == null || images == null) {
            return content;
        }
        String replaced = content;
        for (int i = 0; i < images.length; i++) {
            MultipartFile file = images[i];
            if (!file.isEmpty()) {
                try {
                    String key = s3Service.upload(userId, S3KeyType.notice, file);
                    String url = s3Service.getS3FileUrl(key);
                    replaced = replaced.replace("src=\"image-" + i + "\"", "src=\"" + url + "\"");
                    replaced = replaced.replace("src='image-" + i + "'", "src='" + url + "'");
                } catch (IOException e) {
                    throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
                }
            }
        }
        return replaced;
    }

    private NoticeSummaryEntry toSummaryEntry(NoticeBoard board) {
        return new NoticeSummaryEntry(
                board.getArticleId(),
                board.getArticleNumber(),
                board.getCategory(),
                board.getTitle(),
                board.getPostedByName(),
                board.getCreatedAt(),
                board.getViewCount(),
                board.isPinned(),
                board.getStatus()
        );
    }

    private NoticeSimpleResponse toSimpleResponse(NoticeBoard board) {
        return new NoticeSimpleResponse(
                board.getArticleId(),
                board.getArticleNumber(),
                board.getCategory(),
                board.getTitle(),
                board.getPostedByName(),
                board.getCreatedAt(),
                board.getViewCount()
        );
    }

    private AttachmentEntry toAttachmentEntry(NoticeBoardAttachment attachment) {
        return new AttachmentEntry(
                attachment.getAttachmentId(),
                attachment.getAttachmentType(),
                attachment.getOriginalName(),
                attachment.getFileSize(),
                attachment.getMimeType(),
                attachment.getLinkUrl()
        );
    }

    private DeletedNoticeListResponse toDeletedResponse(NoticeBoard board) {
        return new DeletedNoticeListResponse(
                board.getArticleId(),
                board.getArticleNumber(),
                board.getCategory(),
                board.getTitle(),
                board.getPostedByName(),
                board.getDeletedAt(),
                board.getViewCount(),
                board.isPinned()
        );
    }
}
