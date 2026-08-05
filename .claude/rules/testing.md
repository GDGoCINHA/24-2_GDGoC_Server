---
paths:
  - "**/*Test.java"
  - "**/src/test/**"
  - ".github/workflows/**"
---

# 테스트·CI 를 건드릴 때

## CI 초록불 관련 이력 — 테스트 스텝만 고쳐졌다

`.github/workflows/ci.yml`의 테스트 스텝은 한때 `./gradlew test | tee test.log` 형태였다. 파이프 때문에 종료 코드가 `tee`의 것이 되어 **gradlew가 실패해도 CI는 성공으로 보고했다.** 실제로 2026-02-14부터 8월까지 테스트가 컴파일조차 안 되는 상태였으나 CI는 매번 "✅ Test 성공"을 찍었다. 당시엔 테스트 상태를 CI 배지가 아니라 **`Test Results` 체크나 로컬 실행 결과**로 확인해야 했다.

**2026-08-05 에 고쳤다.** 테스트 스텝(`ci.yml:80`)의 파이프를 지웠다 — `set -o pipefail`을 덧대는 것보다 단순하다. `tee`가 만든 `test.log`를 읽는 곳이 없었기 때문이다(테스트 결과 UI는 `build/test-results/**/*.xml`을 읽는다). **테스트 스텝은 신뢰할 수 있다 — `gradlew test`의 종료 코드가 그대로 CI 결과가 된다.**

**assemble 스텝(`ci.yml:64`)은 아직 예외다.** `./gradlew build -x test ... | tee build.log`가 그대로 남아 있어, 테스트 스텝이 6개월간 당한 것과 정확히 같은 조건이다(`shell:` 미지정 시 GitHub Actions 는 `bash -e`로 돌아 `pipefail`이 켜지지 않는다). 실측하면 `./gradlew spotlessCheck`가 303개 파일에서 위반으로 실패하고 `build -x test`에는 `spotlessCheck`·`jar`·`bootJar`가 포함된다 — **이 스텝은 지금 실제로 실패하고 있고 파이프가 그걸 가리고 있다.** PR 코멘트의 "✅ Assemble 성공"은 거짓이다. 지금 파이프를 지우지 않은 이유: 지우면 spotless 303건 때문에 모든 PR 이 즉시 빨간불이 되기 때문이다 — `ci.yml` 수정은 이번 작업 범위가 아니다.

**검증 명령을 파이프로 감싸지 마라.** `cmd | tail` 같은 패턴은 종료 코드를 가려 실패를 성공으로 보이게 한다 — 이 리포 CI가 6개월간 당한 문제, 그리고 지금 assemble 스텝이 겪고 있는 문제가 정확히 그것이다.

## 실패가 나오면 내가 만든 것인지 먼저 확인하라

> ⏱ **2026-08-05 실측: 38/38 통과, 실패 0.** 2026-08-04에 기록됐던 아래 6건은 그 사이 해결됐다.

**`./gradlew test`만 실행하면 `UP-TO-DATE`로 건너뛰고 캐시된 결과를 보여준다.** 현재 상태를 실제로 알려면 `./gradlew cleanTest test`를 쓴다.

해결된 과거 실패 — 같은 증상을 다시 만나면 참고할 것:

- `RecruitCoreApplicationServiceTest` 5건 — `RECRUITMENT_DEADLINE`이 `2026-03-14`로 하드코딩되어 마감이 지나자 실패했다. `Instant.now(clock)`으로 **`Clock`을 주입받게 바뀌어** 테스트가 시간을 고정할 수 있게 되면서 해결됐다.
  **다만 상수는 그대로다.** 테스트만 시간에서 풀렸을 뿐, 운영에서는 여전히 코어 지원 API가 마감 상태이며 다음 모집 시 코드 수정·재배포가 필요하다.
- `RecruitMemberMemoNotificationServiceTest` 1건 — 알림 상태가 `SENT` 대신 `PENDING`이었다.

## 버그 수정은 재현 테스트를 먼저 쓴다

실패를 확인한 뒤에 고친다. 순서를 뒤집으면 무엇을 고쳤는지 증명할 수 없다.

## 명령어

```bash
./gradlew cleanTest test   # 테스트 (캐시를 건너뛰지 않으려면 cleanTest 를 붙인다)
./gradlew compileTestJava  # 테스트 컴파일만 확인
```
