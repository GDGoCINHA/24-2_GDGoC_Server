#!/usr/bin/env node
// PreToolUse 게이트 — 깨진 테스트를 둔 채 코드가 공유·배포되는 것을 막는다.
//
// **대상은 develop push 와 PR 생성뿐이다.** main·master 는 guard.mjs 가 무조건 막는다.
// 성격이 다르므로(무조건 차단 vs 테스트 결과에 따른 조건부 차단) 대상도 겹치지 않게 한다.
//
// **통과하면 조용하다.** 테스트가 통과하는 정상 작업은 아무 방해도 받지 않는다.
//
// 판정 불가(오프라인·래퍼 실패·타임아웃)는 **fail-open** 이다. 막으면 네트워크 문제로
// push 가 봉쇄된다. 대신 조용히 넘어가지 않고 알린다 — migration-guard.mjs 와 같은 원칙이다.

/**
 * 이 명령에 게이트를 걸어야 하나.
 *
 * ref 는 **끝이 develop 인지**만 본다. `feature/develop-tools` 를 잡지 않기 위해서다.
 * 따옴표를 허용하는 이유: `git push origin "develop"` 은 정상적인 셸 표현인데
 * 예전 guard 패턴은 뒤에 공백이나 끝만 봐서 `master"` 로 그대로 새어나갔다.
 */
export function isTestGateTrigger(command) {
  if (typeof command !== "string" || command === "") return false;
  if (/\bgh\s+pr\s+create\b/.test(command)) return true;
  if (!/\bgit\s+push\b/.test(command)) return false;
  // main·master 는 guard 영역이다. 여기서 판정하지 않는다.
  if (/\b(main|master)["']?(\s|$)/.test(command)) return false;
  return /\bdevelop["']?(\s|$)/.test(command);
}

/** 실패했을 때 에이전트가 다음에 무엇을 할지 정할 수 있어야 한다. */
const FAIL_HINT =
  "고치기 전에는 push 하지 마라. 재현: `./gradlew cleanTest test` " +
  "(cleanTest 없이 test 만 하면 UP-TO-DATE 로 캐시된 결과가 나온다). " +
  "**기존 실패일 수 있으니 내 변경이 만든 것인지 먼저 확인하라** — 이 리포는 기존 실패를 오래 안고 있던 이력이 있다.";

/**
 * @param {{ok: boolean, undecidable: boolean, summary?: string}} result
 * @returns {{action: "pass"|"deny"|"warn", message?: string}}
 */
export function decideResponse({ ok, undecidable, summary }) {
  // 판정 불가가 먼저다. 테스트를 못 돌렸으면 ok 는 의미가 없다.
  if (undecidable) {
    return { action: "warn", message: `[테스트 게이트] ${summary} 테스트를 확인하지 못한 채 통과시킨다.` };
  }
  if (ok) return { action: "pass" };
  return { action: "deny", message: `[테스트 게이트] ${summary}. ${FAIL_HINT}` };
}
