package inha.gdgoc.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Duration;
import java.time.LocalDateTime;


@Entity
public class AuthCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    protected AuthCode() {}

    public AuthCode(String email, String code) {
        this.email = email;
        this.code = code;
        this.issuedAt = LocalDateTime.now();
    }

    public boolean isExpired(Duration expiration) {
        return issuedAt.plus(expiration).isBefore(LocalDateTime.now());
    }

    public boolean matches(String inputCode) {
        return code.equals(inputCode);
    }
}

