# 내 정보(프로필) 조회·수정 설계

- 작성일: 2026-08-06
- 상태: 승인됨 (구현 계획 수립 대기)
- 리포: `24-2_GDGoC_Server`(선행) + `24-2_GDGoC_Web`(후행)
- 디자인: Figma `🖥️ GDGoC TECH` → 와이어프레임 → 공식 홈페이지 → **내 정보 페이지**

## 배경

현재 프로필 조회·수정 기능이 **양쪽 리포 모두에 없다.**

- Web: `/profile`·`/mypage` 라우트 부재. `src/app/` 전체를 확인함
- Server: 조회는 `GET /api/v1/auth/me`가 있으나 반환 필드가 7개(`id, name, email, userRole, team, membershipStatus, image`)뿐이고, 수정 API는 존재하지 않음

랜딩 드로어 메뉴의 `{ label: '마이페이지', href: '/dashboard' }`(`OnboardingLanding.tsx:394`)가 CORE 이상 전용 관리자 대시보드를 가리켜, 일반 회원이 누르면 다음 경로로 튕긴다. 이번 작업이 해결하려는 증상이다.

```
마이페이지 → /dashboard → ApiCodeGuard(requiredRole="CORE") → 403
           → useAuthenticatedApi.ts:131 router.replace('/')
           → app/page.tsx:4 redirect('/onboarding')
```

## 범위

**포함**
- 프로필 조회 (프로필 카드 + 개인정보)
- 개인정보 수정 (이름·학과·전화번호)
- 프로필 이미지 변경
- 활동 및 신청 현황 — **운영진 지원서만**
- 랜딩 메뉴 링크 정리

**제외**
- 신입 부원 모집 신청서 현황. `RecruitMember` 엔티티에 `user_id` 연관이 없고(학번·이메일·전화번호로만 식별) **심사 상태 컬럼 자체가 없다**(`isPayed`뿐). 구현하려면 스키마 신설 + User 매칭 규칙 수립이 필요해 별도 과제로 분리한다
- 프로필 이미지 삭제 (디자인에 없음)
- `SocialUrls`·`Careers` 노출 (디자인에 없음)

## 디자인 ↔ 서버 enum 대조

| 디자인 | 서버 | 결과 |
|---|---|---|
| GUEST/MEMBER/CORE/LEAD/ORGANIZER/ADMIN | `UserRole` 동일 | 일치 |
| SUBMITTED/IN-REVIEW/ACCEPTED/REJECTED | `RecruitCoreResultStatus` 동일 | 일치 |
| HQ/BD/HR/TECH/PR·DESIGN + **NONE** | `TeamType`에 앞 5개 | `NONE`은 `team == null`. 프론트에서 흡수 |

## 접근 방식 — `/users/me` 전용 리소스

`AuthUserResponse`를 확장하지 않고 별도 리소스를 신설한다.

**근거**: `AuthUserResponse`는 `/auth/me`·로그인 응답·토큰 갱신 응답·관리자 로그인 **네 곳에서 재사용**된다. Web은 로그인·갱신 응답의 `user`를 `writeStoredUser`로 **localStorage에 저장**한다. 여기에 전화번호·학번을 추가하면 개인정보가 브라우저에 상주하게 되어 노출면이 넓어진다. 프론트는 현재 `/auth/me`를 호출하지도 않는다.

기존 `/api/v1/fileupload`를 재사용하지 않는 이유는 별도로 기록한다.

- `permitAll`이라 인증 없이 누구나 업로드 가능
- `S3KeyType.study`와 `userId 0L`이 하드코딩됨
- 클라이언트가 보낸 URL을 그대로 신뢰하면 임의 URL 주입이 가능해짐

---

# 서버 설계

## API 계약

```
GET   /api/v1/users/me
  → 200 ApiResponse<UserProfileResponse, Void>
    data: { id, name, email, studentId, major, phoneNumber,
            userRole, team, membershipStatus, image }

PATCH /api/v1/users/me
  body: { name, major, phoneNumber }          // major는 코드(예: "DTE")
  → 200 ApiResponse<UserProfileResponse, Void>   // 수정 결과 전체 반환

PATCH /api/v1/users/me/image
  multipart/form-data: file
  → 200 ApiResponse<UserImageResponse, Void>     // { image: "https://..." }
```

`PATCH`가 결과 전체를 반환하는 이유는 프론트가 재조회 없이 화면을 갱신하기 위해서다.

**보안 설정 변경 없음.** `/api/v1/users/**`는 `SecurityConfig`의 permitAll 목록에 없어 기본 `.anyRequest().authenticated()`가 적용된다. 게시판 세 브랜치가 각자 `SecurityConfig`를 수정 중이므로 충돌을 피하는 것이 중요하다.

