---
paths:
  - "**/*Test.java"
  - "**/src/test/**"
  - ".github/workflows/**"
---

# 테스트·CI 를 건드릴 때

## CI의 초록불을 믿지 마라

`.github/workflows/ci.yml`의 테스트 스텝이 `./gradlew test | tee test.log` 형태다. 파이프 때문에 종료 코드가 `tee`의 것이 되어 **gradlew가 실패해도 CI는 성공으로 보고한다.**

실제로 2026-02-14부터 8월까지 테스트가 컴파일조차 안 되는 상태였으나 CI는 매번 "✅ Test 성공"을 찍었다. 테스트 상태는 CI 배지가 아니라 **`Test Results` 체크나 로컬 실행 결과**로 확인해야 한다.

수정하려면 스텝에 `set -o pipefail`을 추가하면 되지만, 그러면 아래 기존 실패로 모든 PR이 막힌다. **기존 실패를 먼저 정리한 뒤 게이트를 살릴 것.**

**검증 명령을 파이프로 감싸지 마라.** `cmd | tail`은 종료 코드를 가려 실패를 성공으로 보이게 한다 — 이 리포 CI가 6개월간 당한 문제가 정확히 그것이다.

## 기존 테스트 실패가 있다 — 내가 만든 것인지 먼저 확인하라

> ⏱ **스냅샷 2026-08-04** — 이후 수정되었을 수 있다. `./gradlew test`로 현재 상태를 먼저 확인하라.

- `RecruitCoreApplicationServiceTest` 5건 — `RecruitCoreApplicationService.java`의 `RECRUITMENT_DEADLINE`이 `2026-03-14`로 하드코딩되어 `Instant.now()`와 비교한다. 마감일이 지나 실패한다. **운영에서도 코어 지원 API가 계속 차단된 상태**이며, 다음 모집 시 코드 수정·재배포가 필요하다.
- `RecruitMemberMemoNotificationServiceTest` 1건 — 알림 상태가 `SENT` 대신 `PENDING`.

## 버그 수정은 재현 테스트를 먼저 쓴다

실패를 확인한 뒤에 고친다. 순서를 뒤집으면 무엇을 고쳤는지 증명할 수 없다.

## 명령어

```bash
./gradlew test             # 테스트
./gradlew compileTestJava  # 테스트 컴파일만 확인
```
