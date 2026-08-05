// 게이트가 언제 도는가.
//
// 대상은 develop push 와 PR 생성뿐이다. main·master 는 guard.mjs 가 **무조건** 막으므로
// 여기서 다시 판정하지 않는다 — 같은 명령을 두 훅이 막으면 어느 쪽이 막았는지 흐려진다.
import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync, rmSync, utimesSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { isTestGateTrigger, decideResponse, parseRepo, failureSummary, testsRanSince } from "./test-gate.mjs";

/** `build/test-results/test/<name>` 를 가진 임시 리포를 만들고, 정리 함수를 함께 준다. */
function tempRepoWithResults() {
  const repo = mkdtempSync(join(tmpdir(), "test-gate-"));
  const dir = join(repo, "build/test-results/test");
  mkdirSync(dir, { recursive: true });
  return { repo, dir, cleanup: () => rmSync(repo, { recursive: true, force: true }) };
}

test("develop push 는 트리거다", () => {
  // develop push 는 개발 서버 자동 배포이고 Flyway 마이그레이션이 자동 실행된다.
  assert.equal(isTestGateTrigger("git push origin develop"), true);
});

test("플래그가 끼어도 잡는다", () => {
  assert.equal(isTestGateTrigger("git push -u origin develop"), true);
});

test("따옴표 친 ref 도 잡는다", () => {
  // guard.mjs 가 `master"` 로 새어나갔던 전례가 있다(90e4c6c). 같은 실수를 반복하지 않는다.
  assert.equal(isTestGateTrigger('git push origin "develop"'), true);
  assert.equal(isTestGateTrigger("git push origin 'develop'"), true);
});

test("refspec 형태도 잡는다", () => {
  assert.equal(isTestGateTrigger("git push origin HEAD:develop"), true);
});

test("main·master 는 guard 영역이라 잡지 않는다", () => {
  assert.equal(isTestGateTrigger("git push origin main"), false);
  assert.equal(isTestGateTrigger("git push origin master"), false);
});

test("push 앞에 main/master 가 섞여도 develop push 는 잡는다", () => {
  // 회귀 고정: 예전 패턴은 명령 전체에서 main/master 를 찾아 이런 명령들을
  // 조용히 제외시켰다(실측 확인됨). git push 뒤만 보도록 앵커링을 옮긴 뒤의 동작이다.
  assert.equal(isTestGateTrigger("git merge main && git push origin develop"), true);
  assert.equal(
    isTestGateTrigger('git commit -m "sync with main" && git push origin develop'),
    true
  );
  assert.equal(isTestGateTrigger("git fetch origin master && git push origin develop"), true);
});

test("gh pr create --base main 은 조기 반환 자리를 유지해 여전히 잡힌다", () => {
  // main/master 제외 조건을 gh pr create 반환보다 뒤에 두면 안 된다 — 옮기면
  // `gh pr create --base main` 이 게이트를 빠져나가는 더 큰 구멍이 생긴다.
  assert.equal(isTestGateTrigger("gh pr create --base main --fill"), true);
});

test("ref 끝이 develop 이 아니면 잡지 않는다", () => {
  // `feature/develop-tools` 는 대상이 아니다. guard.mjs 가 `feature/main-menu` 를 다루는 방식과 같다.
  assert.equal(isTestGateTrigger("git push origin feature/develop-tools"), false);
});

test("PR 생성은 트리거다", () => {
  assert.equal(isTestGateTrigger("gh pr create --base develop --fill"), true);
});

test("무관한 명령은 잡지 않는다", () => {
  // 게이트가 아무 명령에나 돌면 매 턴이 29초씩 느려진다.
  assert.equal(isTestGateTrigger("echo hello"), false);
  assert.equal(isTestGateTrigger("git status"), false);
  assert.equal(isTestGateTrigger(""), false);
});

test("테스트가 통과하면 조용히 지나간다", () => {
  // 정상 작업을 방해하지 않는 것이 게이트가 받아들여지는 조건이다.
  assert.deepEqual(decideResponse({ ok: true, undecidable: false }), { action: "pass" });
});

test("테스트가 실패하면 막는다", () => {
  const r = decideResponse({ ok: false, undecidable: false, summary: "RecruitCoreApplicationServiceTest 5건 실패" });
  assert.equal(r.action, "deny");
  assert.match(r.message, /RecruitCoreApplicationServiceTest 5건 실패/);
});

test("실패 메시지는 재현 명령을 담는다", () => {
  // 막기만 하고 다음 행동을 못 정하면 게이트가 벽이 된다.
  const r = decideResponse({ ok: false, undecidable: false, summary: "1건 실패" });
  assert.match(r.message, /cleanTest test/);
});