**대상 사용자는 항상 `@AuthenticationPrincipal`의 userId에서 취한다.** 경로·바디로 userId를 받지 않으므로 타인의 프로필을 수정할 경로가 존재하지 않는다.

## 변경 파일

| 파일 | 변경 |
|---|---|
| `domain/user/controller/UserProfileController` | 신설 |
| `domain/user/service/UserProfileService` | 신설 |
| `domain/user/dto/response/UserProfileResponse` | 신설 (record) |
| `domain/user/dto/response/UserImageResponse` | 신설 (record) |
| `domain/user/dto/request/UpdateUserProfileRequest` | 신설 (record) |
| `domain/user/entity/User` | `updateProfile(...)`, `updateImage(...)` 추가 |
| `domain/user/exception/UserErrorCode` | `INVALID_MAJOR`(400), `INVALID_PHONE_NUMBER`(400), `INVALID_IMAGE_FILE`(400) 추가 |
| `domain/resource/enums/S3KeyType` | `profile("profile")` 추가 |
| `global/util/MajorNormalizer` | 코드 유효성 확인용 public 메서드 추가 |

기존 `UserController`(이메일 중복 체크·아이디 찾기)와 `UserService`는 **건드리지 않는다.** 프로필은 별도 서비스로 분리해 각 단위가 하나의 책임만 갖게 한다.

**스키마 변경 없음 → Flyway 마이그레이션 없음.** 이 프로젝트는 down 스크립트가 없어 마이그레이션을 되돌릴 수 없으므로, 스키마를 건드리지 않는 것이 설계상 이점이다.

## 검증 규칙

| 필드 | 규칙 |
|---|---|
| `name` | `@NotBlank`, 1~30자 |
| `major` | `MajorNormalizer.normalize()` 후 알려진 코드인지 확인. 아니면 `INVALID_MAJOR` |
| `phoneNumber` | 정규식 `^01[0-9]\d{7,8}$` — **하이픈 없는 숫자만** |
| 이미지 | MIME `image/png`, `image/jpeg`, `image/webp` 화이트리스트. 최대 5MB |

세 필드 모두 값이 비어 오는 것을 허용하지 않는다(부분 수정이 아닌 전체 치환). `User`의 `name`·`major`·`phoneNumber`가 모두 `nullable = false`이기 때문이다.

**전화번호는 하이픈 없이 저장된다.** 회원가입이 `toDigits()`를 거친 값을 보낸다(`signup/page.tsx:256` → `phoneNumber: phoneDigits`). 기존 테스트 픽스처도 `"01012345678"` 형태다. 하이픈 포함 정규식을 쓰면 **기존 사용자 전원이 수정에 실패**하므로 숫자만 받는다. `01[0-9]`로 여는 이유는 011·016 등 구형 번호가 남아 있을 수 있어서다.

`major`를 서버에서도 검증하는 이유는 프론트가 드롭다운이어도 API는 직접 호출될 수 있기 때문이다. `MajorNormalizer.normalize()`는 알 수 없는 값을 그대로 되돌려주므로 정규화만으로는 검증이 되지 않는다.

## 예외

`BusinessException` + `ErrorCode` 패턴을 따른다. HTTP 상태는 `ErrorCode`가 정한다. 기존 `USER_NOT_FOUND`(404)를 재사용하고 위 세 개를 추가한다.

---

# 프론트 설계

## 라우트

```
app/profile/
  layout.tsx   <ApiCodeGuard requiredRole="GUEST" nextOverride="/profile">
  page.tsx     'use client' — 데이터 로딩 + 편집 상태
```

`requiredRole="GUEST"`는 rank 0이라 실질적으로 **로그인 여부만 확인**한다. 기존 가드를 그대로 재사용한다.

## 페이지 구조

단일 라우트에서 조회 ↔ 편집 모드를 전환한다. 디자인 노트의 "페이지 2개"는 라우트 분리가 아니라 두 화면 상태로 구현한다. 이 프로젝트는 `output: 'export'` 정적 사이트라 라우트를 나누면 edit 화면에서 데이터를 재조회하거나 상태를 넘겨야 하고, 취소 시 원본 복구도 번거로워진다.

```
components/profile/
  ProfileCard.tsx         아바타 · 이름 · 권한/소속 태그 · 이미지 변경 · 권한 배너
  ProfileInfoSection.tsx  개인정보 5필드 (조회 ↔ 편집)
  ApplicationStatus.tsx   활동 및 신청 현황
  profileTagMeta.ts       role/team → GdgColorTag 색 매핑 + 배너 문구
```

## 재사용 컴포넌트

