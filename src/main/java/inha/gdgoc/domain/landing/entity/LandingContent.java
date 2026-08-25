package inha.gdgoc.domain.landing.entity;

import inha.gdgoc.domain.landing.enums.LandingContentStatus;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 랜딩 콘텐츠 한 판. 상태마다 한 행이다(UNIQUE). */
@Entity
@Table(name = "landing_content")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LandingContent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, unique = true)
    private LandingContentStatus status;

    /** 검증을 통과한 문서를 JSON 문자열로 담는다. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    public static LandingContent create(
        LandingContentStatus status, String content, Long updatedBy) {
        LandingContent row = new LandingContent();
        row.status = status;
        row.apply(content, updatedBy);
        return row;
    }

    public void apply(String content, Long updatedBy) {
        this.content = content;
        this.updatedBy = updatedBy;
    }
}
