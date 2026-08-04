# 아키텍처

## 디렉터리 구조

```
src/main/java/inha/gdgoc/
├── domain/              # 도메인별 수직 분할
│   ├── admin/           # 관리자 기능
│   ├── auth/            # 인증·로그인
│   ├── board/           # 게시판
│   ├── core/            # 코어 멤버
│   ├── game/            # 이벤트성 게임
│   ├── guestbook/       # 방명록
│   ├── manito/          # 마니또
│   ├── recruit/         # 모집 (core / member)
│   ├── resource/        # S3 파일 업로드·다운로드
│   ├── study/           # 스터디
│   └── user/            # 사용자
└── global/              # 도메인 무관 공통 인프라
    ├── config/          # jpa, jwt, openapi, querydsl, s3
    ├── dto/             # ApiResponse, PageMeta, ErrorMeta
    ├── entity/          # BaseEntity
    ├── exception/       # BusinessException, ErrorCode
    ├── security/        # SecurityConfig, AccessGuard, TokenAuthenticationFilter
    └── util/
```

각 도메인은 동일한 내부 구조를 갖는다:

```
domain/{도메인}/
├── controller/          # HTTP 엔드포인트 (+ message/ 응답 메시지 상수)
├── dto/request/         # 요청 DTO (record + Bean Validation)
├── dto/response/        # 응답 DTO (record)
├── entity/              # JPA 엔티티
├── enums/
├── exception/           # 도메인 전용 ErrorCode
├── repository/          # JpaRepository + QueryDSL Impl
└── service/             # 비즈니스 로직·트랜잭션 경계
```

`src/main/resources/db/migration/` 에 Flyway 마이그레이션이 위치한다.

## 레이어 패턴

```
Controller  →  Service  →  Repository  →  DB
   │             │            │
   │             │            └── JpaRepository(단순 조회) + QueryDslRepository(동적 쿼리)
   │             └── @Transactional 경계. 엔티티 → DTO 변환
   └── 요청 검증(@Valid), 인가(@Authorize), ApiResponse 래핑
```

**규칙**

- 컨트롤러는 엔티티를 반환하지 않는다. 서비스가 DTO로 변환해 넘긴다.
- 트랜잭션은 서비스에서 연다. 클래스에 `@Transactional(readOnly = true)`를 걸고 쓰기 메서드만 `@Transactional`로 덮는 방식을 쓴다.
- 동적 쿼리는 `{Entity}QueryDslRepository` 인터페이스로 선언하고 `{Entity}RepositoryImpl`에서 구현한다. 서비스는 통합 인터페이스(`{Entity}Repository`)에만 의존한다.

## 인증·인가 흐름

```
Request
  │
  ├─ TokenAuthenticationFilter          JWT 파싱 → CustomUserDetails → SecurityContext
  │    (shouldNotFilter 목록은 스킵)
  │
  ├─ SecurityConfig                     경로 단위 인가 (permitAll / authenticated)
  │
  ├─ @Authorize (AuthorizeAspect)       메서드 단위 인가 → AccessGuard.require()
  │  또는 @PreAuthorize("@accessGuard.check(...)")
  │
  └─ Service                            도메인 권한 (예: 작성팀 소유권 검사)
```

**3중 구조인 이유**

- `SecurityConfig` — 경로 자체가 공개인지 여부. 여기서 열면 인증 정보 없이도 통과하므로 신중히 다룬다.
- `@Authorize` / `@PreAuthorize` — 역할(role)·팀 기반 정적 조건. 둘 다 내부적으로 `AccessGuard`를 호출한다.
- 서비스 계층 — "작성한 팀만 수정 가능" 같이 **데이터를 조회해야 판단 가능한** 조건.

`AccessGuard`는 두 진입점을 제공한다.

| 메서드 | 용도 | 실패 시 |
|---|---|---|
| `check(...)` | SpEL(`@PreAuthorize`)에서 호출 | `false` 반환 |
| `require(...)` | AOP·서비스에서 직접 호출 | `AccessDeniedException` |

역할 서열은 `UserRole.rank()`가 정한다: `GUEST(0) < MEMBER(1) < CORE(2) < LEAD(3) < ORGANIZER(4) < ADMIN(5)`. 비교는 항상 `UserRole.hasAtLeast(me, required)`를 쓴다.

## 공통 규약

**응답** — 모든 엔드포인트는 `ApiResponse<T, M>`를 반환한다.

```java
record ApiResponse<T, M>(int code, String message, T data, M meta)
```

`meta`에는 페이징 정보(`PageMeta`)나 에러 컨텍스트(`ErrorMeta`)가 들어간다. `null` 필드는 직렬화에서 제외된다.

**예외** — `BusinessException(ErrorCode)` 하나로 던지고, HTTP 상태는 `ErrorCode`가 보유한다. 전역 핸들러가 `ErrorCode.getStatus()`로 응답 코드를 정한다.

| 코드 | 상태 |
|---|---|
| `UNAUTHORIZED_USER` | 401 |
| `FORBIDDEN_USER` | 403 |
| `RESOURCE_NOT_FOUND` | 404 |

리소스의 **존재 자체를 숨겨야 할 때**는 403이 아니라 404를 쓴다 (권한 없는 사용자에게 존재 여부가 새지 않도록).

**감사 필드** — 엔티티는 `BaseEntity`를 상속해 `created_at`·`updated_at`을 JPA Auditing으로 자동 관리한다.

## 데이터 흐름 (조회 예시)

```
GET /api/v1/board/events?page=0&size=12
  → Controller: @AuthenticationPrincipal로 사용자 컨텍스트 추출
  → Service: 권한에 따른 가시성 조건 결정
  → RepositoryImpl: QueryDSL 동적 where + count
  → Service: Entity → DTO 변환 (S3 key → URL 치환)
  → ApiResponse + PageMeta
```

## 환경 프로파일

| 프로파일 | DB | Flyway | 용도 |
|---|---|---|---|
| `local` | 로컬 PostgreSQL (docker-compose-local.yml) | enabled | 개발 |
| `test` | H2 (PostgreSQL 모드) | **disabled** (`ddl-auto: create-drop`) | 테스트 |
| `dev` | 개발 서버 PostgreSQL | enabled | develop 브랜치 배포 |
| `prod` | 운영 PostgreSQL | enabled | main 브랜치 배포 |

테스트는 Flyway를 끄고 엔티티에서 스키마를 생성한다. **따라서 마이그레이션 SQL의 오류는 테스트로 잡히지 않는다.**
