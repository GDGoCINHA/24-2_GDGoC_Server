package inha.gdgoc.domain.landing.enums;

/**
 * 랜딩 콘텐츠의 두 판.
 *
 * <p>관리자는 DRAFT 를 고치다가 발행할 때 PUBLISHED 로 옮긴다. 방문자는 PUBLISHED 만 본다 — 편집
 * 중인 문구가 첫 화면에 그대로 나가지 않게 하려는 것이다.
 */
public enum LandingContentStatus {
    DRAFT,
    PUBLISHED
}