새로 만들지 않고 기존 디자인 시스템을 쓴다.

`GdgSiteHeader`, `GdgSiteFooter`, `GdgColorTag`, `GdgInputField`, `GdgFieldContainer`, `GdgMajorDropdown`, `GdgButton`, `Loader`, `usePhoneNumber`(하이픈 자동 포맷)

## 태그·배너 매핑 (`profileTagMeta.ts`)

| `userRole` | 태그 색 | 배너 |
|---|---|---|
| GUEST | white | 없음 |
| MEMBER | white | 없음 |
| CORE | green | 운영진 권한이 부여된 계정입니다. |
| LEAD | blue | 운영진 권한이 부여된 계정입니다. |
| ORGANIZER | yellow | 운영진 권한이 부여된 계정입니다. |
| ADMIN | red | 관리자 권한이 부여된 계정입니다. |

| `team` | 태그 색 |
|---|---|
| HQ | white |
| BD | red |
| HR | blue |
| TECH | green |
| PR_DESIGN | yellow |
| `null` | white, 라벨 `NONE` |

`GdgColorTag`의 `color`(red/blue/green/yellow/white) × `fill`(off/on/half) 조합으로 디자인의 "테두리O/테두리X" 바리에이션을 표현한다.

## 학과 코드 변환

DB에는 학과가 **코드로 저장된다**(`디자인테크놀로지학과` → `DTE`). `MajorNormalizer`에는 정방향(label→code)만 있고 역변환이 없다.

**서버는 코드만 주고받고, 표시용 변환은 프론트가 담당한다.** `src/constant/majorOptions.ts`가 이미 `{ code, label }` 매핑과 `normalizeMajorCode()`를 갖고 있고, `GdgMajorDropdown`이 이를 사용한다. 편집 시 자유 입력이 아닌 드롭다운을 쓰므로 정규화 실패가 원천 차단된다.

## 전화번호 표시 변환

학과와 같은 구조다. **저장은 숫자만, 표시는 하이픈.**

- 조회·편집 표시: `formatPhoneNumberInput(value)` → `010-1234-5678`
- 입력 중 자동 포맷: `usePhoneNumber().formatInput`
- 전송 직전: `usePhoneNumber().toDigits` → `01012345678`

회원가입 페이지가 이미 쓰는 흐름을 그대로 따른다.

## 데이터 흐름

```
마운트 ─┬─ GET /users/me                      → 프로필
        └─ GET /recruit/core/applications/me  → 활동 현황

수정하기 → draft 상태로 복사 → 편집
  저장 → PATCH /users/me → 응답으로 갱신 + setUser 머지
  취소 → draft 폐기, 원본 유지

이미지 → PATCH /users/me/image (FormData) → 응답 URL 갱신 + setUser 머지
```

`setUser` 머지가 필요한 이유는 헤더 아바타와 이름이 `AuthProvider`의 `user`를 보기 때문이다. 이름·이미지가 바뀌면 두 소스가 어긋난다.

**API 호출은 반드시 `useAuthenticatedApi`의 `apiClient` 또는 `authorizedFetch`를 통한다.** axios를 새로 만들면 토큰 주입과 401 재발급 인터셉터를 우회하게 된다.

## 활동 및 신청 현황 — 주의점

`GET /recruit/core/applications/me`는 **`ApiResponse` 래퍼를 쓰지 않고 DTO를 직접 반환한다.** `RecruitCoreController`만 이 스타일이므로 `unwrapApiResponse`를 태우면 안 된다.

지원 이력이 없는 사용자는 **정상 상태**이므로 빈 상태로 렌더하고 에러로 처리하지 않는다.

## 랜딩 메뉴 (`OnboardingLanding.tsx:394`)

```
소개 / 활동 / FAQ
마이페이지  → /profile      (로그인한 모든 사용자)
대시보드    → /dashboard    (CORE 이상일 때만 노출)
```

`/dashboard` URL과 페이지는 변경하지 않는다. 같은 컴포넌트에 이미 `role` 변수가 있어 분기가 간단하다.

## 에러 처리

| 상황 | 처리 |
|---|---|
| 401 | 기존 인터셉터가 `/login?next=` 로 이동 |
| 403 | 기존 인터셉터가 `/` 로 이동 (프로필에서는 발생하지 않아야 정상) |
| 400 검증 실패 | 필드별 인라인 메시지 |
| 이미지 업로드 실패 | 인라인 메시지, 기존 이미지 유지 |
| 활동 현황 조회 실패 | 해당 섹션만 빈 상태. 페이지 전체를 막지 않음 |

---

# 명시된 가정

