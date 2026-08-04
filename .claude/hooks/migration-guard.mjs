#!/usr/bin/env node
// PreToolUse 가드 — 이미 공유된 Flyway 마이그레이션의 수정을 막는다.
//
// Flyway 는 적용된 마이그레이션의 체크섬을 저장한다. 이미 적용된 파일을 고치면
// 다음 부팅에서 체크섬 불일치로 죽고, down 스크립트가 없어 DB 를 직접 손봐야 한다.
//
// **판정선은 "이미 공유되었나"** 다. 내 브랜치에서 방금 만든 파일은 아직 어디에도
// 적용되지 않았으므로 고쳐도 안전하다. origin/develop·origin/main 에 있으면 위험하다.
//
// guard.mjs 와 달리 **fail-open** 이다. 판정할 수 없을 때 막으면 오프라인에서
// 정상적인 신규 마이그레이션 작성까지 막힌다. 대신 조용히 넘어가지 않고 경고한다 —
// 조용한 통과와 판정된 통과는 다르다.
//
// 대상 리포는 **편집 대상 파일 경로에서 유도한다** — cwd 와 무관하게 동작하므로
// 부모 폴더 세션에서도 인자 없이 그대로 쓸 수 있다.

import { spawnSync } from "node:child_process";
import { basename, dirname } from "node:path";

const SHARED_REFS = ["origin/develop", "origin/main"];

export function isMigrationPath(relPath) {
  if (typeof relPath !== "string") return false;
  return /(^|\/)db\/migration\/V[^/]*\.sql$/.test(relPath.replace(/\\/g, "/"));
}

/**
 * 리포 기준 상대 경로. `prefix` 는 `git rev-parse --show-prefix` 의 결과다.
 *
 * **절대 경로끼리 빼지 않는다.** Git Bash 는 `/c/Users/...`, git 은 `C:/Users/...` 를
 * 주므로 `path.relative()` 로 계산하면 엉뚱한 값이 나오고, 그러면 마이그레이션이
 * 아닌 것으로 판정돼 **조용히 통과한다.** 상대 경로는 git 에게 직접 묻는다.
 */
export function relPathFrom(prefix, filePath) {
  if (typeof prefix !== "string" || typeof filePath !== "string") return null;
  return prefix.replace(/\\/g, "/") + basename(filePath.replace(/\\/g, "/"));
}

/**
 * @param {{relPath: string|null, shared: "present"|"absent"|"unknown"}} input
 * @returns {{decision: "deny"|"warn", reason: string} | null}  판정할 게 없으면 null
 */
export function judge({ relPath, shared }) {
  if (!isMigrationPath(relPath)) return null;
  if (shared === "present") {
    return {
      decision: "deny",
      reason:
        `${relPath} 는 이미 공유된 마이그레이션이다(${SHARED_REFS.join(" 또는 ")} 에 있다). ` +
        "수정하면 체크섬 불일치로 부팅이 실패하고, down 스크립트가 없어 되돌릴 수 없다. " +
        "새 파일 V{YYYYMMDD}__{설명}.sql 을 추가하라.",
    };
  }
  if (shared === "unknown") {
    return {
      decision: "warn",
      reason:
        `${relPath} 가 공유됐는지 판정할 수 없다(${SHARED_REFS.join("·")} 를 읽지 못했다). ` +
        "이미 적용된 파일이면 수정하지 마라.",
    };
  }
  return null;
}

// --- 수집 ------------------------------------------------------------------

const gitStatus = (cwd, args) =>
  spawnSync("git", ["-C", cwd, ...args], { stdio: "ignore" }).status;

/** 이 파일이 든 디렉터리의 리포 기준 접두사. 리포 밖이면 null. */
function repoPrefixOf(dir) {
  const res = spawnSync("git", ["-C", dir, "rev-parse", "--show-prefix"], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "ignore"],
  });
  return res.status === 0 ? res.stdout.trim() : null;
}

/**
 * 이 파일이 공유 ref 에 있나.
 *
 * ref 자체를 못 읽는 것과 ref 에 파일이 없는 것을 **반드시 구별한다** —
 * 섞으면 오프라인에서 "공유 안 됨"으로 오판해 조용히 통과시킨다.
 */
function sharedState(dir, relPath) {
  let anyRefResolved = false;
  for (const ref of SHARED_REFS) {
    if (gitStatus(dir, ["rev-parse", "--verify", "--quiet", ref]) !== 0) continue;
    anyRefResolved = true;
    if (gitStatus(dir, ["cat-file", "-e", `${ref}:${relPath}`]) === 0) return "present";
  }
  return anyRefResolved ? "absent" : "unknown";
}

// --- CLI ------------------------------------------------------------------
// 이 파일이 직접 실행될 때만 stdin 을 읽는다. import 하는 테스트는 여기 오지 않는다.

const isMain =
  process.argv[1] && import.meta.url.endsWith(process.argv[1].replace(/\\/g, "/"));

if (isMain) {
  let raw = "";
  process.stdin.setEncoding("utf8");
  process.stdin.on("data", (d) => (raw += d));
  process.stdin.on("end", () => {
    let filePath = null;
    try {
      filePath = JSON.parse(raw)?.tool_input?.file_path ?? null;
    } catch {
      // 파싱 실패를 조용히 넘기지 않는다. 가드가 죽은 것과 통과가 구별되어야 한다.
      console.error("[마이그레이션 가드] 훅 입력을 파싱하지 못해 판정을 건너뛴다.");
      process.exit(0);
    }
    if (typeof filePath !== "string") process.exit(0);

    const dir = dirname(filePath);
    const prefix = repoPrefixOf(dir);
    if (prefix === null) {
      // 리포를 못 찾았다. 대개는 리포 밖의 파일이라 조용히 넘어가는 게 맞지만,
      // 경로만 봐도 마이그레이션이면 **조용히 넘어가면 안 된다** — 판정하지 못한
      // 것과 안전한 것은 다르다. (`git -C` 가 셸 고유 경로 형식을 못 읽는 경우)
      if (isMigrationPath(filePath)) {
        console.error(
          `[마이그레이션 가드] ${filePath} 의 리포를 찾지 못해 판정하지 못했다. ` +
            "이미 적용된 파일이면 수정하지 마라."
        );
      }
      process.exit(0);
    }

    const relPath = relPathFrom(prefix, filePath);
    const verdict = judge({ relPath, shared: sharedState(dir, relPath) });
    if (!verdict) process.exit(0);

    if (verdict.decision === "warn") {
      console.error(`[마이그레이션 가드] ${verdict.reason}`);
      process.exit(0);
    }

    process.stdout.write(
      JSON.stringify({
        hookSpecificOutput: {
          hookEventName: "PreToolUse",
          permissionDecision: "deny",
          permissionDecisionReason: `BLOCKED: ${verdict.reason}`,
        },
      })
    );
    process.exit(0);
  });
}
