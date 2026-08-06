# 내 정보(프로필) 조회·수정 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 로그인한 사용자가 `/profile`에서 자기 정보를 보고 이름·학과·전화번호·프로필 이미지를 수정할 수 있게 한다.

**Architecture:** 서버에 `/api/v1/users/me` 전용 리소스를 신설한다. 기존 `AuthUserResponse`(로그인·토큰갱신 응답에 재사용됨)는 건드리지 않아 개인정보가 localStorage에 상주하지 않게 한다. 프론트는 단일 라우트에서 조회 ↔ 편집 모드를 전환하며, 학과 코드와 전화번호 하이픈 변환은 프론트가 담당한다.

**Tech Stack:** Spring Boot 3.5 / Java 21 / JPA / JUnit5 + Mockito + AssertJ / Next.js 15 (정적 export) / TypeScript / Tailwind

## Global Constraints

- **작업 위치**: Server `C:/Users/good/Desktop/gdgocinha-profile/24-2_GDGoC_Server`, Web `C:/Users/good/Desktop/gdgocinha-profile/24-2_GDGoC_Web`. 둘 다 브랜치 `feature/user-profile`. **원본 `gdgocinha/`와 다른 세션의 `gdgocinha-recruit/`는 절대 건드리지 않는다**
- **두 리포는 각자 커밋한다.** 교차 커밋 금지. 커밋 전 `git rev-parse --show-toplevel`로 확인
- **전화번호는 하이픈 없는 숫자로 저장한다.** 서버 정규식 `^01[0-9]\d{7,8}$`. 표시용 하이픈은 프론트가 넣는다
- **학과는 코드로 주고받는다** (`DTE`, `CSE` …). 라벨 변환은 프론트 `majorOptions`가 담당
- **`SecurityConfig`를 수정하지 않는다.** `/api/v1/users/**`는 기본 `authenticated()`에 걸린다. 게시판 브랜치들과 충돌하는 파일이다
- **Flyway 마이그레이션을 만들지 않는다.** 스키마 변경이 없다
- **대상 사용자는 항상 `@AuthenticationPrincipal`의 userId에서 취한다.** 경로·바디로 userId를 받지 않는다
- 커밋 메시지는 conventional commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`)
- 응답은 `ApiResponse<T, M>`로 감싼다. 컨트롤러가 엔티티를 반환하지 않는다
- 예외는 `UserException`(= `BusinessException`) + `UserErrorCode`로 던진다

## File Structure

**Server** (`src/main/java/inha/gdgoc/`)

| 파일 | 책임 |
|---|---|
| `domain/user/entity/User.java` (수정) | `updateProfile`, `updateImage` 상태 변경 메서드 |
| `domain/user/exception/UserErrorCode.java` (수정) | 검증 실패 코드 3종 |
| `global/util/MajorNormalizer.java` (수정) | 코드 유효성 판별 |
| `domain/resource/enums/S3KeyType.java` (수정) | `profile` 키 타입 |
| `domain/user/dto/response/UserProfileResponse.java` | 프로필 조회·수정 응답 |
| `domain/user/dto/response/UserImageResponse.java` | 이미지 갱신 응답 |
| `domain/user/dto/request/UpdateUserProfileRequest.java` | 수정 요청 + Bean Validation |
| `domain/user/service/UserProfileService.java` | 조회·수정·이미지. 유일한 비즈니스 로직 위치 |
| `domain/user/controller/UserProfileController.java` | HTTP 경계. 로직 없음 |
| `domain/user/controller/message/UserProfileMessage.java` | 응답 메시지 상수 |

기존 `UserController`·`UserService`는 건드리지 않는다 (이메일 중복 체크·아이디 찾기 담당).

**Web** (`src/`)

| 파일 | 책임 |
|---|---|
| `types/profile.ts` | `UserProfile`, `UpdateProfilePayload`, `MyCoreApplication` 타입 |
| `services/profile/profileClient.ts` | 3개 API 호출 함수 |
| `components/profile/profileTagMeta.ts` | role/team → 태그색·배너 매핑 (순수 함수) |
| `components/profile/ProfileCard.tsx` | 아바타·이름·태그·배너·이미지 변경 |
| `components/profile/ProfileInfoSection.tsx` | 개인정보 5필드 조회/편집 |
| `components/profile/ApplicationStatus.tsx` | 활동 및 신청 현황 |
| `app/profile/layout.tsx` | 인증 가드 |
| `app/profile/page.tsx` | 데이터 로딩·편집 상태 조립 |
| `components/landing/OnboardingLanding.tsx` (수정) | 메뉴 링크 |

---

# Server

### Task 1: User 엔티티 상태 변경 메서드와 에러 코드

**Files:**
- Modify: `src/main/java/inha/gdgoc/domain/user/entity/User.java`
- Modify: `src/main/java/inha/gdgoc/domain/user/exception/UserErrorCode.java`
- Modify: `src/main/java/inha/gdgoc/global/util/MajorNormalizer.java`
- Test: `src/test/java/inha/gdgoc/domain/user/entity/UserProfileUpdateTest.java`

**Interfaces:**
- Produces: `User.updateProfile(String name, String major, String phoneNumber)`, `User.updateImage(String image)`, `UserErrorCode.INVALID_MAJOR|INVALID_PHONE_NUMBER|INVALID_IMAGE_FILE`, `MajorNormalizer.isKnownCode(String code)` → `boolean`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/inha/gdgoc/domain/user/entity/UserProfileUpdateTest.java`:

```java
package inha.gdgoc.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;

class UserProfileUpdateTest {

    @Test
    void updateProfile_changesOnlyEditableFields() {
        User user = createUser();

        user.updateProfile("김철수", "CSE", "01099998888");

        assertThat(user.getName()).isEqualTo("김철수");
        assertThat(user.getMajor()).isEqualTo("CSE");
        assertThat(user.getPhoneNumber()).isEqualTo("01099998888");
        assertThat(user.getStudentId()).isEqualTo("12201234");
        assertThat(user.getEmail()).isEqualTo("hong@inha.edu");
        assertThat(user.getUserRole()).isEqualTo(UserRole.CORE);
    }

    @Test
    void updateImage_replacesImageUrl() {
        User user = createUser();

        user.updateImage("https://bucket.s3.amazonaws.com/user/1/profile/abc.png");

        assertThat(user.getImage())
                .isEqualTo("https://bucket.s3.amazonaws.com/user/1/profile/abc.png");
    }

    private User createUser() {
        return User.builder()
                .name("홍길동")
                .major("DTE")
                .studentId("12201234")
                .phoneNumber("01012345678")
                .email("hong@inha.edu")
                .userRole(UserRole.CORE)
                .team(TeamType.TECH)
                .image(null)
                .social(null)
                .careers(null)
                .build();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd "C:/Users/good/Desktop/gdgocinha-profile/24-2_GDGoC_Server"
./gradlew test --tests "inha.gdgoc.domain.user.entity.UserProfileUpdateTest"
```