1. **모바일 디자인이 없다.** Figma 프레임이 전부 `PC_` 접두사다. 반응형은 기존 프로젝트 관례(`pc:`/`mobile:` breakpoint)를 따라 구현자가 판단한다
2. **`short` 그리드를 기준으로 한다.** 디자인 노트에 "short 그리드 버전만 하단부에 바리에이션으로 정리했습니다"라고 적혀 있어 태그 규격의 기준이 short다. `long`·`제안1`·`제안2`는 채택하지 않는다
3. **전화번호는 하이픈 없는 숫자로 저장된다.** `signup/page.tsx:256`이 `toDigits()` 결과를 보내는 것과 테스트 픽스처 `"01012345678"`로 확인했다. 화면 표시용 하이픈은 프론트가 `formatPhoneNumberInput()`으로 넣고, 전송 시 `toDigits()`로 되돌린다
4. **`users` 테이블의 실제 DB 제약은 리포에서 확인할 수 없다.** `ddl-auto: none`이고 `baseline-on-migrate: true`로 Flyway 이전 스키마가 baseline 처리되어 `users` CREATE 문이 리포에 없다. 따라서 엔티티의 `unique = true`는 실제 제약을 보장하지 않는다. 이번 설계는 이메일·학번을 읽기 전용으로 두어 이 불확실성을 우회한다

# 성공 기준

1. 로그인한 일반 회원(MEMBER)이 `/profile`에 진입해 **온보딩으로 튕기지 않고** 자기 정보를 본다
2. 이름·학과·전화번호를 수정하고 저장하면 화면과 헤더 아바타에 즉시 반영된다
3. 취소하면 원본이 그대로 남는다
4. 프로필 이미지를 변경하면 S3 URL이 저장되고 화면에 반영된다
5. 잘못된 학과 코드·전화번호 형식을 API로 직접 보내면 400으로 거부된다
6. **타인의 프로필을 수정할 수 있는 경로가 없다** (userId를 토큰에서만 취함)
7. 권한별 태그 색과 배너 문구가 디자인 매핑표와 일치한다
8. `./gradlew compileJava compileTestJava` 통과, 신규 테스트 통과
9. `npm run build` 통과 — 이 머신에는 yarn이 설치돼 있지 않다(`node`·`npm`·`npx`·`corepack`만 존재). 리포에 `yarn.lock`이 있고 CLAUDE.md도 yarn을 말하지만 실행할 수단이 없어 npm이 유일한 검증 경로다

# 테스트 전략

**Server**
- `UserProfileService` 단위 테스트: 조회·수정·이미지 갱신
- 검증 실패 케이스: 알 수 없는 학과 코드, 잘못된 전화번호 형식
- **권한 회귀 테스트**: 인증 주체와 다른 사용자를 수정할 수 없음을 보장. CLAUDE.md가 권한·가시성 로직 변경 시 테스트 추가를 요구한다
- 기존 실패 6건과 섞이지 않도록 신규 테스트만 따로 확인한다. CI는 파이프라인 종료 코드 문제로 테스트 실패를 감지하지 못하므로 로컬 확인이 필수다

**Web**
- 테스트 인프라가 없다. `yarn build`와 개발 서버 수동 확인에 의존한다

# 배포 순서

1. Server `develop` 머지 → 개발 서버 자동 배포
2. **실제 엔드포인트로 반영 확인.** Actions 초록불은 반영 근거가 아니다 — 워크플로우는 CodeDeploy에 요청만 하고 `wait`이 없다

   **401로는 판별할 수 없다.** `SecurityConfig`가 `.anyRequest().authenticated()`이므로 **존재하지 않는 경로도 401을 반환한다** — 인증 필터가 `DispatcherServlet`의 핸들러 조회보다 먼저 돌기 때문이다. Task 5 리뷰에서 확인됐고, 같은 이유로 보안 테스트 4개가 컨트롤러 없이도 통과했다.

   대신 `/v3/api-docs/**`가 `permitAll`이므로 springdoc이 실제로 매핑한 경로 목록으로 판별한다.

   ```bash
   curl -s "https://dev-api.gdgocinha.com/v3/api-docs" > /tmp/api-docs.json; echo "exit=$?"
   grep -c '/api/v1/users/me' /tmp/api-docs.json
   # 0 = 미반영 / 1 이상 = 반영 완료
   ```
3. Web `develop` 머지 → S3 sync + CloudFront 무효화 (전파에 시간 소요)
4. `dev.gdgocinha.com`에서 수동 확인
5. 운영 반영은 Server `main` → Web `master` 순. **브랜치 이름이 서로 다르다**

Server를 먼저 올리는 이유는 Web이 없는 API를 호출하지 않게 하기 위해서다.
