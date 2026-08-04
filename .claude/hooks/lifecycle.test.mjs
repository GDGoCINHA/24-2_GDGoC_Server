// 산출물 수명 훅 — 판정 로직.
//
// 순수 함수만 테스트한다. git 조회와 파일시스템 접근은 CLI 쪽에 있고,
// 여기서는 "이런 사실이 주어지면 무엇을 말해야 하나"만 고정한다.
import { test } from "node:test";
import assert from "node:assert/strict";
import {
  taskFromBranch,
  retirementTargets,
  promotionNotice,
  isPrCreate,
} from "./lifecycle.mjs";

// PR 생성 명령만 골라낸다. settings.json 의 `if` 매칭 문법에 기대지 않고
// 스크립트 안에서 판정한다 — 문법이 틀리면 조용히 안 뜨고, 그걸 알아챌 방법이 없다.
test("PR 생성 명령을 알아본다", () => {
  for (const c of [
    "gh pr create --base develop --title x",
    "gh pr create",
    "cd /repo && gh pr create --fill",
    "gh  pr  create --draft",
  ]) {
    assert.ok(isPrCreate(c), `PR 생성으로 봐야 한다: ${c}`);
  }
});

test("다른 명령은 승격 안내를 부르지 않는다", () => {
  for (const c of [
    "gh pr view 330",
    "gh pr merge 330 --merge",
    "gh pr list",
    "git push -u origin feature/x",
    "./gradlew test",
    "",
  ]) {
    assert.equal(isPrCreate(c), false, `PR 생성이 아니다: ${c}`);
  }
});

test("브랜치에서 과제 이름을 뽑는다", () => {
  assert.equal(taskFromBranch("feature/eventboard"), "eventboard");
  assert.equal(taskFromBranch("fix/deploy-image-prune"), "deploy-image-prune");
  assert.equal(taskFromBranch("chore/harness-guard"), "harness-guard");
  assert.equal(taskFromBranch("hotfix/urgent"), "urgent");
  assert.equal(taskFromBranch("standalone"), "standalone");
  // 접두사가 여러 겹이면 마지막만 쓴다 — 디렉터리 이름이므로 슬래시가 없어야 한다
  assert.equal(taskFromBranch("feature/board/event"), "board-event");
});

test("보호 브랜치에서는 과제를 만들지 않는다", () => {
  for (const b of ["develop", "main", "HEAD", ""]) {
    assert.equal(taskFromBranch(b), null, `과제가 아니어야 한다: ${b}`);
  }
});

test("머지된 브랜치의 작업 공간을 회수 대상으로 올린다", () => {
  const targets = retirementTargets({
    workDirs: ["eventboard", "harness-guard", "in-progress"],
    mergedTasks: ["eventboard", "harness-guard"],
  });
  assert.deepEqual(targets, ["eventboard", "harness-guard"]);
});

test("진행 중인 과제는 회수하지 않는다", () => {
  const targets = retirementTargets({
    workDirs: ["in-progress"],
    mergedTasks: [],
  });
  assert.deepEqual(targets, []);
});

test("작업 공간이 없으면 회수할 것도 없다", () => {
  assert.deepEqual(retirementTargets({ workDirs: [], mergedTasks: ["eventboard"] }), []);
});

test("승격 안내는 작업 공간에 파일이 있을 때만 뜬다", () => {
  const notice = promotionNotice({ task: "eventboard", workFileCount: 3 });
  assert.ok(notice, "안내가 있어야 한다");
  assert.match(notice, /eventboard/);
  assert.match(notice, /근거/, "자립 조건을 알려줘야 한다");
});

test("작업 공간이 비어 있으면 승격 안내를 하지 않는다", () => {
  assert.equal(promotionNotice({ task: "eventboard", workFileCount: 0 }), null);
  assert.equal(promotionNotice({ task: null, workFileCount: 5 }), null);
});
