# 프로젝트: GDGoC INHA 백엔드

인하대학교 GDGoC 공식 웹사이트의 백엔드 API 서버. 프론트엔드는 별도 리포(`GDGoCINHA/24-2_GDGoC_Web`, Next.js)에서 관리한다.

## 기술 스택

- Java 21 / Spring Boot 3.5.9
- Spring Data JPA + QueryDSL (`io.github.openfeign.querydsl`)
- PostgreSQL + Flyway
- Spring Security + JWT(jjwt) + OAuth2(Google)
- Redis (토큰 저장)
- AWS S3 (`spring-cloud-aws`)
- springdoc-openapi (Swagger UI)

## 아키텍처 규칙

- CRITICAL: 도메인은 `domain/{도메인}/` 아래 `controller`·`service`·`repository`·`entity`·`dto`·`enums`로 분리한다. 도메인 간 직접 참조 대신 서비스 계층을 경유한다.
- CRITICAL: 모든 응답은 `ApiResponse<T, M>`로 감싼다. 컨트롤러가 엔티티를 그대로 반환하지 않는다 — 반드시 DTO(record)로 변환한다.
- CRITICAL: 스키마 변경은 **반드시 Flyway 마이그레이션**으로 한다. `ddl-auto`에 의존하지 않는다. 파일명은 `V{YYYYMMDD}__{설명}.sql`이며 **기존 파일을 수정하지 않는다**(체크섬 불일치로 부팅 실패).
- CRITICAL: 권한 검사는 `global/security/AccessGuard`를 사용한다. 직접 role을 비교하는 코드를 새로 만들지 않는다.
- 엔티티는 `BaseEntity`를 상속해 `created_at`·`updated_at`을 자동 관리한다.
- 예외는 `BusinessException` + `ErrorCode`로 던진다. HTTP 상태는 `ErrorCode`가 정한다.
- QueryDSL 동적 쿼리는 `{Entity}RepositoryImpl`에 두고 인터페이스로 노출한다.

## 개발 프로세스

- CRITICAL: 버그 수정 시 **재현 테스트를 먼저 작성**하고, 실패를 확인한 뒤 고친다.
- CRITICAL: 권한·가시성 로직을 수정하면 반드시 테스트를 추가한다. 회귀 시 보안 사고로 이어진다.
- 커밋 메시지는 conventional commits를 따른다 (`feat:`, `fix:`, `test:`, `refactor:`, `chore:`).
- 브랜치는 `feature/{기능}` → `develop` → `main` 순으로 올린다.

## 명령어

```bash
./gradlew build -x test    # 빌드 (테스트 제외)
./gradlew test             # 테스트
./gradlew compileTestJava  # 테스트 컴파일만 확인
```

로컬 실행에는 PostgreSQL·Redis와 `.env`가 필요하다. 두 파일 모두 리포에 포함되지 않으므로 팀에서 별도로 받는다.

## ⚠️ 이 프로젝트의 함정

코드만 읽어서는 드러나지 않는 것들. **작업 전에 반드시 인지할 것.**

### CI의 초록불을 믿지 마라

`.github/workflows/ci.yml`의 테스트 스텝이 `./gradlew test | tee test.log` 형태다. 파이프 때문에 종료 코드가 `tee`의 것이 되어 **gradlew가 실패해도 CI는 성공으로 보고한다.**

실제로 2026-02-14부터 8월까지 테스트가 컴파일조차 안 되는 상태였으나 CI는 매번 "✅ Test 성공"을 찍었다. 테스트 상태는 CI 배지가 아니라 **`Test Results` 체크나 로컬 실행 결과**로 확인해야 한다.

수정하려면 스텝에 `set -o pipefail`을 추가하면 되지만, 그러면 아래 기존 실패로 모든 PR이 막힌다. **기존 실패를 먼저 정리한 뒤 게이트를 살릴 것.**

### 기존 테스트 6건이 실패 상태다

내가 만든 실패가 아닌지 먼저 확인하라. 2026-08-04 기준:

- `RecruitCoreApplicationServiceTest` 5건 — `RecruitCoreApplicationService.java`의 `RECRUITMENT_DEADLINE`이 `2026-03-14`로 하드코딩되어 `Instant.now()`와 비교한다. 마감일이 지나 실패한다. **운영에서도 코어 지원 API가 계속 차단된 상태**이며, 다음 모집 시 코드 수정·재배포가 필요하다.
- `RecruitMemberMemoNotificationServiceTest` 1건 — 알림 상태가 `SENT` 대신 `PENDING`.

### 머지가 곧 배포다

승인 단계나 수동 트리거가 없다.

| 브랜치 | 결과 |
|---|---|
| `develop` push | CD-DEV → 개발 서버 자동 배포 |
| `main` push | **CD-PROD → 운영 서버 자동 배포** |

- 배포는 `docker-compose down` → `up` 방식이라 **다운타임이 발생**한다 (블루/그린 아님).
- **Flyway 마이그레이션은 되돌릴 수 없다.** down 스크립트가 없어 코드를 롤백해도 스키마는 남는다.
- 롤백은 이전 커밋을 `main`에 다시 push하는 것이 사실상 유일한 수단이다 (`deploy.prod.sh`가 `:latest`만 pull).

### 인증 경로 설정에 와일드카드를 쓰지 마라

`SecurityConfig`에서 `/api/v1/xxx/*` 같은 패턴은 의도치 않은 하위 경로까지 공개한다. 실제로 `/api/v1/board/events/*`가 관리자용 `/deleted`까지 permitAll 처리한 사례가 있었다. **공개할 경로를 개별 명시**하고, 경로 변수는 `{id:[0-9]+}`처럼 제약을 건다.

메서드 단위 인가(`@Authorize` AOP 또는 `@PreAuthorize`)가 막아주더라도, 시큐리티 계층에서 열어두면 401이어야 할 응답이 403으로 나가고 방어가 단일 지점에 의존하게 된다.

## 관련 문서

- `docs/ARCHITECTURE.md` — 디렉터리 구조, 레이어, 인증 흐름
- `docs/ADR.md` — 주요 설계 결정과 트레이드오프
