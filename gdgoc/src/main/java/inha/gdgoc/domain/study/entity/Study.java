package inha.gdgoc.domain.study.entity;

import inha.gdgoc.domain.study.enums.CreaterType;
import inha.gdgoc.domain.study.enums.StudyStatus;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.common.BaseEntity;
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
    private CreaterType createrType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StudyStatus status;

    @Column(name = "recruit_start_date")
    private LocalDateTime recruitStartDate;

    @Column(name = "recruit_end_date")
    private LocalDateTime recruitEndDate;

    @Column(name = "activity_start_date")
    private LocalDateTime activityStartDate;

    @Column(name = "activity_end_date")
    private LocalDateTime activityEndDate;

    @Column(name = "expected_time", length = 100)
    private String expectedTime;

    @Column(name = "expected_place", length = 100)
    private String expectedPlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyAttendee> studyAttendees = new ArrayList<>();
}
