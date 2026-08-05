#!/usr/bin/env node
import { spawnSync } from "node:child_process";
import { readFileSync, readdirSync, existsSync, statSync } from "node:fs";
import { join, resolve } from "node:path";
import { tellAgent } from "./agent-message.mjs";
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

// --- CLI ------------------------------------------------------------------

/** `--repo <path>` 의 값. 없으면 현재 디렉터리 — 리포 안 세션은 인자가 필요 없다. */
export function parseRepo(argv) {
  const i = argv.indexOf("--repo");
  return i >= 0 && argv[i + 1] ? argv[i + 1] : ".";
}

/**
 * 실패한 테스트를 XML 에서 센다. 못 읽으면 null — 종료 코드는 이미 확정된 사실이므로
 * **집계에 실패했다고 게이트를 열지는 않는다.**
 *
 * export 하는 이유: 리뷰 지적사항 — 이 판정 로직 자체가 테스트 없이 배선만 됐었다.
 */
export function failureSummary(repo) {
  const dir = join(repo, "build/test-results/test");
  if (!existsSync(dir)) return null;
  const classes = [];
  let total = 0;
  try {
    for (const f of readdirSync(dir)) {
      if (!f.endsWith(".xml")) continue;
      const head = readFileSync(join(dir, f), "utf8").slice(0, 2000);
      const fails = Number(/failures="(\d+)"/.exec(head)?.[1] ?? 0);
      const errors = Number(/errors="(\d+)"/.exec(head)?.[1] ?? 0);
      const n = fails + errors;
      if (n > 0) {
        classes.push(`${/name="([^"]+)"/.exec(head)?.[1] ?? f} ${n}건`);
        total += n;
      }
    }
  } catch {
    return null;
  }
  return total > 0 ? `테스트 실패 ${total}건 — ${classes.join(", ")}` : null;
}

/**
 * XML 결과가 **이번 실행 동안** 실제로 갱신됐는지 본다.
 *
 * 리뷰 지적(Critical): 종료 코드가 0 이 아니라고 곧장 "테스트 실패"로 보면 안 된다 —
 * gradlew.bat 부재·JAVA_HOME 무효·`--offline` 캐시 미스는 전부 exit 1 을 내지만 test
 * 태스크 자체가 안 돈 것이다. test 태스크가 실제로 돌았다면 XML 은 이번 실행 중에
 * 새로 쓰인다 — 그 여부로 "테스트가 실패했다"와 "테스트를 못 돌렸다"를 가른다.
 * (부수 효과로 Important A 도 같이 풀린다: 직전 실행의 오래된 XML 은 mtime 이 기준보다
 * 예전이므로 이번 실행의 실패로 오인되지 않는다.)
 *
 * 파일시스템 타임스탬프 해상도 오차를 감안해 2초 여유를 둔다.
 */
export function testsRanSince(repo, since) {
  const dir = join(repo, "build/test-results/test");
  if (!existsSync(dir)) return false;
  const cutoff = since - 2000;
  try {
    return readdirSync(dir).some(
      (f) => f.endsWith(".xml") && statSync(join(dir, f)).mtimeMs >= cutoff
    );
  } catch {
    return false;
  }
}

const isMain =
  process.argv[1] && import.meta.url.endsWith(process.argv[1].replace(/\\/g, "/"));

if (isMain) {
  const repo = parseRepo(process.argv);
  let raw = "";
  process.stdin.setEncoding("utf8");
  process.stdin.on("data", (d) => (raw += d));
  process.stdin.on("end", () => {
    let command = "";
    try {
      command = JSON.parse(raw)?.tool_input?.command ?? "";
    } catch {
      // 입력을 못 읽으면 조용히 넘어간다. 게이트는 조건부이지 가드가 아니다.
      process.exit(0);
    }
    if (!isTestGateTrigger(command)) process.exit(0);

    // 래퍼는 **절대 경로로** 부른다. 이 PC 는 NoDefaultCurrentDirectoryInExePath=1 이라
    // cmd 가 현재 디렉터리를 탐색하지 않는다.
    const wrapper = process.platform === "win32" ? "gradlew.bat" : "gradlew";
    // cleanTest 를 붙이지 않는다 — Gradle 의 UP-TO-DATE 는 입력 해시 기반이라 신뢰할 수 있고,
    // 매번 붙이면 안 바뀐 코드도 29초를 낸다.
    const startedAt = Date.now(); // XML 이 이번 실행으로 갱신됐는지 판별하는 기준선
    const res = spawnSync(`"${resolve(repo, wrapper)}" test --offline -q`, {
      cwd: repo,
      shell: true,
      timeout: 90_000, // 실측 29초의 3배. 배선 timeout(120초)보다 먼저 걸려야 침묵하지 않는다.
      stdio: ["ignore", "ignore", "pipe"],
      encoding: "utf8",
    });

    // spawnSync 는 타임아웃·실행 자체 실패에서 status 가 null 이다 — 이건 무조건 판정 불가다.
    // status 가 0 이 아니어도 "테스트가 실패해서"인지 "애초에 못 돌아서"인지는 종료 코드
    // 만으로 구별되지 않는다(리뷰 지적, Critical). test 태스크가 실제로 돌았는지는 XML 이
    // 이번 실행 동안 갱신됐는지로 판별한다 — 헤더의 "판정 불가는 fail-open" 원칙을
    // 여기까지 넓힌 것이다.
    const ranTests = res.status === 0 || testsRanSince(repo, startedAt);
    const undecidable = res.status === null || !ranTests;
    const summary =
      res.status === null
        ? `${repo}: gradlew 를 실행하지 못했다(${res.error?.code ?? res.signal ?? "알 수 없는 오류"}).`
        : !ranTests
        ? `${repo}: gradlew 가 종료 코드 ${res.status} 로 끝났지만 테스트 결과가 갱신되지 않았다 — ` +
          `테스트가 실행되지 못한 것으로 본다(컴파일 에러·래퍼·환경 문제로 추정).`
        : (failureSummary(repo) ?? `${repo}: 테스트 실패(종료 코드 ${res.status})`);

    const r = decideResponse({ ok: res.status === 0, undecidable, summary });
    if (r.action === "pass") process.exit(0);
    if (r.action === "warn") {
      tellAgent("PreToolUse", r.message);
      process.exit(0);
    }
    process.stdout.write(
      JSON.stringify({
        hookSpecificOutput: {
          hookEventName: "PreToolUse",
          permissionDecision: "deny",
          permissionDecisionReason: r.message,
        },
      }) + "\n"
    );
    process.exit(0); // JSON 은 exit 0 에서만 파싱된다.
  });
}
