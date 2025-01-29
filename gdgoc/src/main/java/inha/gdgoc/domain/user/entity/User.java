package inha.gdgoc.domain.user.entity;

import inha.gdgoc.domain.user.enums.CoreType;
import inha.gdgoc.domain.user.enums.Interest;
import inha.gdgoc.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
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

    @Column(name = "interest_etc", nullable = true)
    private String interestEtc;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "google_id", nullable = true)
    private String googleId;

    @Column(name = "core_type", nullable = false)
    private CoreType coreType;

    @Column(name = "salt", nullable = false)
    private String salt;

    @Column(name = "image", nullable = true)
    private String image;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "socials")
    private SocialUrls social;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "careers")
    private Careers careers;
}
