package inha.gdgoc.domain.user.entity;

import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.enums.Interest;
import inha.gdgoc.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "major", nullable = false)
    private String major;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "user_role", nullable = false)
    private UserRole userRole;

    @Column(name = "salt", nullable = false)
    private String salt;

    @Column(name = "image")
    private String image;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "socials")
    private SocialUrls social;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "careers")
    private Careers careers;

    @Builder
    public User(
            String name, String major, String studentId, String phoneNumber,
            String email, String password, String googleId, UserRole userRole,
            String salt, String image, SocialUrls social, Careers careers) {
        this.name = name;
        this.major = major;
        this.studentId = studentId;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.password = password;
        this.googleId = googleId;
        this.userRole = userRole;
        this.salt = salt;
        this.image = image;
        this.social = social != null ? social : new SocialUrls();
        this.careers = careers != null ? careers : new Careers();
    }

}
