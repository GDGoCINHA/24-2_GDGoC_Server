# 하네스

이 레포용 Claude Code 하네스. **커밋된다 — 클론하면 팀원 모두에게 붙는다.**

## 구성

| 경로 | 역할 | 커밋 |
|---|---|---|
| `settings.json` | 훅 등록 | O |
| `hooks/guard.mjs` | 되돌릴 수 없는 **명령** 차단 (PreToolUse) | O |
| `hooks/migration-guard.mjs` | 공유된 Flyway **파일** 수정 차단 (PreToolUse) | O |
| `hooks/lifecycle.mjs` | 산출물 수명 — 승격·회수 안내 | O |
| `hooks/verify.mjs` | 턴 종료 검증 — 변경이 있을 때만 (Stop) | O |
| `hooks/session-start.mjs` | 훅 활성 여부·리포 상태 한 줄 (SessionStart) | O |
| `parent-settings.example.json` | 부모 폴더에서 띄울 때의 배선 예시 | O |
| `rules/artifact-lifecycle.md` | 문서를 어디에 쓰나 (해당 경로를 건드릴 때 로드) | O |
| `work/<과제>/` | **작업 공간** — 산출물의 기본 목적지 | X |
| `attic/<과제>/` | 회수 보관소 — 끝난 과제에서 물러난 것 | X |

`work/`·`attic/` 이 커밋되지 않는 것이 요점이다. 레포에 남는 건 최종본뿐이다.

## 훅

| 언제 | 무엇 |
|---|---|
| 세션 시작 | `session-start.mjs` — 훅 활성 여부와 리포 상태 한 줄 |
| Bash·PowerShell 실행 전 | `guard.mjs` — `rm -rf`·force push·`reset --hard`·`DROP TABLE`·운영 배포(`main`·`master`) 차단 |
| Edit·Write 실행 전 | `migration-guard.mjs` — 이미 공유된 Flyway 마이그레이션 수정 차단 |
| `gh pr create` 직전 | `lifecycle.mjs promote` — `work/` 에 남은 산출물의 승격 여부를 묻는다 |
| 턴 종료 | `verify.mjs` — 변경이 있으면 컴파일 확인 |
| 턴 종료 | `lifecycle.mjs retire` — 머지된 과제의 작업 공간이 남아 있으면 알린다 |

세션 시작 한 줄을 빼면 **조건을 만족할 때만 말한다.** 평소에는 완전히 조용하다.

그 한 줄이 예외인 이유는 침묵의 뜻이 둘이기 때문이다 — "문제 없음"과 "내가 죽었음"이
구별되지 않는다. 훅이 살아 있다는 사실만은 조건 없이 말해야 한다.

## 테스트

```bash
node --test ".claude/hooks/*.test.mjs"
```

**따옴표와 글로브가 필요하다.** `node --test .claude/hooks/` 는 동작하지 않는다 —
Node 의 테스트 러너가 점으로 시작하는 디렉터리를 건너뛰어 테스트를 하나도 찾지 못하고,
인자를 모듈로 해석해 엉뚱한 `MODULE_NOT_FOUND` 를 낸다. **통과처럼 보이지 않는 것이
그나마 다행인 실패다.**

**가드는 반드시 테스트로 고정한다.** 2026-08-04 이전 가드는 `$CLAUDE_TOOL_INPUT` 환경변수를
읽었는데 훅 입력은 stdin 으로 들어온다. 변수가 늘 비어 있어 **아무것도 막지 않으면서 막는
것처럼 보였다.** 조용히 죽은 가드는 없는 가드보다 위험하다 — 막힌다고 믿고 위험한 작업을 하기 때문이다.

`guard.test.mjs` 는 **막지 못하는 것도 함께 고정한다**(스크립트 내부·변수 치환).
정규식으로 더 막으려 하면 정상 명령까지 잡혀 하네스를 못 쓰게 되므로, 막는 대신 아는 구멍으로 남긴다.

**`migration-guard.mjs` 만 fail-open 이다.** `guard.mjs` 는 판정할 수 없으면 막지만
(fail-closed), 마이그레이션 가드는 판정할 수 없으면 경고만 하고 통과시킨다. `origin/develop`
을 못 읽는 상황(오프라인·미fetch)에서 막으면 **정상적인 신규 마이그레이션 작성이 불가능해지고**,
그 비용이 오판의 비용보다 크다. 단 조용히 넘어가지는 않는다 — 판정된 통과와 판정 못 한
통과는 출력으로 구별된다. 이 구멍도 테스트에 고정돼 있다.

## 훅이 안 뜰 때

**설정은 세션을 띄운 디렉터리에서만 읽힌다.**

> "Claude Code reads this file from the directory the session runs in"
> — https://code.claude.com/docs/en/settings

