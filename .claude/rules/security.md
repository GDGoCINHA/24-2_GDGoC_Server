---
paths:
  - "**/SecurityConfig.java"
  - "**/global/security/**"
---

# 인증·인가를 건드릴 때

## 경로 설정에 와일드카드를 쓰지 마라

`SecurityConfig`에서 `/api/v1/xxx/*` 같은 패턴은 의도치 않은 하위 경로까지 공개한다. 실제로 `/api/v1/board/events/*`가 관리자용 `/deleted`까지 permitAll 처리한 사례가 있었다. **공개할 경로를 개별 명시**하고, 경로 변수는 `{id:[0-9]+}`처럼 제약을 건다.

메서드 단위 인가(`@Authorize` AOP 또는 `@PreAuthorize`)가 막아주더라도, 시큐리티 계층에서 열어두면 401이어야 할 응답이 403으로 나가고 방어가 단일 지점에 의존하게 된다.

## 권한 검사는 `AccessGuard`를 쓴다

권한 검사는 `global/security/AccessGuard`를 사용한다. 직접 role을 비교하는 코드를 새로 만들지 않는다.

## 권한·가시성 로직을 고치면 테스트를 추가한다

회귀 시 보안 사고로 이어진다. 이 규칙에는 예외를 두지 않는다.
