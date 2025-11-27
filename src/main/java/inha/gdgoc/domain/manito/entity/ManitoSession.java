package inha.gdgoc.domain.manito.entity;

import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "manito_sessions", indexes = {@Index(name = "idx_manito_sessions_created_at", columnList = "created_at DESC")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManitoSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 세션 코드 (예: gmg-2025, manito-2025-fall)
     */
    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    /**
     * 사람이 읽기 좋은 제목
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Builder
    private ManitoSession(String code, String title) {
        this.code = code;
        this.title = title;
    }

    public void changeTitle(String title) {
        this.title = title;
    }
}