# Architecture Decision Records

> **이 문서의 성격**
>
> 기존 코드베이스에서 **관찰된 결정을 역으로 정리**한 것이다. `결정`과 `트레이드오프`는 코드에서 확인한 사실이지만, `이유` 중 `[추정]` 표시가 붙은 항목은 당시 논의 기록이 없어 추론한 것이다. **당사자가 확인 후 수정하거나 확정 표시로 바꿔달라.**

## 철학

관찰된 경향:

- 도메인별 수직 분할. 공통 인프라는 `global/`에 모으고 도메인은 서로 직접 참조하지 않는다.
- 응답·예외·감사 필드를 전역 규약으로 통일한다 (`ApiResponse`, `ErrorCode`, `BaseEntity`).
- 스키마는 코드가 아니라 마이그레이션이 소유한다.

---

### ADR-001: QueryDSL은 openfeign fork를 사용

**결정**: `io.github.openfeign.querydsl:querydsl-jpa`를 사용한다. 원본 `com.querydsl`이 아니다.

**이유**: 원본 QueryDSL이 Jakarta EE(`jakarta.persistence`) 전환에 대응하지 않아 Spring Boot 3.x에서 그대로 쓸 수 없다. openfeign fork가 이를 지원한다.

**트레이드오프**: 업스트림이 아닌 fork에 의존한다. 원본 QueryDSL 문서·예제와 좌표가 달라 혼동이 생길 수 있다.

---

### ADR-002: 스키마 변경은 Flyway로만, 파일명은 날짜 기반

**결정**: `V{YYYYMMDD}__{설명}.sql` 형식으로 마이그레이션을 작성한다. 운영·개발 프로파일에서 `flyway.enabled: true`, `baseline-on-migrate: true`, `clean-disabled: true`.

**이유**: 스키마 이력을 코드와 함께 버전 관리하기 위함. 날짜 기반 네이밍은 [추정] 여러 명이 동시에 브랜치를 파도 버전 번호가 충돌하지 않게 하려는 의도로 보인다.

**트레이드오프**:
- 날짜 기반이라 **머지 순서와 버전 순서가 어긋날 수 있다.** 늦게 만든 마이그레이션이 먼저 머지되면 out-of-order 상황이 발생한다.
- **down 스크립트가 없다.** 롤백 시 스키마는 남는다.
- 테스트는 Flyway를 끄고 `ddl-auto: create-drop`을 쓰므로 **마이그레이션 SQL 자체는 테스트로 검증되지 않는다.**

---

### ADR-003: 권한 검사는 AccessGuard로 중앙화, 호출 방식은 두 가지

**결정**: `global/security/AccessGuard`가 역할·팀 조건 판정을 단독으로 책임진다. 호출 진입점은 둘이다.

- `check(...)` → `@PreAuthorize("@accessGuard.check(...)")` SpEL에서 사용, boolean 반환
- `require(...)` → `@Authorize` AOP 및 서비스에서 직접 호출, 실패 시 예외

**이유**: 역할 비교 로직이 여러 도메인에 흩어지는 것을 막기 위함. 서열 비교를 `UserRole.rank()` 한 곳에 두어 enum 순서 변경에 영향받지 않게 했다.

**트레이드오프**: 진입점이 둘이라 **도메인마다 다른 방식을 쓸 수 있다.** 실제로 행사 게시판은 `@Authorize`, 공지 게시판은 `@PreAuthorize`를 쓴다. 판정 로직은 동일하므로 기능상 충돌은 없으나, 신규 코드가 어느 쪽을 따라야 하는지 불명확하다. **팀 합의가 필요한 항목.**

---

### ADR-004: 게시판은 소프트 딜리트

**결정**: `deleted_at` 컬럼으로 논리 삭제한다. 저장소의 `findById`는 `findByIdAndDeletedAtIsNull`로 위임하고, 삭제분 조회는 `findDeletedById`로 분리한다.

**이유**: 실수로 삭제한 게시글을 복구하기 위함 (`POST /{id}/restore`).

**트레이드오프**:
- 모든 조회 경로에 `deleted_at IS NULL` 조건이 필요하다. 저장소 인터페이스에서 기본 동작으로 감싸 실수를 줄였지만, QueryDSL 직접 조회 시에는 조건을 빠뜨릴 수 있다.
- 첨부파일 등 연관 데이터는 함께 정리되지 않고 남는다.

---

### ADR-005: 인증은 JWT + Redis

**결정**: 액세스 토큰은 JWT(jjwt)로 발급하고 리프레시 토큰 등 상태는 Redis에 둔다. 세션은 `STATELESS`.

**이유**: [추정] 서버 확장 시 세션 공유 문제를 피하고, 토큰 무효화가 필요한 부분만 Redis로 처리하려는 절충으로 보인다.

**트레이드오프**: Redis 장애가 인증 흐름에 영향을 준다. 액세스 토큰 자체는 만료 전까지 무효화할 수 없다.

---

### ADR-006: 배포는 Docker 이미지 + CodeDeploy, 브랜치 push가 트리거

**결정**: `develop` push → 개발 서버, `main` push → 운영 서버로 자동 배포한다. GitHub Actions가 이미지를 빌드해 Docker Hub에 올리고(`:latest` + `:{sha}`), S3를 거쳐 CodeDeploy가 EC2에서 `deploy.prod.sh`를 실행한다.

**이유**: [추정] 별도 배포 인프라 없이 GitHub Actions만으로 파이프라인을 구성하려는 선택으로 보인다.

**트레이드오프**:
- **승인 단계가 없다.** 머지가 곧 배포다.
- `docker-compose down` → `up` 방식이라 **다운타임이 발생**한다.
- `deploy.prod.sh`가 `:latest`만 pull하므로, Docker Hub에 SHA 태그가 남아있어도 **롤백은 이전 커밋을 다시 push하는 방식**에 의존한다.

---

### ADR-007: 응답 포맷 통일

**결정**: 모든 엔드포인트가 `ApiResponse<T, M>(code, message, data, meta)`를 반환한다. `null` 필드는 직렬화에서 제외한다.

**이유**: 프론트엔드가 응답 구조를 일관되게 처리하도록 하기 위함.

**트레이드오프**: HTTP 상태 코드와 본문의 `code`가 중복된다. 제네릭 파라미터가 둘이라 `meta`가 없는 경우에도 `Void`를 명시해야 한다.
