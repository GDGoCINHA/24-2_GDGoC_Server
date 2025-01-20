package inha.gdgoc.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

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

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