Expected: 컴파일 실패 — `cannot find symbol: method updateProfile`

- [ ] **Step 3: Add methods to User entity**

`User.java`의 `reject()` 메서드 바로 아래에 추가:

```java
    public void updateProfile(String name, String major, String phoneNumber) {
        this.name = name;
        this.major = major;
        this.phoneNumber = phoneNumber;
    }

    public void updateImage(String image) {
        this.image = image;
    }
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew test --tests "inha.gdgoc.domain.user.entity.UserProfileUpdateTest"
```

Expected: PASS (2 tests)

- [ ] **Step 5: Add error codes**

`UserErrorCode.java`의 enum 상수를 다음으로 교체 (기존 `USER_NOT_FOUND` 뒤에 세미콜론 대신 콤마):

```java
    // 400 BAD REQUEST
    INVALID_MAJOR(HttpStatus.BAD_REQUEST, "알 수 없는 학과입니다."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "전화번호 형식이 올바르지 않습니다."),
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 파일입니다."),

    // 404 NOT FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다.");
```

- [ ] **Step 6: Add code validation to MajorNormalizer**

`MajorNormalizer.java`의 `normalize` 메서드 아래에 추가:

```java
    public boolean isKnownCode(String code) {
        return code != null && KNOWN_CODES.contains(code);
    }
```

- [ ] **Step 7: Verify compilation**

```bash
./gradlew compileJava compileTestJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add src/main/java/inha/gdgoc/domain/user/entity/User.java \
        src/main/java/inha/gdgoc/domain/user/exception/UserErrorCode.java \
        src/main/java/inha/gdgoc/global/util/MajorNormalizer.java \
        src/test/java/inha/gdgoc/domain/user/entity/UserProfileUpdateTest.java
git commit -m "feat: User 프로필 수정 메서드와 검증 에러 코드 추가"
```

---

### Task 2: 프로필 조회 API

**Files:**
- Create: `src/main/java/inha/gdgoc/domain/user/dto/response/UserProfileResponse.java`
- Create: `src/main/java/inha/gdgoc/domain/user/service/UserProfileService.java`
- Test: `src/test/java/inha/gdgoc/domain/user/service/UserProfileServiceTest.java`

**Interfaces:**
- Consumes: Task 1의 `UserErrorCode.USER_NOT_FOUND`
- Produces: `UserProfileResponse.from(User)`, `UserProfileService.getMyProfile(Long userId)` → `UserProfileResponse`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/inha/gdgoc/domain/user/service/UserProfileServiceTest.java`:

```java
package inha.gdgoc.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.user.dto.response.UserProfileResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.exception.UserException;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.global.util.MajorNormalizer;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @Spy
    private MajorNormalizer majorNormalizer = new MajorNormalizer();

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void getMyProfile_returnsAllProfileFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));

        UserProfileResponse response = userProfileService.getMyProfile(1L);

        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.major()).isEqualTo("DTE");
        assertThat(response.studentId()).isEqualTo("12201234");
        assertThat(response.phoneNumber()).isEqualTo("01012345678");
        assertThat(response.email()).isEqualTo("hong@inha.edu");
        assertThat(response.userRole()).isEqualTo(UserRole.CORE);
        assertThat(response.team()).isEqualTo(TeamType.TECH);
    }

    @Test
    void getMyProfile_throwsWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getMyProfile(99L))
                .isInstanceOf(UserException.class);
    }

    static User createUser() {
        return User.builder()
                .name("홍길동")
                .major("DTE")
                .studentId("12201234")
                .phoneNumber("01012345678")
                .email("hong@inha.edu")
                .userRole(UserRole.CORE)
                .team(TeamType.TECH)
                .image(null)
                .social(null)
                .careers(null)
                .build();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "inha.gdgoc.domain.user.service.UserProfileServiceTest"
```

Expected: 컴파일 실패 — `UserProfileService`, `UserProfileResponse` 없음

- [ ] **Step 3: Create the response DTO**

Create `src/main/java/inha/gdgoc/domain/user/dto/response/UserProfileResponse.java`:

```java
package inha.gdgoc.domain.user.dto.response;

import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String studentId,
        String major,
        String phoneNumber,
        UserRole userRole,
        TeamType team,
        User.MembershipStatus membershipStatus,
        String image
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getStudentId(),
                user.getMajor(),
                user.getPhoneNumber(),
                user.getUserRole(),
                user.getTeam(),
                user.getMembershipStatus(),
                user.getImage()
        );
    }
}
```

- [ ] **Step 4: Create the service with read operation**

Create `src/main/java/inha/gdgoc/domain/user/service/UserProfileService.java`:

```java
package inha.gdgoc.domain.user.service;

import static inha.gdgoc.domain.user.exception.UserErrorCode.USER_NOT_FOUND;

import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.dto.response.UserProfileResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.exception.UserException;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.util.MajorNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final MajorNormalizer majorNormalizer;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
./gradlew test --tests "inha.gdgoc.domain.user.service.UserProfileServiceTest"
```

Expected: PASS (2 tests)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/inha/gdgoc/domain/user/dto/response/UserProfileResponse.java \
        src/main/java/inha/gdgoc/domain/user/service/UserProfileService.java \
        src/test/java/inha/gdgoc/domain/user/service/UserProfileServiceTest.java
git commit -m "feat: 내 프로필 조회 서비스 추가"
```

---

### Task 3: 프로필 수정 (검증 포함)

**Files:**
- Create: `src/main/java/inha/gdgoc/domain/user/dto/request/UpdateUserProfileRequest.java`
- Modify: `src/main/java/inha/gdgoc/domain/user/service/UserProfileService.java`
- Modify: `src/test/java/inha/gdgoc/domain/user/service/UserProfileServiceTest.java`

**Interfaces:**
- Consumes: Task 1의 `User.updateProfile`, `MajorNormalizer.isKnownCode`, `UserErrorCode.INVALID_MAJOR|INVALID_PHONE_NUMBER`; Task 2의 `UserProfileService`, `UserProfileResponse`
- Produces: `UserProfileService.updateMyProfile(Long userId, UpdateUserProfileRequest request)` → `UserProfileResponse`

- [ ] **Step 1: Write the failing tests**

`UserProfileServiceTest.java`에 다음 테스트를 추가 (import `inha.gdgoc.domain.user.dto.request.UpdateUserProfileRequest;` 추가):

