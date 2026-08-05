// 게이트가 언제 도는가.
//
// 대상은 develop push 와 PR 생성뿐이다. main·master 는 guard.mjs 가 **무조건** 막으므로
// 여기서 다시 판정하지 않는다 — 같은 명령을 두 훅이 막으면 어느 쪽이 막았는지 흐려진다.
import { test } from "node:test";
import assert from "node:assert/strict";
import { isTestGateTrigger } from "./test-gate.mjs";

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
