package inha.gdgoc.domain.study.entity;

import inha.gdgoc.domain.study.enums.AttendeeStatus;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StudyAttendee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AttendeeStatus status;

    @Column(columnDefinition = "TEXT")
    private String introduce;

    @Column(name = "activity_time", length = 100)
    private String activityTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id")
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public static StudyAttendee create(
            AttendeeStatus status,
            String introduce,
            String activityTime,
            Study study,
            User user
    ) {
        StudyAttendee studyAttendee = new StudyAttendee();
        studyAttendee.status = status;
        studyAttendee.introduce = introduce;
        studyAttendee.activityTime = activityTime;
        studyAttendee.study = study;
        studyAttendee.setUser(user);
        return studyAttendee;
    }

    public void setStatus(AttendeeStatus status) {
        this.status = status;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null && !user.getStudies().contains(this)) {
            user.addStudyAttendee(this);
        }
    }
}