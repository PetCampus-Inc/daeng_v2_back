> 생성: 2026-07-28 12:00 · 최종 수정: 2026-08-30 20:41

# Git 규칙

## 1. 커밋 메시지

### 형식

```
<type>(<scope>): <subject>

<body (선택)>

Refs: <JIRA-KEY>
```

### 규칙

| 항목      | 규칙                                                                                                                                               |
|---------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| type    | `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style` 중 하나                                                                                 |
| scope   | 도메인 슬러그(`auth`, `kindergarten`, `owner`, `pet`, `memo` 등 — `docs/domains/*.md`와 1:1 대응) 또는 인프라성 스코프(`build`, `ci`, `docs`, `global` — `global`은 `global/` 패키지처럼 도메인 무관 공통 계층 변경에 쓴다). 여러 **도메인 패키지**를 동시에 수정하는 경우(예: `auth`와 `owner`를 한 커밋에 함께 반영)에 한해 생략 가능 — `global/` 계층 변경은 여기 해당하지 않으므로 scope를 생략하지 말고 `global`을 명시한다 |
| subject | 한국어, 명령형, 마침표 없음                                                                                                                                 |
| footer  | `Refs: <JIRA-KEY>` 필수                                                                                                                            |


### 예시

```
feat(auth): add refresh token rotation

Refs: KD3-210
```

## 2. 브랜치 전략

### 기본 흐름 — `dev` → `epic` → 티켓 브랜치

모든 티켓은 `dev`에서 바로 파지 않는다. 먼저 그 티켓의 **상위 Jira 에픽**과 1:1 대응하는 epic 브랜치를 `dev`에서 파고(이미 있으면 재사용), 그 위에서 티켓 브랜치를 판다.

```
dev
 └─ epic/<JIRA-EPIC-KEY>-<kebab-설명>          그 에픽 소속 티켓이 처음 착수될 때 dev에서 생성, 이후 재사용
     └─ <type>/<JIRA-KEY>-<kebab-설명>          티켓 하나당 브랜치 하나, 항상 자신의 epic 브랜치 위에서 판다
```

- `type`은 위 커밋 메시지 type 목록과 동일한 어휘를 사용한다 (`feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style`)
- 예: 에픽 `KD3-194`(신규 Kotlin Server 구축) 소속 티켓 `KD3-210` → `epic/KD3-194-new-kotlin-server` 위에 `feat/KD3-210-refresh-token-rotation`
- `feat/A` → `epic/...`: **squash merge**한다 (커밋 1개, `Refs: <JIRA-KEY>` 유지)
- `epic/...` → `dev`: **squash하지 않고 일반 merge**한다. epic 브랜치를 squash로 합치면 하위 티켓들 각자의 커밋과 `Refs`가 하나로 뭉개져, `dev`의 히스토리만으로 어떤 커밋이 어떤 티켓 때문인지 추적할 수 없게 된다.
- epic 브랜치는 그 에픽 소속 티켓이 모두 merge되고 에픽 자체가 끝나면 `dev`로 합치고 삭제한다. 에픽이 진행 중인 동안은 여러 티켓 브랜치가 같은 epic 브랜치 위에서 병렬로 열릴 수 있다.

이 방식으로 `feat/B`를 `feat/A` 위에 바로 파는 스택 브랜치를 피한다 — `feat/A`가 epic 브랜치에 squash merge된 뒤에도 `feat/B`는 여전히 깨끗한 epic 브랜치를 base로 갖는다(`feat/A`의 원본 커밋이 섞여 diff가 지저분해지지 않는다).

## 3. PR 본문 템플릿

```markdown
## Summary
(변경 요약)

## Details
(주요 변경 내용, 설계 선택, 영향 범위)

## Notes
(영향도/주의사항, 후속 작업 필요 여부)

## Test plan
(검증 방법 — 체크리스트)

## Related issue
(Jira 링크)
- Work record: `docs/work/<JIRA-KEY>-<slug>.md`
```