하위 디렉터리의 `.claude/settings.json` 은 **읽히지 않는다.** 그래서 이 리포를 하위에 둔
부모 폴더에서 세션을 띄우면 여기 훅이 하나도 붙지 않는다. **재시작해도 같은 곳에서 띄우면
결과가 같다 — 실행 디렉터리가 원인이다.**

2026-08-04 이전 이 절은 "설정 파일이 세션 시작 시점에 없었으면 영영 안 읽힌다"고 적었는데
**틀렸다.** 그 오진 때문에 파일이 멀쩡히 있는데도 재시작만 반복한 세션이 있었다.
**틀린 진단은 없는 진단보다 비싸다.**

살리는 방법은 둘이다.

1. 리포 안에서 띄운다 — `cd 24-2_GDGoC_Server && claude`
2. 부모 폴더에서 띄운다면 부모에도 배선이 필요하다 — 아래 「부모 폴더에서 띄울 때」

### 훅이 실제로 붙었는지 확인하는 법

세션 시작 한 줄이 1차 증거다.

```
하네스 훅 활성 | 24-2_GDGoC_Server develop ✓clean
```

이 줄이 없으면 훅이 안 붙은 것이다. **개별 가드까지 확인하려면 프로브를 쏜다.**
실행돼도 무해하다 — `echo` 다.

```bash
echo "DROP TABLE hook_probe"
```

차단되면 가드가 살아 있다. 그냥 출력되면 훅이 안 붙었거나 가드가 죽은 것이다.
둘을 가르려면 스크립트를 직접 실행해 본다.

```bash
echo '{"tool_name":"Bash","tool_input":{"command":"echo DROP TABLE x"}}' | node .claude/hooks/guard.mjs
```

여기서 `deny` 가 나오면 **스크립트는 살아 있고 배선이 안 된 것**이다.
**둘은 원인이 다르므로 반드시 가른 뒤에 고친다.**

## 부모 폴더에서 띄울 때

`gdgocinha/` 처럼 이 리포와 `24-2_GDGoC_Web` 을 나란히 둔 폴더에서 세션을 띄우면,
그 폴더에도 `.claude/settings.json` 이 있어야 한다.

```bash
cd <부모 폴더>
mkdir -p .claude
cp 24-2_GDGoC_Server/.claude/parent-settings.example.json .claude/settings.json
```

**부모 폴더는 git 리포가 아니므로 그 설정은 어디에도 커밋되지 않는다.** 예시 파일만
커밋한다 — 자동 로드되지 않으므로 설치가 명시적 행위로 남는다. (스킬을 여기 두지 않는 것과
같은 논리다. 아래 「로컬 전용 자산」 참조.)

스크립트 사본은 **리포 안에만** 둔다. 부모 설정은 그것을 경로로 가리킬 뿐이다.
`--repo` 인자의 기본값이 `.` 이라 **리포 안에서 띄운 세션의 동작은 바뀌지 않는다.**

`guard.mjs` 와 `migration-guard.mjs` 는 **Server 사본으로 한 번만** 부른다. 전자는 명령
문자열만, 후자는 편집 대상 파일 경로만 보므로 리포에 무관하다 — 두 번 부르면 같은 차단
메시지가 두 번 나온다. `lifecycle.mjs`·`verify.mjs` 만 리포별로 두 번 부른다.

(설정 파일에는 주석을 달 수 없다. 스키마에 없는 키를 넣으면 거부될 수 있으므로
`parent-settings.example.json` 은 설명 없이 두고, 설명은 여기 둔다.)

복사한 뒤 **가리키는 파일이 전부 있는지 확인한다.** 없는 파일을 가리키는 훅은 조용히 죽는다.

```bash
node -e "
const s=require('./.claude/settings.json'), {existsSync}=require('fs');
let bad=0;
for (const ev of Object.values(s.hooks)) for (const g of ev) for (const h of g.hooks) {
  const m=h.command.match(/node\s+(\S+\.mjs)/); if(!m) continue;
  const ok=existsSync(m[1]); if(!ok) bad++;
  console.log((ok?'OK  ':'MISS')+' '+m[1]);
}
process.exit(bad?1:0);"
```

## 로컬 전용 자산

스킬·에이전트는 여기 두지 않는다. `description` 이 매 세션 컨텍스트에 실려 스킬 선택을
흐리는데 받는 쪽엔 뺄 수단이 없다. 각자 `~/.claude/skills/` 에 두고, 팀에 줄 만하면
플러그인으로 배포한다 — **설치가 명시적 행위가 되는 것이 핵심이다.**

배포·서버 진단은 `gdgoc-ops` 개인 스킬을 쓴다. 없으면 `CLAUDE.md` 의 「배포 워크플로우의
초록불은 "요청 성공"일 뿐이다」 절을 직접 본다.
