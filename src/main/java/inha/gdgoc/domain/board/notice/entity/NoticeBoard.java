package inha.gdgoc.domain.board.notice.entity;

import inha.gdgoc.domain.board.notice.enums.ArticleStatusEnum;
import inha.gdgoc.domain.board.notice.enums.AttachmentTypeEnum;
import inha.gdgoc.domain.board.notice.enums.CategoryEnum;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notice_board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeBoard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "article_number", nullable = false, insertable = false, updatable = false)
    private Long articleNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32)
    private CategoryEnum category;

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ArticleStatusEnum status;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "posted_by", nullable = false)
    private Long postedBy;

    @Column(name = "posted_by_name", nullable = false, length = 100)
    private String postedByName;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "noticeBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<NoticeBoardAttachment> attachments = new ArrayList<>();

    public static NoticeBoard create(
            String title, CategoryEnum category, String content,
            boolean isPinned, ArticleStatusEnum status,
            Long postedBy, String postedByName) {
        NoticeBoard board = new NoticeBoard();
        board.title = title;
        board.category = category;
        board.content = content;
        board.isPinned = isPinned;
        board.status = status;
        board.postedBy = postedBy;
        board.postedByName = postedByName;
        board.viewCount = 0;
        return board;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void softDelete() {
        this.status = ArticleStatusEnum.DELETED;
        this.deletedAt = Instant.now();
    }

    public void restore() {
        this.status = ArticleStatusEnum.PUBLISHED;
        this.deletedAt = null;
    }

    public void update(String title, CategoryEnum category, String content,
                       Boolean isPinned, ArticleStatusEnum status) {
        if (title != null) this.title = title;
        if (category != null) this.category = category;
        if (content != null) this.content = content;
        if (isPinned != null) this.isPinned = isPinned;
        if (status != null) this.status = status;
    }

    public void addAttachment(AttachmentTypeEnum type, String originalName,
                              String storedName, String fileUrl, Long fileSize,
                              String mimeType, String linkUrl, int displayOrder) {
        this.attachments.add(NoticeBoardAttachment.create(
                this, type, originalName, storedName, fileUrl, fileSize,
                mimeType, linkUrl, displayOrder));
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