```java
    @Test
    void updateMyProfile_updatesEditableFieldsAndNormalizesMajorLabel() {
        User user = createUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.updateMyProfile(
                1L,
                new UpdateUserProfileRequest("김철수", "컴퓨터공학과", "01099998888")
        );

        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.major()).isEqualTo("CSE");
        assertThat(response.phoneNumber()).isEqualTo("01099998888");
    }

    @Test
    void updateMyProfile_acceptsMajorCodeDirectly() {
        User user = createUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.updateMyProfile(
                1L,
                new UpdateUserProfileRequest("홍길동", "CSE", "01012345678")
        );

        assertThat(response.major()).isEqualTo("CSE");
    }

    @Test
    void updateMyProfile_rejectsUnknownMajor() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));

        assertThatThrownBy(() -> userProfileService.updateMyProfile(
                1L,
                new UpdateUserProfileRequest("홍길동", "없는학과", "01012345678")
        )).isInstanceOf(UserException.class);
    }

    @Test
    void updateMyProfile_rejectsHyphenatedPhoneNumber() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));

        assertThatThrownBy(() -> userProfileService.updateMyProfile(
                1L,
                new UpdateUserProfileRequest("홍길동", "DTE", "010-1234-5678")
        )).isInstanceOf(UserException.class);
    }

    @Test
    void updateMyProfile_doesNotTouchStudentIdOrEmail() {
        User user = createUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userProfileService.updateMyProfile(
                1L,
                new UpdateUserProfileRequest("김철수", "CSE", "01099998888")
        );

        assertThat(user.getStudentId()).isEqualTo("12201234");
        assertThat(user.getEmail()).isEqualTo("hong@inha.edu");
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew test --tests "inha.gdgoc.domain.user.service.UserProfileServiceTest"
```

Expected: 컴파일 실패 — `UpdateUserProfileRequest` 없음

- [ ] **Step 3: Create the request DTO**

Create `src/main/java/inha/gdgoc/domain/user/dto/request/UpdateUserProfileRequest.java`:

```java
package inha.gdgoc.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank
        @Size(min = 1, max = 30)
        String name,

        @NotBlank
        String major,

        @NotBlank
        String phoneNumber
) {
}
```

- [ ] **Step 4: Add update logic to the service**

`UserProfileService.java`에 상수와 메서드를 추가한다. import에 `inha.gdgoc.domain.user.dto.request.UpdateUserProfileRequest`, `java.util.regex.Pattern`, 그리고 `static inha.gdgoc.domain.user.exception.UserErrorCode.INVALID_MAJOR`, `INVALID_PHONE_NUMBER`를 더한다.

```java
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^01[0-9]\\d{7,8}$");

    public UserProfileResponse updateMyProfile(Long userId, UpdateUserProfileRequest request) {
        User user = findUser(userId);

        String major = majorNormalizer.normalize(request.major());
        if (!majorNormalizer.isKnownCode(major)) {
            throw new UserException(INVALID_MAJOR);
        }

        String phoneNumber = request.phoneNumber().trim();
        if (!PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
            throw new UserException(INVALID_PHONE_NUMBER);
        }

        user.updateProfile(request.name().trim(), major, phoneNumber);
        return UserProfileResponse.from(user);
    }
```

`userRepository.save()`를 부르지 않는 이유는 `@Transactional` 안에서 조회한 엔티티가 영속 상태라 변경 감지(dirty checking)로 반영되기 때문이다.

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew test --tests "inha.gdgoc.domain.user.service.UserProfileServiceTest"
```

Expected: PASS (7 tests)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/inha/gdgoc/domain/user/dto/request/UpdateUserProfileRequest.java \
        src/main/java/inha/gdgoc/domain/user/service/UserProfileService.java \
        src/test/java/inha/gdgoc/domain/user/service/UserProfileServiceTest.java
git commit -m "feat: 내 프로필 수정 서비스와 학과·전화번호 검증 추가"
```

---

### Task 4: 프로필 이미지 업로드

**Files:**
- Modify: `src/main/java/inha/gdgoc/domain/resource/enums/S3KeyType.java`
- Create: `src/main/java/inha/gdgoc/domain/user/dto/response/UserImageResponse.java`
- Modify: `src/main/java/inha/gdgoc/domain/user/service/UserProfileService.java`
- Modify: `src/test/java/inha/gdgoc/domain/user/service/UserProfileServiceTest.java`

**Interfaces:**
- Consumes: Task 1의 `User.updateImage`, `UserErrorCode.INVALID_IMAGE_FILE`; `S3Service.upload(Long, S3KeyType, MultipartFile)` → key, `S3Service.getS3FileUrl(String key)` → URL
- Produces: `UserProfileService.updateMyImage(Long userId, MultipartFile file)` → `UserImageResponse`

- [ ] **Step 1: Write the failing tests**

`UserProfileServiceTest.java`에 추가 (import: `inha.gdgoc.domain.resource.enums.S3KeyType`, `inha.gdgoc.domain.user.dto.response.UserImageResponse`, `org.springframework.mock.web.MockMultipartFile`, `org.springframework.web.multipart.MultipartFile`, `static org.mockito.ArgumentMatchers.any`, `static org.mockito.ArgumentMatchers.eq`):

```java
    @Test
    void updateMyImage_storesUrlReturnedByS3() throws Exception {
        User user = createUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(s3Service.upload(eq(1L), eq(S3KeyType.profile), any(MultipartFile.class)))
                .thenReturn("user/1/profile/uuid-avatar.png");
        when(s3Service.getS3FileUrl("user/1/profile/uuid-avatar.png"))
                .thenReturn("https://bucket.s3.amazonaws.com/user/1/profile/uuid-avatar.png");

        UserImageResponse response = userProfileService.updateMyImage(1L, pngFile());

        assertThat(response.image())
                .isEqualTo("https://bucket.s3.amazonaws.com/user/1/profile/uuid-avatar.png");
        assertThat(user.getImage())
                .isEqualTo("https://bucket.s3.amazonaws.com/user/1/profile/uuid-avatar.png");
    }

    @Test
    void updateMyImage_rejectsNonImageMimeType() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));

        MultipartFile pdf = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> userProfileService.updateMyImage(1L, pdf))
                .isInstanceOf(UserException.class);
    }

    @Test
    void updateMyImage_rejectsEmptyFile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));

        MultipartFile empty = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> userProfileService.updateMyImage(1L, empty))
                .isInstanceOf(UserException.class);
    }

    @Test
    void updateMyImage_rejectsFileLargerThan5Mb() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));

        MultipartFile huge = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> userProfileService.updateMyImage(1L, huge))
                .isInstanceOf(UserException.class);
    }

    private MultipartFile pngFile() {
        return new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew test --tests "inha.gdgoc.domain.user.service.UserProfileServiceTest"
```

Expected: 컴파일 실패 — `S3KeyType.profile`, `UserImageResponse` 없음

- [ ] **Step 3: Add the S3 key type**

