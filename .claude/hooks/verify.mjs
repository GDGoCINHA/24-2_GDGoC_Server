#!/usr/bin/env node
// Stop 훅 — 턴이 끝날 때 이 리포가 여전히 성립하는지 확인한다.
//
// **변경이 있을 때만 돈다.** 부모 폴더 세션에서는 두 리포의 검증이 모두 등록되므로,
// 안 건드린 리포까지 빌드하면 매 턴이 느려진다.
//
// dirty 판정을 소스 경로로 좁히지 않는 이유: 경로 목록은 낡으면 **검증을 조용히
// 건너뛴다.** 낭비는 눈에 보이고 조용한 누락은 안 보인다. 이 하네스의 실패 이력이
// 전부 후자다.

import { spawnSync } from "node:child_process";
import { resolve } from "node:path";

// --- 이 리포의 검증 방법. **리포마다 다른 유일한 부분이다.** -----------------
//
// 래퍼를 **절대 경로로** 부른다. 이 PC 는 `NoDefaultCurrentDirectoryInExePath=1`
// 이라 cmd 가 현재 디렉터리를 탐색하지 않아 `gradlew.bat` 만 쓰면 못 찾는다.
// 부모 폴더 세션에서는 cwd 가 리포 밖이라 상대 경로도 어차피 안 통한다.
const WRAPPER = process.platform === "win32" ? "gradlew.bat" : "gradlew";
const VERIFY = {
  label: "gradlew compileJava compileTestJava",
  command: (repo) => `"${resolve(repo, WRAPPER)}" compileJava compileTestJava --offline -q`,
  // Gradle 래퍼는 리포에 들어 있으므로 별도 설치가 필요 없다.
  toolReady: () => true,
  toolMissingHint: "",
};
// ---------------------------------------------------------------------------

/** `--repo <path>` 의 값. 없으면 현재 디렉터리 — 리포 안 세션은 인자가 필요 없다. */
export function parseRepo(argv) {
  const i = argv.indexOf("--repo");
  return i >= 0 && argv[i + 1] ? argv[i + 1] : ".";
}

/** @returns {{action: "skip"|"tool-missing"|"run"}} */
export function decide({ dirty, toolReady }) {
  if (!dirty) return { action: "skip" };
  if (!toolReady) return { action: "tool-missing" };
  return { action: "run" };
}

// --- CLI ------------------------------------------------------------------

// **안내는 stdout 으로 낸다.** stderr 는 훅 레코드에 보관만 되고, 화면에 쓰이는 `content`
// 필드로 올라오지 않는다 — 훅은 도는데 아무도 못 보는 상태가 된다. 2026-08-05 실측했다.
// 근거와 레코드 원문은 `.claude/work/harness-multirepo/verification.md` 3절에 있다.

const isMain =
  process.argv[1] && import.meta.url.endsWith(process.argv[1].replace(/\\/g, "/"));

if (isMain) {
  const repo = parseRepo(process.argv);

  const status = spawnSync("git", ["-C", repo, "status", "--porcelain"], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "ignore"],
  });
  if (status.status !== 0) {
    // 리포를 못 읽으면 조용히 넘어가지 않는다. 검증이 죽은 것과 통과가 구별되어야 한다.
    console.log(`[검증] ${repo} 의 git 상태를 읽지 못해 검증을 건너뛴다.`);
    process.exit(0);
  }

  const { action } = decide({
    dirty: status.stdout.trim() !== "",
    toolReady: VERIFY.toolReady(repo),
  });

  if (action === "skip") process.exit(0);
  if (action === "tool-missing") {
    console.log(`[검증] ${repo}: ${VERIFY.toolMissingHint}`);
    process.exit(0);
  }

  const res = spawnSync(VERIFY.command(repo), { cwd: repo, stdio: "inherit", shell: true });
  if (res.status !== 0) {
    console.log(`[검증] ${repo}: ${VERIFY.label} 실패 — 위 에러를 확인하라.`);
    process.exit(1);
  }
  process.exit(0);
}
