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

| 브랜치 | 결과 | 서버 |
|---|---|---|
| `develop` push | CD-DEV → 개발 서버 자동 배포 | `https://dev-api.gdgocinha.com` |
| `main` push | **CD-PROD → 운영 서버 자동 배포** | `https://api.gdgocinha.com` |

- 배포는 `docker-compose down` → `up` 방식이라 **다운타임이 발생**한다 (블루/그린 아님).
- **Flyway 마이그레이션은 되돌릴 수 없다.** down 스크립트가 없어 코드를 롤백해도 스키마는 남는다.
- 롤백은 이전 커밋을 `main`에 다시 push하는 것이 사실상 유일한 수단이다 (`deploy.prod.sh`가 `:latest`만 pull).

### 배포 워크플로우의 초록불은 "요청 성공"일 뿐이다

`deploy-dev.yml`·`deploy-prod.yml`의 마지막 스텝은 `aws deploy create-deployment`이며, **`wait`이 없다.** CodeDeploy에 배포를 *요청*하면 워크플로우는 즉시 성공으로 끝난다.

따라서 **Actions가 초록불이어도 서버에는 이전 버전이 떠 있을 수 있고, CodeDeploy가 실패해도 알 수 없다.** 실제 반영까지는 `docker pull` → 컨테이너 기동 → Flyway 마이그레이션 순으로 수 분이 걸린다.

**배포 후에는 반드시 실제 엔드포인트로 확인하라.**

```bash
# 반영 확인 (공개 GET이므로 인증 불필요)
curl -s -o /dev/null -w "%{http_code}\n" \
  "https://dev-api.gdgocinha.com/api/v1/board/events?page=0&size=1"
```

`200`이면 반영 완료. `401`이면 아직 구버전이다 — 구버전에는 해당 경로가 `permitAll` 목록에 없어 인증을 요구하기 때문이다.

**구버전 판별 요령**: 대조군으로 예전부터 공개였던 경로를 함께 호출한다. `/api/v1/auth/login`이 `400`(시큐리티 통과)인데 확인 대상만 `401`이라면, 시큐리티는 정상 동작하는데 그 경로가 아직 없는 것 — 즉 배포 미반영이다.

### 개발 서버는 디스크 여유가 거의 없다

dev 인스턴스(t2.micro)의 루트 볼륨은 6.8G인데 OS 2.5G + `/var` 2.0G + 스왑 2.0G로 이미 6.4G를 쓴다. 여유는 500~800MB 수준이다.

**디스크가 차면 배포가 스스로를 구제하지 못한다.** 정리 코드는 `AfterInstall`의 `deploy.dev.sh` 안에 있는데, 디스크가 차면 그 앞 단계인 `ApplicationStop`에서 죽어 도달하지 못한다. 이후 모든 단계가 `Skipped`로 남고 구버전이 계속 응답한다.

2026-08-04에 실제로 발생했다. 더 나쁜 건 **SSM 에이전트도 같이 죽어서**(`PingStatus`는 `Online`인데 명령이 0초 만에 빈 출력으로 실패) 원격 진단조차 막혔고, 콘솔에서 EC2 Instance Connect로 직접 붙어야 했다. 이때 공간을 만든 건 `apt-get clean`(330M)과 `journalctl --vacuum-size`(35M)였다 — `docker system prune`은 0B였다.

### 인증 경로 설정에 와일드카드를 쓰지 마라

`SecurityConfig`에서 `/api/v1/xxx/*` 같은 패턴은 의도치 않은 하위 경로까지 공개한다. 실제로 `/api/v1/board/events/*`가 관리자용 `/deleted`까지 permitAll 처리한 사례가 있었다. **공개할 경로를 개별 명시**하고, 경로 변수는 `{id:[0-9]+}`처럼 제약을 건다.

메서드 단위 인가(`@Authorize` AOP 또는 `@PreAuthorize`)가 막아주더라도, 시큐리티 계층에서 열어두면 401이어야 할 응답이 403으로 나가고 방어가 단일 지점에 의존하게 된다.

## 관련 문서

- `docs/ARCHITECTURE.md` — 디렉터리 구조, 레이어, 인증 흐름
- `docs/ADR.md` — 주요 설계 결정과 트레이드오프
- `.claude/README.md` — 하네스(훅·규칙·작업 공간). 훅이 안 뜰 때의 진단도 여기 있다

## 작업 산출물을 어디에 쓰나

조사 메모·중간 산출물의 기본 목적지는 **`.claude/work/<과제>/`** 다. 커밋되지 않는다. 과제 이름은 브랜치에서 나온다(`feature/eventboard` → `eventboard`).

레포에 남기는 건 **작업이 끝난 뒤에 쓴 최종본**뿐이고, 최종본은 근거·판정 사유·재생성 방법을 담아야 한다. 자세한 판정 기준은 `.claude/rules/artifact-lifecycle.md`가 정본이며, 해당 경로를 건드릴 때 자동으로 로드된다.