`S3KeyType.java`의 enum 상수를 교체:

```java
    study("study"),
    recruitCore("recruit/core"),
    recruitMember("recruit/member"),
    profile("profile");
```

값이 `"profile"`인 이유는 `S3Service.buildKey`가 이미 `user/{userId}/` 접두사를 붙이기 때문이다. `"user/profile"`로 하면 `user/1/user/profile/...`이 된다.

- [ ] **Step 4: Create the image response DTO**

Create `src/main/java/inha/gdgoc/domain/user/dto/response/UserImageResponse.java`:

```java
package inha.gdgoc.domain.user.dto.response;

public record UserImageResponse(String image) {
}
```

- [ ] **Step 5: Add image update logic**

`UserProfileService.java`에 추가 (import: `inha.gdgoc.domain.resource.enums.S3KeyType`, `inha.gdgoc.domain.user.dto.response.UserImageResponse`, `org.springframework.web.multipart.MultipartFile`, `java.io.IOException`, `java.util.Set`, `static ...UserErrorCode.INVALID_IMAGE_FILE`):

```java
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp");
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    public UserImageResponse updateMyImage(Long userId, MultipartFile file) {
        User user = findUser(userId);

        if (file == null || file.isEmpty()) {
            throw new UserException(INVALID_IMAGE_FILE);
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new UserException(INVALID_IMAGE_FILE);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new UserException(INVALID_IMAGE_FILE);
        }

        String url;
        try {
            String key = s3Service.upload(userId, S3KeyType.profile, file);
            url = s3Service.getS3FileUrl(key);
        } catch (IOException e) {
            throw new UserException(INVALID_IMAGE_FILE);
        }

        user.updateImage(url);
        return new UserImageResponse(url);
    }
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
./gradlew test --tests "inha.gdgoc.domain.user.service.UserProfileServiceTest"
```

Expected: PASS (11 tests)

- [ ] **Step 7: Commit**

```bash
git add src/main/java/inha/gdgoc/domain/resource/enums/S3KeyType.java \
        src/main/java/inha/gdgoc/domain/user/dto/response/UserImageResponse.java \
        src/main/java/inha/gdgoc/domain/user/service/UserProfileService.java \
        src/test/java/inha/gdgoc/domain/user/service/UserProfileServiceTest.java
git commit -m "feat: 프로필 이미지 업로드 서비스 추가"
```

---

### Task 5: 컨트롤러와 보안 회귀 테스트

**Files:**
- Create: `src/main/java/inha/gdgoc/domain/user/controller/UserProfileController.java`
- Create: `src/main/java/inha/gdgoc/domain/user/controller/message/UserProfileMessage.java`
- Test: `src/test/java/inha/gdgoc/domain/user/controller/UserProfileSecurityTest.java`

**Interfaces:**
- Consumes: Task 2~4의 `UserProfileService.getMyProfile|updateMyProfile|updateMyImage`
- Produces: `GET/PATCH /api/v1/users/me`, `PATCH /api/v1/users/me/image`

- [ ] **Step 1: Note the established test skeleton**

이 리포의 보안 테스트는 `@ActiveProfiles("test")` + `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Autowired MockMvc` 조합을 쓴다 (`RecruitCoreSecurityTest` 참조). Step 2의 코드가 이미 이를 반영하고 있으므로 그대로 쓰면 된다.

- [ ] **Step 2: Write the failing security test**

Create `src/test/java/inha/gdgoc/domain/user/controller/UserProfileSecurityTest.java`:

```java
package inha.gdgoc.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 내 정보 엔드포인트의 인증 경계를 검증한다.
 *
 * <p>{@code /api/v1/users/**} 는 SecurityConfig 의 permitAll 목록에 없어야 하며
 * 기본 authenticated() 로 보호된다. 게시판 브랜치들이 같은 파일을 수정 중이라
 * 머지 과정에서 실수로 열리는 것을 이 테스트가 잡는다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class UserProfileSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMyProfile_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMyProfile_requiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"김철수\",\"major\":\"CSE\",\"phoneNumber\":\"01012345678\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMyImage_requiresAuthentication() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/users/me/image")
                        .file(new MockMultipartFile(
                                "file", "avatar.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 공개 경로를 startsWith 로 등록하면 하위 경로까지 열린다. 이 리포에서
     * {@code /api/v1/board/events/*} 가 관리자용 {@code /deleted} 까지 공개해버린 전례가 있다.
     */
    @Test
    void unknownSubPath_isNotPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/anything"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
./gradlew test --tests "inha.gdgoc.domain.user.controller.UserProfileSecurityTest"
```

Expected: FAIL — 404 (핸들러 없음) 또는 컴파일 실패

- [ ] **Step 4: Create the message constants**

Create `src/main/java/inha/gdgoc/domain/user/controller/message/UserProfileMessage.java`:

```java
package inha.gdgoc.domain.user.controller.message;

public final class UserProfileMessage {

    public static final String PROFILE_RETRIEVED_SUCCESS = "내 정보를 조회했습니다.";
    public static final String PROFILE_UPDATED_SUCCESS = "내 정보를 수정했습니다.";
    public static final String PROFILE_IMAGE_UPDATED_SUCCESS = "프로필 이미지를 변경했습니다.";

    private UserProfileMessage() {
    }
}
```

- [ ] **Step 5: Create the controller**

Create `src/main/java/inha/gdgoc/domain/user/controller/UserProfileController.java`:

```java
package inha.gdgoc.domain.user.controller;

import static inha.gdgoc.domain.user.controller.message.UserProfileMessage.PROFILE_IMAGE_UPDATED_SUCCESS;
import static inha.gdgoc.domain.user.controller.message.UserProfileMessage.PROFILE_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.user.controller.message.UserProfileMessage.PROFILE_UPDATED_SUCCESS;

import inha.gdgoc.domain.user.dto.request.UpdateUserProfileRequest;
import inha.gdgoc.domain.user.dto.response.UserImageResponse;
import inha.gdgoc.domain.user.dto.response.UserProfileResponse;
import inha.gdgoc.domain.user.service.UserProfileService;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "내 정보 조회")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse, Void>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        UserProfileResponse response = userProfileService.getMyProfile(me.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(PROFILE_RETRIEVED_SUCCESS, response));
    }

    @Operation(summary = "내 정보 수정")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping
    public ResponseEntity<ApiResponse<UserProfileResponse, Void>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails me,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserProfileResponse response = userProfileService.updateMyProfile(me.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(PROFILE_UPDATED_SUCCESS, response));
    }

    @Operation(summary = "프로필 이미지 변경")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserImageResponse, Void>> updateMyImage(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestPart("file") MultipartFile file
    ) {
        UserImageResponse response = userProfileService.updateMyImage(me.getUserId(), file);
        return ResponseEntity.ok(ApiResponse.ok(PROFILE_IMAGE_UPDATED_SUCCESS, response));
    }
}
```

