> 생성: 2026-07-28 · 최종 수정: 2026-07-28 16:07

# 0009. docs 폴더를 규범형(rules)과 서술형(conventions/domains 등)으로 분리

## 맥락

`daeng_v2_back` 마이그레이션 설계 문서([`0001`](0001-legacy-v1-v2-폐기.md) 등)에서 처음 그린 `docs/` 구조는 `architecture/`, `conventions/`, `flows/`, `migration/`, `domains/`로만 구성돼 있었다. 그런데 "이 프로젝트가 기술적으로 어떻게 되어 있는가"(서술형)와 "작업할 때 반드시 지켜야 하는가"(규범형)가 뒤섞여 있어, AI 에이전트가 어떤 문서를 규칙으로 취급해야 할지 구분하기 어려웠다.

## 검토한 후보

1. 기존 구조(`architecture/conventions/flows/migration/domains`)를 그대로 유지한다.
2. `rules/`(규범형)를 신설해 분리하고, ADR도 `architecture/` 하위가 아니라 `docs/` 최상위 폴더로 둔다.

## 결정

**후보 2 — `rules/`와 `adr/`를 최상위로 분리.**

근거:
- `conventions/`는 "이 프로젝트가 기술적으로 어떤 패턴/포맷을 쓰는지"를 서술하고, `rules/`는 "작업·문서 작성·git 과정에서 반드시 지켜야 하는 절차"를 규정한다 — 성격이 다르므로 분리한다.
- ADR(Architecture Decision Record라는 이름과 달리)은 아키텍처 결정에만 쓰이는 게 아니라 프로세스·툴링·컨벤션 변경 같은 다른 종류의 의사결정도 기록 대상이 된다. `architecture/` 하위에 두면 "아키텍처 관련 결정"으로 범위가 좁아 보이므로 최상위로 분리한다.

## 결과

`rules/`는 `docs/README.md`(전체 진입점)에서 "다른 무엇보다 먼저, 반드시 지킬 것"으로 안내한다. 이 결정 이후 구조는 몇 차례 더 다듬어졌다 — 예를 들어 `rules/git-commit.md`와 `rules/github.md`는 `rules/git.md` 하나로 합쳐졌고, 브레인스토밍 산출물은 `docs/superpowers/specs·plans/`를 거쳐 지금은 `docs/plans/<JIRA-KEY>-<kebab-설명>.md`(티켓 단위 계획 문서)로 정착했다. **현재 확정된 최종 구조와 각 폴더의 용도는 이 ADR이 아니라 [`docs/README.md`](../README.md)와 [`docs/rules/documentation.md`](../rules/documentation.md)를 항상 기준으로 삼는다** — 이 문서는 "왜 처음에 규범형/서술형을 나누기로 했는지"에 대한 역사적 기록이다.
