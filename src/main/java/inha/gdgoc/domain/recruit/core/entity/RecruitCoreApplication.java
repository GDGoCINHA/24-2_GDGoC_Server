package inha.gdgoc.domain.recruit.core.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "core_recruit_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecruitCoreApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session", nullable = false, length = 32)
    private String session;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "major", nullable = false)
    private String major;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "team", nullable = false)
    private String team;

    @Column(name = "motivation", nullable = false, columnDefinition = "text")
    private String motivation;

    @Column(name = "wish", nullable = false, columnDefinition = "text")
    private String wish;

    @Column(name = "strengths", nullable = false, columnDefinition = "text")
    private String strengths;

    @Column(name = "pledge", nullable = false, columnDefinition = "text")
    private String pledge;

    @Type(JsonType.class)
    @Column(name = "file_urls", nullable = false, columnDefinition = "jsonb")
    private List<String> fileUrls;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 32)
    private RecruitCoreResultStatus resultStatus = RecruitCoreResultStatus.SUBMITTED;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "result_note", columnDefinition = "text")
    private String resultNote;

    public Long getId() {
        return id;
    }

    public boolean isOwnedBy(Long userId) {
        return userId != null && user != null && userId.equals(user.getId());
    }

    public void accept(Long reviewerId, String note, Instant reviewedAt) {
        this.resultStatus = RecruitCoreResultStatus.ACCEPTED;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewerId;
        this.resultNote = note;
    }

    public void reject(Long reviewerId, String note, Instant reviewedAt) {
        this.resultStatus = RecruitCoreResultStatus.REJECTED;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewerId;
        this.resultNote = note;
    }

    public void moveToReview(Long reviewerId, Instant reviewedAt) {
        this.resultStatus = RecruitCoreResultStatus.IN_REVIEW;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewerId;
    }
}
