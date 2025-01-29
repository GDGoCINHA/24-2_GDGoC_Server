package inha.gdgoc.domain.user.entity;

import inha.gdgoc.domain.user.enums.CoreType;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "interest", nullable = false)
    private Interest interest;

    @Column(name = "interest_etc")
    private String interestEtc;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "core_type", nullable = false)
    private CoreType coreType;

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

    //TODO create 값 추가해야함, 테스트삼아 몇개 인자만 넣음
    public static User create(String salt, CoreType coreType, String googleId, String password, String email, String interestEtc, Interest interest, String major, String name) {
        User user = new User();
        user.name = name;
        user.major = major;
        user.interest = interest;
        user.interestEtc = interestEtc;
        user.email = email;
        user.password = password;
        user.googleId = googleId;
        user.coreType = coreType;
        user.salt = salt;
        return user;
    }
}
