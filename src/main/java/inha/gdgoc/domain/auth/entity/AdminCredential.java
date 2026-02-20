package inha.gdgoc.domain.auth.entity;

import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "admin_credentials")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminCredential extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, unique = true, length = 100)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Builder
    public AdminCredential(
            String loginId,
            String passwordHash,
            boolean enabled
    ) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
