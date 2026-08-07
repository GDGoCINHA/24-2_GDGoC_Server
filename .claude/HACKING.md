# 하네스를 고칠 때

> **이 파일은 사본이다.** 반대쪽: `24-2_GDGoC_Web/.claude/HACKING.md`
> 공통 내용을 고치면 **반드시 양쪽을 함께 고친다.** 두 파일은 리포 고유 내용이 섞여
> 파일 단위 비교가 불가능하므로 자동 대조 대상이 아니다.

훅을 수정하거나 부모 폴더에 설치할 때 읽는다.
**훅이 안 뜨는 진단은 `README.md` 에 있다.**

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

**사본 대조는 `sync.test.mjs` 가 한다.** 두 리포가 나란히 있을 때만 돌고, 없으면
skip 하며 사유를 출력에 남긴다. **skip 을 통과로 읽지 마라 — 대조하지 못한 것이다.**
사본이 각자 자기 테스트를 통과하므로, 맞대보지 않으면 달라져도 양쪽 다 초록불이다.

## 진입점 판정을 복제하지 마라

훅이 "직접 실행됐는가"를 판정하는 코드는 **`is-main.mjs` 한 곳에만 둔다.**

```js
import { isMainModule } from "./is-main.mjs";
if (isMainModule(import.meta.url, process.argv[1])) { /* CLI */ }
```

이 판정이 훅 10개에 복제돼 있었고, **그래서 같은 버그를 함께 갖고 있었다.**
`import.meta.url.endsWith(argv[1])` 은 경로에 공백이나 한글이 있으면 영영 거짓이다 —
file URL 은 그것들을 퍼센트 인코딩하는데 `process.argv[1]` 은 하지 않는다. 그 결과
훅이 stdin 을 읽지도 않고 `exit 0` 으로 끝났다. **붙어 있는데 아무것도 막지 않고
아무 말도 안 하는 상태**였다.

Windows 계정명이 한글이거나 공백을 포함하면 그 팀원의 훅은 **전부** 죽는다.
2026-08-05 에 `C:/Users/홍길동/...` 형태 경로로 실측해 확인하고 고쳤다.

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

`test-gate.mjs` 도 **Server 사본으로 한 번만** 걸지만 이유가 다르다 — 앞의 둘과 달리
리포에 무관하지 않다. 게이트는 지금 Server 만 대상이다. Web 은 테스트 인프라가 없어
`--repo` 로 Server 를 명시해 한 번만 건다. **알려진 한계:** 부모 세션에서 Web 리포의
develop push 를 해도 게이트는 Server 의 테스트 결과로 판정한다 — Web 리포에 실제
관련된 검증은 아니다. Web 에 테스트 인프라가 생기기 전까지는 고치지 않는다.

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

배포·서버 진단은 `gdgoc-ops` 개인 스킬을 쓴다. 없으면 `CLAUDE.md` 의 「Actions 초록불은
배포 반영이 아니다」 절을 직접 본다.
