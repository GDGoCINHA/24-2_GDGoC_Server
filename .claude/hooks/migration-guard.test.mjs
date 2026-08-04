// 이미 공유된 Flyway 마이그레이션의 수정을 막나.
//
// Flyway 는 적용된 마이그레이션의 체크섬을 저장한다. 이미 적용된 파일을 고치면
// 다음 부팅에서 체크섬 불일치로 죽고, down 스크립트가 없어 DB 를 직접 손봐야 한다.
//
// guard.mjs 와 마찬가지로 판정은 순수 함수에 모아 두고 git 접근은 CLI 쪽에 둔다.
import { test } from "node:test";
import assert from "node:assert/strict";
import { isMigrationPath, judge, relPathFrom } from "./migration-guard.mjs";

// 절대 경로끼리 빼면 안 된다. Git Bash 는 `/c/Users/...`, git 은 `C:/Users/...` 를
// 주므로 path.relative() 로 계산하면 엉뚱한 값이 나오고, 그러면 마이그레이션이
// 아닌 것으로 판정돼 **조용히 통과한다.** 실제로 그렇게 통과했다 (2026-08-04).
test("상대 경로는 git 이 준 접두사로 만든다 — 절대 경로를 빼지 않는다", () => {
  assert.equal(
    relPathFrom("src/main/resources/db/migration/", "C:\\repo\\src\\main\\resources\\db\\migration\\V1__init.sql"),
    "src/main/resources/db/migration/V1__init.sql"
  );
  // 셸에 따라 들어오는 경로 형식이 달라도 결과가 같아야 한다
  assert.equal(
    relPathFrom("src/main/resources/db/migration/", "/c/repo/src/main/resources/db/migration/V1__init.sql"),
    "src/main/resources/db/migration/V1__init.sql"
  );
  // 리포 루트의 파일은 접두사가 빈 문자열이다
  assert.equal(relPathFrom("", "C:\\repo\\CLAUDE.md"), "CLAUDE.md");
});

test("마이그레이션 경로를 알아본다", () => {
  assert.ok(isMigrationPath("src/main/resources/db/migration/V20260519__create_event_board.sql"));
  assert.ok(isMigrationPath("db/migration/V1__init.sql"));
  // Windows 경로 구분자로도 들어온다
  assert.ok(isMigrationPath("src\\main\\resources\\db\\migration\\V20260519__x.sql"));
});

test("마이그레이션이 아닌 경로는 아니라고 한다", () => {
  assert.equal(isMigrationPath("src/main/java/Foo.java"), false);
  assert.equal(isMigrationPath("db/migration/README.md"), false);
  // 이름만 비슷한 경우까지 잡으면 정상 작업이 막힌다
  assert.equal(isMigrationPath("docs/db/migration-notes.sql"), false);
  assert.equal(isMigrationPath("db/migration/R__repeatable.sql"), false);
  assert.equal(isMigrationPath(null), false);
});

test("공유된 마이그레이션 수정은 차단한다", () => {
  const verdict = judge({ relPath: "db/migration/V1__init.sql", shared: "present" });
  assert.equal(verdict.decision, "deny");
  assert.match(verdict.reason, /체크섬/);
});

test("내 브랜치의 새 마이그레이션은 통과시킨다", () => {
  // 아직 어디에도 적용되지 않았으므로 고쳐도 안전하다.
  // 여기서 막으면 정상적인 마이그레이션 작성이 불가능해진다.
  assert.equal(judge({ relPath: "db/migration/V2__new.sql", shared: "absent" }), null);
});

test("마이그레이션이 아니면 판정하지 않는다", () => {
  assert.equal(judge({ relPath: "src/main/java/Foo.java", shared: "present" }), null);
  assert.equal(judge({ relPath: null, shared: "unknown" }), null);
});

// --- 여기부터는 "막지 않는다"를 고정한다 --------------------------------
//
// 이 가드는 guard.mjs 와 달리 **fail-open** 이다. 판정할 수 없을 때 막으면
// 오프라인·미fetch 상태에서 정상적인 신규 마이그레이션 작성까지 막힌다.
// 대신 조용히 넘어가지 않고 경고한다 — 조용한 통과와 판정된 통과는 다르다.

test("아는 구멍: 판정 불가일 때는 차단이 아니라 경고다", () => {
  const verdict = judge({ relPath: "db/migration/V1__init.sql", shared: "unknown" });
  assert.equal(verdict.decision, "warn");
  assert.match(verdict.reason, /판정할 수 없다/);
});
