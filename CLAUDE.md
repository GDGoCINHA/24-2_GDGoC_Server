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
- CRITICAL: 스키마 변경은 **반드시 Flyway 마이그레이션**으로 한다. `ddl-auto`에 의존하지 않는다. 파일명 규칙과 수정 금지 사유는 `db/migration`을 건드릴 때 자동 로드된다.
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

### 머지가 곧 배포다

승인 단계나 수동 트리거가 없다.

| 브랜치 | 결과 | 서버 |
|---|---|---|
| `develop` push | CD-DEV → 개발 서버 자동 배포 | `https://dev-api.gdgocinha.com` |
| `main` push | **CD-PROD → 운영 서버 자동 배포** | `https://api.gdgocinha.com` |

- 배포는 `docker-compose down` → `up` 방식이라 **다운타임이 발생**한다 (블루/그린 아님).
- **Flyway 마이그레이션은 되돌릴 수 없다.** down 스크립트가 없어 코드를 롤백해도 스키마는 남는다.
- 롤백은 이전 커밋을 `main`에 다시 push하는 것이 사실상 유일한 수단이다 (`deploy.prod.sh`가 `:latest`만 pull).

### Actions 초록불은 배포 반영이 아니다

배포 워크플로우의 마지막 스텝은 `aws deploy create-deployment`이고 **`wait`이 없다.** CodeDeploy에 *요청*만 하면 워크플로우는 즉시 성공으로 끝난다. 따라서 **초록불이어도 서버에는 이전 버전이 떠 있을 수 있고, CodeDeploy가 실패해도 알 수 없다.**

```bash
# 반영 확인 (공개 GET이므로 인증 불필요). 200이면 반영 완료, 401이면 구버전이다.
curl -s -o /dev/null -w "%{http_code}\n" \
  "https://dev-api.gdgocinha.com/api/v1/board/events?page=0&size=1"
```

---

나머지 함정은 해당 파일을 건드릴 때 자동으로 로드된다 — `.claude/rules/` 참조.
배포 실패 진단과 서버 대응(디스크·CodeDeploy·SSM)은 `gdgoc-ops` 스킬이 다룬다.

## 관련 문서

- `docs/ARCHITECTURE.md` — 새 도메인을 추가하거나 레이어 경계가 헷갈릴 때
- `docs/ADR.md` — 기존 설계를 바꾸려 할 때. "왜 이렇게 됐나"의 답
- `.claude/README.md` — 훅이 안 뜨거나 이상하게 동작할 때
- `.claude/HACKING.md` — 훅을 고치거나 부모 폴더에 설치할 때

## 작업 산출물을 어디에 쓰나

조사 메모·중간 산출물의 기본 목적지는 **`.claude/work/<과제>/`** 다. 커밋되지 않는다. 과제 이름은 브랜치에서 나온다(`feature/eventboard` → `eventboard`).

레포에 남기는 건 **작업이 끝난 뒤에 쓴 최종본**뿐이고, 최종본은 근거·판정 사유·재생성 방법을 담아야 한다. 자세한 판정 기준은 `.claude/rules/artifact-lifecycle.md`가 정본이며, 해당 경로를 건드릴 때 자동으로 로드된다.