**userId를 경로나 바디에서 받지 않는다.** 타인의 프로필을 지정할 방법이 구조적으로 없다.

- [ ] **Step 6: Run tests to verify they pass**

```bash
./gradlew test --tests "inha.gdgoc.domain.user.controller.UserProfileSecurityTest"
```

Expected: PASS (4 tests)

- [ ] **Step 7: Verify the full server build**

```bash
./gradlew compileJava compileTestJava
./gradlew test --tests "inha.gdgoc.domain.user.*"
```

Expected: 신규 테스트 전부 통과. **기존 실패 6건은 이 패키지 밖이므로 여기 나타나면 안 된다.** 나타나면 내가 만든 회귀다.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/inha/gdgoc/domain/user/controller/ \
        src/test/java/inha/gdgoc/domain/user/controller/
git commit -m "feat: 내 정보 조회·수정·이미지 변경 API 추가"
```

---

# Web

### Task 6: API 타입과 클라이언트

**Files:**
- Create: `src/types/profile.ts`
- Create: `src/services/profile/profileClient.ts`

**Interfaces:**
- Consumes: Task 2~5의 API 계약
- Produces: `UserProfile`, `UpdateProfilePayload`, `MyCoreApplication` 타입; `fetchMyProfile(apiClient)`, `updateMyProfile(apiClient, payload)`, `updateMyProfileImage(apiClient, file)`, `fetchMyCoreApplication(apiClient)`

- [ ] **Step 1: Create the types**

Create `src/types/profile.ts`:

```typescript
export type UserRoleValue = 'GUEST' | 'MEMBER' | 'CORE' | 'LEAD' | 'ORGANIZER' | 'ADMIN'

export type TeamValue = 'HQ' | 'HR' | 'PR_DESIGN' | 'TECH' | 'BD' | null

export type MembershipStatusValue = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface UserProfile {
  id: number
  name: string
  email: string
  studentId: string
  major: string
  phoneNumber: string
  userRole: UserRoleValue
  team: TeamValue
  membershipStatus: MembershipStatusValue
  image: string | null
}

export interface UpdateProfilePayload {
  name: string
  major: string
  phoneNumber: string
}

export type ApplicationStatusValue = 'SUBMITTED' | 'IN_REVIEW' | 'ACCEPTED' | 'REJECTED'

export interface MyCoreApplication {
  applicationId: number
  session: string
  team: string
  resultStatus: ApplicationStatusValue
}
```

- [ ] **Step 2: Create the API client**

Create `src/services/profile/profileClient.ts`:

```typescript
import type { AxiosInstance } from 'axios'

import type { MyCoreApplication, UpdateProfilePayload, UserProfile } from '@/types/profile'
import { unwrapApiResponse } from '@/utils/api/unwrap'

export const fetchMyProfile = async (apiClient: AxiosInstance): Promise<UserProfile> => {
  const response = await apiClient.get('/users/me')
  return unwrapApiResponse<UserProfile>(response.data)
}

export const updateMyProfile = async (
  apiClient: AxiosInstance,
  payload: UpdateProfilePayload
): Promise<UserProfile> => {
  const response = await apiClient.patch('/users/me', payload)
  return unwrapApiResponse<UserProfile>(response.data)
}

export const updateMyProfileImage = async (
  apiClient: AxiosInstance,
  file: File
): Promise<string> => {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.patch('/users/me/image', formData)
  return unwrapApiResponse<{ image: string }>(response.data).image
}

/**
 * 운영진 지원서 조회.
 * 이 엔드포인트만 ApiResponse 래퍼 없이 DTO를 직접 반환하므로 unwrap을 타지 않는다.
 * 지원 이력이 없으면 null을 돌려준다 — 정상 상태이며 에러가 아니다.
 */
export const fetchMyCoreApplication = async (
  apiClient: AxiosInstance
): Promise<MyCoreApplication | null> => {
  try {
    const response = await apiClient.get<MyCoreApplication>('/recruit/core/applications/me')
    return response.data ?? null
  } catch {
    return null
  }
}
```

`Content-Type`을 직접 지정하지 않는 이유는 `FormData`를 넘기면 axios가 boundary를 포함한 헤더를 자동으로 설정하기 때문이다. 수동 지정하면 boundary가 빠져 서버가 파싱하지 못한다.

- [ ] **Step 3: Verify the build**

```bash
cd "C:/Users/good/Desktop/gdgocinha-profile/24-2_GDGoC_Web"
yarn install
yarn build
```

Expected: 빌드 성공

- [ ] **Step 4: Commit**

```bash
git add src/types/profile.ts src/services/profile/profileClient.ts
git commit -m "feat: 프로필 API 타입과 클라이언트 추가"
```

---

### Task 7: 태그 매핑과 프로필 카드

**Files:**
- Create: `src/components/profile/profileTagMeta.ts`
- Create: `src/components/profile/ProfileCard.tsx`

**Interfaces:**
- Consumes: Task 6의 `UserProfile`, `UserRoleValue`, `TeamValue`
- Produces: `getRoleTagColor(role)`, `getRoleBanner(role)`, `getTeamTagColor(team)`, `getTeamLabel(team)`; `<ProfileCard profile onImageChange uploading imageError />`

- [ ] **Step 1: Create the mapping module**

Create `src/components/profile/profileTagMeta.ts`:

```typescript
import type { GdgTagColor } from '@/components/ui/design-system/GdgColorTag'
import type { TeamValue, UserRoleValue } from '@/types/profile'

const ROLE_TAG_COLOR: Record<UserRoleValue, GdgTagColor> = {
  GUEST: 'white',
  MEMBER: 'white',
  CORE: 'green',
  LEAD: 'blue',
  ORGANIZER: 'yellow',
  ADMIN: 'red'
}

const ROLE_BANNER: Partial<Record<UserRoleValue, string>> = {
  CORE: '운영진 권한이 부여된 계정입니다.',
  LEAD: '운영진 권한이 부여된 계정입니다.',
  ORGANIZER: '운영진 권한이 부여된 계정입니다.',
  ADMIN: '관리자 권한이 부여된 계정입니다.'
}

const TEAM_TAG_COLOR: Record<string, GdgTagColor> = {
  HQ: 'white',
  BD: 'red',
  HR: 'blue',
  TECH: 'green',
  PR_DESIGN: 'yellow'
}

export const getRoleTagColor = (role: UserRoleValue): GdgTagColor =>
  ROLE_TAG_COLOR[role] ?? 'white'

export const getRoleBanner = (role: UserRoleValue): string | null => ROLE_BANNER[role] ?? null