test("실패 메시지는 기존 실패일 수 있음을 알린다", () => {
  // 이 리포는 기존 실패를 오래 안고 있던 이력이 있다. "내가 깼다"고 단정하면
  // 엉뚱한 코드를 고치게 된다.
  const r = decideResponse({ ok: false, undecidable: false, summary: "1건 실패" });
  assert.match(r.message, /기존/);
});

test("판정 불가는 통과시키되 알린다", () => {
  // fail-open. 막으면 오프라인에서 push 가 봉쇄된다.
  const r = decideResponse({ ok: false, undecidable: true, summary: "gradlew 를 실행하지 못했다" });
  assert.equal(r.action, "warn");
  assert.match(r.message, /gradlew 를 실행하지 못했다/);
});

test("판정 불가는 실패보다 우선한다", () => {
  // 테스트를 못 돌렸으면 ok 값은 의미가 없다. 판정 못 한 것과 실패한 것은 다르다.
  assert.equal(decideResponse({ ok: false, undecidable: true, summary: "x" }).action, "warn");
});

// --- parseRepo --------------------------------------------------------------

test("parseRepo: --repo 가 없으면 현재 디렉터리다", () => {
  assert.equal(parseRepo([]), ".");
  assert.equal(parseRepo(["--repo"]), ".");
});

test("parseRepo: --repo 값을 읽는다", () => {
  assert.equal(parseRepo(["--repo", "24-2_GDGoC_Server"]), "24-2_GDGoC_Server");
});

// --- failureSummary -----------------------------------------------------------
// 리뷰 지적(Important B): 이 판정 로직이 테스트 없이 배선만 됐었다.

test("failureSummary: 실패가 섞인 XML 을 정확히 집계한다", () => {
  const { repo, dir, cleanup } = tempRepoWithResults();
  try {
    writeFileSync(
      join(dir, "TEST-FooTest.xml"),
      `<testsuite name="FooTest" tests="3" failures="2" errors="1"></testsuite>`
    );
    writeFileSync(
      join(dir, "TEST-BarTest.xml"),
      `<testsuite name="BarTest" tests="2" failures="0" errors="0"></testsuite>`
    );
    const summary = failureSummary(repo);
    assert.match(summary, /테스트 실패 3건/);
    assert.match(summary, /FooTest 3건/);
    assert.doesNotMatch(summary, /BarTest/); // 실패 0건인 클래스는 집계에서 빠진다
  } finally {
    cleanup();
  }
});

test("failureSummary: 실패 0건이면 null 을 반환한다", () => {
  const { repo, dir, cleanup } = tempRepoWithResults();
  try {
    writeFileSync(
      join(dir, "TEST-BarTest.xml"),
      `<testsuite name="BarTest" tests="2" failures="0" errors="0"></testsuite>`
    );
    assert.equal(failureSummary(repo), null);
  } finally {
    cleanup();
  }
});

test("failureSummary: 결과 디렉터리가 없으면 null 이다", () => {
  const repo = mkdtempSync(join(tmpdir(), "test-gate-"));
  try {
    assert.equal(failureSummary(repo), null);
  } finally {
    rmSync(repo, { recursive: true, force: true });
  }
});

// --- testsRanSince ------------------------------------------------------------
// 리뷰 지적(Critical): 종료 코드만으로는 "테스트 실패"와 "애초에 못 돌았다"를 못 가른다.
// XML 이 이번 실행 동안 갱신됐는지로 가른다.

test("testsRanSince: 기준 시각 이후에 쓰인 XML 이 있으면 true 다", () => {
  const { repo, dir, cleanup } = tempRepoWithResults();
  try {
    const since = Date.now();
    writeFileSync(join(dir, "TEST-FooTest.xml"), `<testsuite name="FooTest"></testsuite>`);
    assert.equal(testsRanSince(repo, since), true);
  } finally {
    cleanup();
  }
});

test("testsRanSince: XML 이 기준 시각보다 훨씬 예전이면 false 다 (오래된 결과)", () => {
  const { repo, dir, cleanup } = tempRepoWithResults();
  try {
    const oldFile = join(dir, "TEST-FooTest.xml");
    writeFileSync(oldFile, `<testsuite name="FooTest"></testsuite>`);
    const old = new Date(Date.now() - 60_000); // 1분 전
    utimesSync(oldFile, old, old);
    assert.equal(testsRanSince(repo, Date.now()), false);
  } finally {
    cleanup();
  }
});

test("testsRanSince: 결과 디렉터리가 없으면 false 다", () => {
  const repo = mkdtempSync(join(tmpdir(), "test-gate-"));
  try {
    assert.equal(testsRanSince(repo, Date.now()), false);
  } finally {
    rmSync(repo, { recursive: true, force: true });
  }
});
