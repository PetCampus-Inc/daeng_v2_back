> 생성: 2026-07-28 12:00 · 최종 수정: 2026-07-28 15:11

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
| scope   | 도메인 슬러그(`auth`, `kindergarten`, `owner`, `pet`, `memo` 등 — `docs/domains/*.md`와 1:1 대응) 또는 인프라성 스코프(`build`, `ci`, `docs`). 여러 도메인에 걸친 변경은 생략 가능 |
| subject | 한국어, 명령형, 마침표 없음                                                                                                                                 |
| footer  | `Refs: <JIRA-KEY>` 필수                                                                                                                            |


### 예시

```
feat(auth): add refresh token rotation

Refs: KD3-210
```

## 2. 브랜치 전략

### 기본 흐름

`dev`에서 브랜치를 파고, 작업이 끝나면 PR을 올려 `dev`로 **squash merge**한다.

```
<type>/<JIRA-KEY>-<kebab-설명>
```

- `type`은 위 커밋 메시지 type 목록과 동일한 어휘를 사용한다 (`feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style`)
- 예: `feat/KD3-210-refresh-token-rotation`

### 의존성 있는 작업이 연달아 있을 때 (epic 브랜치)

작업 B가 작업 A에 의존해서 A → B 순서로 해야 하는 경우, `feat/B`를 `feat/A` 위에 바로 파는 스택 브랜치는 쓰지 않는다. `feat/A`가 `dev`에 squash merge되고 나면 `feat/A`의 원본 커밋들이 `feat/B`에 그대로 남아 diff가 깨끗하지 않기 때문이다.

대신 `dev`에서 **epic 브랜치**를 하나 파고, 그 위에 `feat/A`, `feat/B`를 각각 파서 PR을 epic 브랜치로 올린다.

```
epic/<JIRA-EPIC-KEY>-<kebab-설명>
```

- `feat/A` → `epic/...`: 기본 흐름과 동일하게 **squash merge**한다 (커밋 1개, `Refs: <JIRA-KEY>` 유지)
- `epic/...` → `dev`: **squash하지 않고 일반 merge**한다. epic 브랜치를 squash로 합치면 `feat/A`·`feat/B` 각자의 커밋과 `Refs`가 하나로 뭉개져, `dev`의 히스토리만으로 어떤 커밋이 어떤 티켓 때문인지 추적할 수 없게 된다.
- epic 브랜치는 하위 작업이 모두 merge되고 `dev`로 합쳐지면 삭제한다.

## 3. PR 본문 템플릿

```markdown
## Summary
(변경 요약)

## Notes
(영향도/주의사항, 후속 작업 필요 여부)

## Test plan
(검증 방법 — 체크리스트)

## Related issue
(Jira 링크)
```