export const getTeamTagColor = (team: TeamValue): GdgTagColor =>
  team ? (TEAM_TAG_COLOR[team] ?? 'white') : 'white'

export const getTeamLabel = (team: TeamValue): string => {
  if (!team) return 'NONE'
  return team === 'PR_DESIGN' ? 'PR·DESIGN' : team
}
```

- [ ] **Step 2: Create the profile card**

Create `src/components/profile/ProfileCard.tsx`:

```tsx
'use client'

import { useRef } from 'react'

import { GdgColorTag } from '@/components/ui/design-system'
import type { UserProfile } from '@/types/profile'
import { cn } from '@/utils/cn'

import {
  getRoleBanner,
  getRoleTagColor,
  getTeamLabel,
  getTeamTagColor
} from './profileTagMeta'

const BANNER_CLASS: Record<string, string> = {
  green: 'bg-green text-white',
  blue: 'bg-blue text-white',
  yellow: 'bg-yellow text-black',
  red: 'bg-red text-white',
  white: ''
}

interface ProfileCardProps {
  profile: UserProfile
  onImageChange: (file: File) => void
  uploading?: boolean
  imageError?: string | null
}

export default function ProfileCard({
  profile,
  onImageChange,
  uploading = false,
  imageError = null
}: ProfileCardProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)

  const roleColor = getRoleTagColor(profile.userRole)
  const banner = getRoleBanner(profile.userRole)

  return (
    <section className="overflow-hidden rounded-2xl bg-gray-100/30">
      <div className="flex items-center gap-4 p-5">
        {profile.image ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={profile.image}
            alt=""
            className="size-16 shrink-0 rounded-full object-cover"
          />
        ) : (
          <div className="size-16 shrink-0 rounded-full bg-gray-400" aria-hidden />
        )}

        <div className="min-w-0 flex-1 space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <span className="typo-pc-h4 text-white">{profile.name}</span>
            <GdgColorTag color={roleColor} size="mini">
              {profile.userRole}
            </GdgColorTag>
            <GdgColorTag color={getTeamTagColor(profile.team)} size="mini">
              {getTeamLabel(profile.team)}
            </GdgColorTag>
          </div>
          <p className="typo-pc-b3 truncate text-gray-700">{profile.email}</p>
        </div>

        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
          className="shrink-0 rounded-full border border-white/10 px-3 py-1 typo-pc-c2 text-gray-700 transition hover:border-white/30 hover:text-white disabled:opacity-50"
        >
          {uploading ? '변경 중…' : '프로필 이미지 변경'}
        </button>

        <input
          ref={fileInputRef}
          type="file"
          accept="image/png,image/jpeg,image/webp"
          className="hidden"
          onChange={(event) => {
            const file = event.target.files?.[0]
            if (file) onImageChange(file)
            event.target.value = ''
          }}
        />
      </div>

      {imageError && (
        <p className="px-5 pb-3 typo-pc-c1 text-red">{imageError}</p>
      )}

      {banner && (
        <div className={cn('px-5 py-3 text-center typo-pc-b3', BANNER_CLASS[roleColor])}>
          {banner}
        </div>
      )}
    </section>
  )
}
```

- [ ] **Step 3: Verify the build**

```bash
yarn build
```

Expected: 빌드 성공.

색 토큰은 확인되었다 — `globals.css:26-33`에 `--color-red`·`--color-blue`·`--color-green`·`--color-yellow`가 정의돼 있어 `bg-red`·`bg-blue`·`bg-green`·`bg-yellow`를 그대로 쓸 수 있다. 빌드가 실패한다면 `GdgColorTag`가 children을 받는지(`GdgColorTag.tsx`의 props)와 `@/components/ui/design-system` 배럴에서 export되는지를 확인한다.

- [ ] **Step 4: Commit**

```bash
git add src/components/profile/profileTagMeta.ts src/components/profile/ProfileCard.tsx
git commit -m "feat: 프로필 카드와 권한·소속 태그 매핑 추가"
```

---

### Task 8: 개인정보 섹션 (조회 ↔ 편집)

**Files:**
- Create: `src/components/profile/ProfileInfoSection.tsx`

**Interfaces:**
- Consumes: Task 6의 `UserProfile`, `UpdateProfilePayload`
- Produces: `<ProfileInfoSection profile onSave saving error />`

- [ ] **Step 1: Create the component**

Create `src/components/profile/ProfileInfoSection.tsx`:

```tsx
'use client'

import { useState } from 'react'

import { GdgButton, GdgInputField, GdgMajorDropdown } from '@/components/ui/design-system'
import { usePhoneNumber } from '@/hooks/usePhoneNumber'
import type { UpdateProfilePayload, UserProfile } from '@/types/profile'
import { formatPhoneNumberInput } from '@/utils/phoneNumber'

interface ProfileInfoSectionProps {
  profile: UserProfile
  onSave: (payload: UpdateProfilePayload) => Promise<void>
  saving?: boolean
  error?: string | null
}

