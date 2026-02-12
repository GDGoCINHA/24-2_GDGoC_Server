package inha.gdgoc.domain.user.entity;

import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.study.entity.StudyAttendee;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.entity.BaseEntity;
import inha.gdgoc.global.util.EncryptUtil;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "oauth_subject", nullable = false, unique = true)
    private String oauthSubject;

    @Column(name = "major", nullable = false)
    private String major;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    @Default
    private UserRole userRole = UserRole.GUEST;

    @Enumerated(EnumType.STRING)
    @Column(name = "team")
    private TeamType team;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false)
    @Default
    private MembershipStatus membershipStatus = MembershipStatus.PENDING;

    @Column(name = "image")
    private String image;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "socials")
    private SocialUrls social;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "careers")
    private Careers careers;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Study> studies = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyAttendee> studyAttendees = new ArrayList<>();

    @Builder
    public User(
            String name, String oauthSubject, String major, String studentId, String phoneNumber,
            String email, UserRole userRole,
            TeamType team,
            String image, SocialUrls social, Careers careers
    ) {
        this.oauthSubject = oauthSubject;
        this.name = name;
        this.major = major;
        this.studentId = studentId;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.userRole = userRole;
        this.team = team;
        this.image = image;
        this.social = (social != null ? social : new SocialUrls());
        this.careers = (careers != null ? careers : new Careers());
    }

    public void addStudy(Study study) {
        this.studies.add(study);
        if (study != null && study.getUser() != this) {
            study.setUser(this);
        }
    }

    public void addStudyAttendee(StudyAttendee studyAttendee) {
        this.studyAttendees.add(studyAttendee);
        if (studyAttendee != null && studyAttendee.getUser() != this) {
            studyAttendee.setUser(this);
        }
    }

    public void approve() {
        this.membershipStatus = MembershipStatus.APPROVED;
        if (this.userRole == UserRole.GUEST) {
            this.userRole = UserRole.MEMBER;
        }
    }
    public void reject() {
        this.membershipStatus = MembershipStatus.REJECTED;
    }
    public enum MembershipStatus { PENDING, APPROVED, REJECTED }
    
    public boolean isGuest() {
        return this.userRole == UserRole.GUEST;
    }
    public void changeRole(UserRole role) { this.userRole = role; }
    public void changeTeam(TeamType team) { this.team = team; }
}
