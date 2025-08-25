package inha.gdgoc.domain.study.entity;

import inha.gdgoc.domain.study.enums.CreatorType;
import inha.gdgoc.domain.study.enums.StudyStatus;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Study extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 256, nullable = false)
    private String title;

    @Column(name = "simple_introduce", length = 512)
    private String simpleIntroduce;

    @Column(name = "activity_introduce", columnDefinition = "TEXT")
    private String activityIntroduce;

    @Column(name = "image_path", length = 256)
    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private CreatorType creatorType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StudyStatus status;

    @Column(name = "expected_time", length = 100)
    private String expectedTime;

    @Column(name = "expected_place", length = 100)
    private String expectedPlace;

    @Column(name = "recruit_start_date")
    private LocalDateTime recruitStartDate;

    @Column(name = "recruit_end_date")
    private LocalDateTime recruitEndDate;

    @Column(name = "activity_start_date")
    private LocalDateTime activityStartDate;

    @Column(name = "activity_end_date")
    private LocalDateTime activityEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyAttendee> studyAttendees = new ArrayList<>();


    public static Study create(
            String title,
            String simpleIntroduce,
            String activityIntroduce,
            String imagePath,
            CreatorType creatorType,
            StudyStatus status,
            String expectedTime,
            String expectedPlace,
            LocalDateTime recruitStartDate,
            LocalDateTime recruitEndDate,
            LocalDateTime activityStartDate,
            LocalDateTime activityEndDate,
            User user
    ) {
        Study study = new Study();
        study.title = title;
        study.simpleIntroduce = simpleIntroduce;
        study.activityIntroduce = activityIntroduce;
        study.imagePath = imagePath;
        study.creatorType = creatorType;
        study.status = status;
        study.expectedTime = expectedTime;
        study.expectedPlace = expectedPlace;
        study.recruitStartDate = recruitStartDate;
        study.recruitEndDate = recruitEndDate;
        study.activityStartDate = activityStartDate;
        study.activityEndDate = activityEndDate;
        study.setUser(user);
        study.studyAttendees = new ArrayList<>();
        return study;
    }

    public boolean isCreatedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null && !user.getStudies().contains(this)) {
            user.addStudy(this);
        }
    }
}