export default function ProfileInfoSection({
  profile,
  onSave,
  saving = false,
  error = null
}: ProfileInfoSectionProps) {
  const { formatInput, toDigits, isValidFormat } = usePhoneNumber()

  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(profile.name)
  const [major, setMajor] = useState(profile.major)
  const [phoneNumber, setPhoneNumber] = useState(formatPhoneNumberInput(profile.phoneNumber))

  const startEditing = () => {
    setName(profile.name)
    setMajor(profile.major)
    setPhoneNumber(formatPhoneNumberInput(profile.phoneNumber))
    setEditing(true)
  }

  const cancelEditing = () => setEditing(false)

  const canSave = name.trim().length > 0 && major.length > 0 && isValidFormat(phoneNumber)

  const handleSave = async () => {
    if (!canSave) return
    await onSave({
      name: name.trim(),
      major,
      phoneNumber: toDigits(phoneNumber)
    })
    setEditing(false)
  }

  return (
    <section className="space-y-4">
      <h2 className="typo-pc-h4 text-white">사용자 개인정보</h2>

      <div className="grid gap-4 pc:grid-cols-2">
        <GdgInputField
          label="이름"
          value={editing ? name : profile.name}
          state={editing ? 'available' : 'disabled'}
          disabled={!editing}
          onChange={(event) => setName(event.target.value)}
          fullWidth
        />
        <GdgInputField
          label="학번"
          value={profile.studentId}
          state="disabled"
          disabled
          fullWidth
        />
      </div>

      <div>
        {editing ? (
          <GdgMajorDropdown value={major} onChangeAction={setMajor} />
        ) : (
          <GdgInputField label="학과" value={profile.major} state="disabled" disabled fullWidth />
        )}
      </div>

      <GdgInputField
        label="전화번호"
        type="tel"
        value={editing ? phoneNumber : formatPhoneNumberInput(profile.phoneNumber)}
        state={editing && !isValidFormat(phoneNumber) ? 'error' : editing ? 'available' : 'disabled'}
        errorText={
          editing && !isValidFormat(phoneNumber) ? '전화번호 형식을 확인해 주세요.' : undefined
        }
        disabled={!editing}
        onChange={(event) => setPhoneNumber(formatInput(event.target.value))}
        fullWidth
      />

      <GdgInputField label="이메일" value={profile.email} state="disabled" disabled fullWidth />

      {error && <p className="typo-pc-c1 text-red">{error}</p>}

      <div className="flex justify-end gap-2">
        {editing ? (
          <>
            <GdgButton type="button" onClick={cancelEditing} disabled={saving}>
              취소
            </GdgButton>
            <GdgButton type="button" onClick={handleSave} disabled={!canSave || saving}>
              {saving ? '저장 중…' : '저장하기'}
            </GdgButton>
          </>
        ) : (
          <GdgButton type="button" onClick={startEditing}>
            수정하기
          </GdgButton>
        )}
      </div>
    </section>
  )
}
```

**학번과 이메일은 편집 모드에서도 `disabled`로 남는다.** 이메일은 계정 식별과 아이디 찾기의 기준이고, 학번은 향후 신청서 매칭 키로 쓰일 수 있다.

- [ ] **Step 2: Verify the build**

```bash
yarn build
```

Expected: 빌드 성공.

`GdgButton`은 `onClick`을 받는다 (`GdgButton.tsx:189,207`에서 확인). `OnboardingLanding.tsx`에 `onPress` 사용례가 있으나 그것은 NextUI 버튼이며 `GdgButton`이 아니다.

- [ ] **Step 3: Commit**

```bash
git add src/components/profile/ProfileInfoSection.tsx
git commit -m "feat: 개인정보 조회·편집 섹션 추가"
```

---

### Task 9: 활동 및 신청 현황

**Files:**
- Create: `src/components/profile/ApplicationStatus.tsx`

**Interfaces:**
- Consumes: Task 6의 `MyCoreApplication`, `ApplicationStatusValue`
- Produces: `<ApplicationStatus application loading />`

- [ ] **Step 1: Create the component**

Create `src/components/profile/ApplicationStatus.tsx`:

```tsx
'use client'

import type { ApplicationStatusValue, MyCoreApplication } from '@/types/profile'
import { cn } from '@/utils/cn'

const STATUS_CLASS: Record<ApplicationStatusValue, string> = {
  SUBMITTED: 'bg-blue text-white',
  IN_REVIEW: 'bg-yellow text-black',
  ACCEPTED: 'bg-green text-white',
  REJECTED: 'bg-red text-white'
}

const STATUS_LABEL: Record<ApplicationStatusValue, string> = {
  SUBMITTED: 'SUBMITTED',
  IN_REVIEW: 'IN-REVIEW',
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED'
}

interface ApplicationStatusProps {
  application: MyCoreApplication | null
  loading?: boolean
}

export default function ApplicationStatus({
  application,
  loading = false
}: ApplicationStatusProps) {
  return (
    <section className="space-y-4">
      <h2 className="typo-pc-h4 text-white">활동 및 신청 현황</h2>

      {loading ? (
        <p className="typo-pc-b3 text-gray-700">불러오는 중…</p>
      ) : application ? (
        <div className="flex items-center overflow-hidden rounded-full bg-gray-100/30">
          <span className="flex-1 px-5 py-3 typo-pc-b3 text-gray-700">운영진 지원서</span>
          <span
            className={cn(
              'min-w-[140px] px-5 py-3 text-center typo-pc-b3',
              STATUS_CLASS[application.resultStatus]
            )}
          >
            {STATUS_LABEL[application.resultStatus]}
          </span>
        </div>
      ) : (
        <p className="typo-pc-b3 text-gray-700">제출한 지원서가 없습니다.</p>
      )}
    </section>
  )
}
```

- [ ] **Step 2: Verify the build**

```bash
yarn build
```

Expected: 빌드 성공

- [ ] **Step 3: Commit**

```bash
git add src/components/profile/ApplicationStatus.tsx
git commit -m "feat: 활동 및 신청 현황 섹션 추가"
```

---

### Task 10: 프로필 페이지 조립

**Files:**
- Create: `src/app/profile/layout.tsx`
- Create: `src/app/profile/page.tsx`

**Interfaces:**
- Consumes: Task 6~9 전부

- [ ] **Step 1: Create the layout guard**

Create `src/app/profile/layout.tsx`:

```tsx
import type { ReactNode } from 'react'

import ApiCodeGuard from '@/components/auth/ApiCodeGuard'

export default function ProfileLayout({ children }: { children: ReactNode }) {
  return (
    <ApiCodeGuard requiredRole="GUEST" nextOverride="/profile">
      {children}
    </ApiCodeGuard>
  )
}
```

`requiredRole="GUEST"`는 rank 0이라 로그인한 모든 사용자를 통과시킨다.

- [ ] **Step 2: Create the page**

Create `src/app/profile/page.tsx`:

```tsx
'use client'

import { useCallback, useEffect, useState } from 'react'

import ApplicationStatus from '@/components/profile/ApplicationStatus'
import ProfileCard from '@/components/profile/ProfileCard'
import ProfileInfoSection from '@/components/profile/ProfileInfoSection'
import Loader from '@/components/ui/common/Loader'
import { useAuth } from '@/hooks/useAuth'
import { useAuthenticatedApi } from '@/hooks/useAuthenticatedApi'
import {
  fetchMyCoreApplication,
  fetchMyProfile,
  updateMyProfile,
  updateMyProfileImage
} from '@/services/profile/profileClient'
import type { MyCoreApplication, UpdateProfilePayload, UserProfile } from '@/types/profile'

