package inha.gdgoc.domain.board.notice.entity;

import inha.gdgoc.domain.board.notice.enums.AttachmentTypeEnum;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "notice_board_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeBoardAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attachment_id", nullable = false, updatable = false)
    private UUID attachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private NoticeBoard noticeBoard;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 32)
    private AttachmentTypeEnum attachmentType;

    // FILE 전용 필드
    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "stored_name", length = 255)
    private String storedName;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    // URL 전용 필드
    @Column(name = "link_url", columnDefinition = "TEXT")
    private String linkUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    // 패키지 프라이빗 팩터리 메서드
    static NoticeBoardAttachment create(
            NoticeBoard noticeBoard, AttachmentTypeEnum type,
            String originalName, String storedName, String fileUrl,
            Long fileSize, String mimeType, String linkUrl, int displayOrder) {
        NoticeBoardAttachment attachment = new NoticeBoardAttachment();
        attachment.noticeBoard = noticeBoard;
        attachment.attachmentType = type;
        attachment.originalName = originalName;
        attachment.storedName = storedName;
        attachment.fileUrl = fileUrl;
        attachment.fileSize = fileSize;
        attachment.mimeType = mimeType;
        attachment.linkUrl = linkUrl;
        attachment.displayOrder = displayOrder;
        return attachment;
    }
}