export default function ProfilePage() {
  const { apiClient } = useAuthenticatedApi()
  const { user, setUser } = useAuth()

  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [application, setApplication] = useState<MyCoreApplication | null>(null)
  const [loading, setLoading] = useState(true)
  const [applicationLoading, setApplicationLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [imageError, setImageError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true

    const load = async () => {
      try {
        const data = await fetchMyProfile(apiClient)
        if (alive) setProfile(data)
      } finally {
        if (alive) setLoading(false)
      }
    }

    const loadApplication = async () => {
      const data = await fetchMyCoreApplication(apiClient)
      if (alive) {
        setApplication(data)
        setApplicationLoading(false)
      }
    }

    void load()
    void loadApplication()

    return () => {
      alive = false
    }
  }, [apiClient])

  const handleSave = useCallback(
    async (payload: UpdateProfilePayload) => {
      setSaving(true)
      setSaveError(null)
      try {
        const updated = await updateMyProfile(apiClient, payload)
        setProfile(updated)
        setUser({ ...user, name: updated.name, image: updated.image })
      } catch (error) {
        setSaveError('수정에 실패했습니다. 입력값을 확인해 주세요.')
        throw error
      } finally {
        setSaving(false)
      }
    },
    [apiClient, setUser, user]
  )

  const handleImageChange = useCallback(
    async (file: File) => {
      setUploading(true)
      setImageError(null)
      try {
        const image = await updateMyProfileImage(apiClient, file)
        setProfile((prev) => (prev ? { ...prev, image } : prev))
        setUser({ ...user, image })
      } catch {
        setImageError('이미지 변경에 실패했습니다. png·jpg·webp 5MB 이하만 가능합니다.')
      } finally {
        setUploading(false)
      }
    },
    [apiClient, setUser, user]
  )

  if (loading) return <Loader isLoading />
  if (!profile) return null

  return (
    <main className="min-h-screen bg-black px-6 py-10 text-white pc:px-10">
      <div className="mx-auto w-full max-w-[880px] space-y-10">
        <h1 className="typo-h3 mobile:typo-m-h2">내 정보 페이지</h1>

        <div className="space-y-4">
          <h2 className="typo-pc-h4 text-white">사용자 프로필</h2>
          <ProfileCard
            profile={profile}
            onImageChange={handleImageChange}
            uploading={uploading}
            imageError={imageError}
          />
        </div>

        <ProfileInfoSection
          profile={profile}
          onSave={handleSave}
          saving={saving}
          error={saveError}
        />

        <ApplicationStatus application={application} loading={applicationLoading} />
      </div>
    </main>
  )
}
```

`handleSave`가 실패 시 `throw`하는 이유는 `ProfileInfoSection`이 편집 모드를 유지해야 하기 때문이다 — 저장 실패인데 조회 모드로 돌아가면 입력이 사라진다.

- [ ] **Step 3: Verify the build**

```bash
yarn build
```

Expected: 빌드 성공

- [ ] **Step 4: Manual verification**

```bash
yarn dev
```

브라우저에서 `http://localhost:3000/profile` 접속 후 확인:
1. 로그인 상태에서 **온보딩으로 튕기지 않고** 내 정보가 보인다
2. 이름·학과·전화번호가 채워져 있고 전화번호에 하이픈이 표시된다
3. 학번·이메일 입력칸이 비활성이다
4. `수정하기` → 이름·학과·전화번호만 활성화된다
5. 값을 바꾸고 `저장하기` → 화면에 반영된다
6. `수정하기` → 값 변경 → `취소` → 원래 값으로 돌아온다
7. 프로필 이미지 변경 → 아바타가 바뀐다

- [ ] **Step 5: Commit**

```bash
git add src/app/profile/
git commit -m "feat: 내 정보 페이지 추가"
```

---

### Task 11: 랜딩 메뉴 링크 정리

**Files:**
- Modify: `src/components/landing/OnboardingLanding.tsx:394`

- [ ] **Step 1: Read the surrounding code**

```bash
sed -n '370,400p' src/components/landing/OnboardingLanding.tsx
```

`role` 변수(388행 부근)와 `menuItems` 배열(390~395행)의 실제 형태를 확인한다.

- [ ] **Step 2: Update the menu items**

`menuItems` 배열을 다음으로 교체한다:

```tsx
  const ROLE_RANK: Record<string, number> = {
    GUEST: 0,
    MEMBER: 1,
    CORE: 2,
    LEAD: 3,
    ORGANIZER: 4,
    ADMIN: 5
  }
  const canSeeDashboard = (ROLE_RANK[role] ?? 0) >= ROLE_RANK.CORE

  const menuItems = [
    { label: '소개', action: () => onNavigate('about') },
    { label: '활동', action: () => onNavigate('activities') },
    { label: 'FAQ', action: () => onNavigate('faq') },
    { label: '마이페이지', href: '/profile' },
    ...(canSeeDashboard ? [{ label: '대시보드', href: '/dashboard' }] : [])
  ]
```

`ROLE_RANK`는 `app/dashboard/page.tsx`에 있는 것과 같은 표다. 컴포넌트 밖 모듈 스코프에 두는 편이 낫다면 파일 상단으로 올린다.

**`/dashboard` 라우트와 페이지는 변경하지 않는다.** 이 태스크는 메뉴 항목만 다룬다.

- [ ] **Step 3: Verify the build**

```bash
yarn build
```

Expected: 빌드 성공

- [ ] **Step 4: Manual verification**

```bash
yarn dev
```

1. MEMBER 계정으로 로그인 → 메뉴에 `마이페이지`만 보이고 누르면 `/profile`로 간다. **온보딩으로 튕기지 않는다**
2. CORE 이상 계정 → `마이페이지`와 `대시보드`가 모두 보이고 각각 정상 이동한다

- [ ] **Step 5: Commit**

```bash
git add src/components/landing/OnboardingLanding.tsx
git commit -m "fix: 마이페이지 링크를 프로필로 연결하고 대시보드 항목 분리"
```

---

# 완료 검증

두 리포에서 각각 실행한다.

**Server**

```bash
cd "C:/Users/good/Desktop/gdgocinha-profile/24-2_GDGoC_Server"
./gradlew compileJava compileTestJava
./gradlew test --tests "inha.gdgoc.domain.user.*"
```

기존 실패 6건은 이 패키지 밖이다. 여기서 실패가 나오면 이번 작업이 만든 회귀다.

**Web**

```bash
cd "C:/Users/good/Desktop/gdgocinha-profile/24-2_GDGoC_Web"
yarn build
yarn format
```

**성공 기준 대조** — 스펙의 9개 항목을 모두 확인한다. 특히:
- 타인의 프로필을 수정할 경로가 없다 (Task 5의 컨트롤러가 userId를 토큰에서만 취함)
- 하이픈 없는 전화번호가 저장된다 (Task 3의 정규식, Task 8의 `toDigits`)
- `SecurityConfig`가 변경되지 않았다 — `git diff develop --stat`로 확인

# 배포

머지가 곧 배포다. 순서를 지킨다.

1. Server `feature/user-profile` → `develop`
2. **실제 엔드포인트로 반영 확인.** Actions 초록불은 근거가 아니다
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" "https://dev-api.gdgocinha.com/api/v1/users/me"
   ```
   `401` = 라우트 존재(반영됨). `404` = 미반영. 판별이 애매하면 예전부터 공개였던 `/api/v1/auth/login`을 대조군으로 함께 호출한다
3. Web `feature/user-profile` → `develop`. CloudFront 무효화 전파에 시간이 걸린다
4. `dev.gdgocinha.com/profile`에서 수동 확인
5. 운영은 Server `main` → Web `master` 순. **브랜치 이름이 서로 다르다**
