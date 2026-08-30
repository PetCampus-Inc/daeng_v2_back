> 생성: 2026-07-28 11:43 · 최종 수정: 2026-08-30 23:52

# docs 폴더 개요

이 저장소의 `docs/`는 AI(Claude 등)가 세션 간 기억을 갖지 않는다는 전제로, 프로젝트에 대한 맥락과 규칙을 코드 밖에 영구적으로 남겨두기 위한 폴더다. 새 작업을 시작하기 전 이 문서에서 폴더 구조를 파악하고, 실제 작성 규칙은 [`docs/rules/documentation.md`](rules/documentation.md)를 따른다.

이 서비스가 무엇을 하는 서비스이고 지금 어떤 범위를 만들고 있는지(제품 맥락)는 [`docs/service.md`](service.md)를 먼저 읽는다. 도메인 작업이나 아키텍처 문서는 그 다음이다.

AI 관련 작업 규칙(트레일러 금지, 결정 기록 워크플로우 등)은 리포 루트 [`AGENTS.md`](../AGENTS.md)에 있다. 대부분의 코딩 에이전트가 저장소 루트의 `AGENTS.md`를 관례적으로 자동 로드하므로, 도구 무관 진입점은 여기여야 한다.

## 폴더 구조

```text
docs/
├── README.md
├── service.md
├── rules/
│   ├── documentation.md
│   ├── git.md
│   ├── api-contract.md
│   ├── database-change.md
│   ├── notion-api-page-template.json
│   └── notion-api-spec-sync.md
├── workflows/
│   ├── 000-common.md
│   ├── 001-current-status.md
│   ├── 002-continue-work.md
│   ├── 003-migration.md
│   ├── 004-bug-fix.md
│   ├── 005-new-feature.md
│   ├── 006-code-review.md
│   ├── 007-design-research.md
│   └── 008-docs.md
├── work/
│   └── <JIRA-KEY>-<slug>.md
├── domains/
│   └── <domain>.md
├── inventory/
│   ├── api.md
│   ├── database.md
│   ├── integrations.md
│   └── operations.md
├── architecture/
│   ├── hexagonal.md
│   └── common-response-error.md
└── adr/
    └── NNNN-<slug>.md
```

## 문서 지도

| 위치 | 목적 |
|---|---|
| `service.md` | 서비스와 제품 범위 |
| `rules/` | 반드시 지킬 저장소 규칙 |
| `workflows/` | 작업 유형별 진행 절차 |
| `work/` | 티켓별 조사·결정·구현·검증 기록 |
| `domains/` | 후속 작업에도 유지할 도메인 사실과 제약 |
| `inventory/` | API·데이터·연동·운영 대상의 현재 구성, 누락 방지와 판정 |
| `architecture/` | 현재 공통 기술 구조와 책임 경계 |
| `adr/` | 되돌리기 어려운 결정의 이유 |

## 읽는 순서

1. 서비스·제품 맥락은 [`service.md`](service.md)를 읽는다.
2. 문서의 배치와 갱신 기준은 [`rules/documentation.md`](rules/documentation.md)를 읽는다.
3. 작업이면 [`workflows/000-common.md`](workflows/000-common.md)와 해당 유형 workflow를 읽는다.
4. 그 뒤 관련 `domains/`, `architecture/`, `inventory/`, `adr/`를 작업 범위에 맞춰 읽는다.

문서의 용도, 단일 기준, 작성·갱신 위치는 `README.md`가 아니라 [`rules/documentation.md`](rules/documentation.md)가 유일한 기준이다.

## AI 작업 시작

저장소 루트에서 `./scripts/ai-start codex` 또는 `./scripts/ai-start claude`를 실행하면, 두 도구 모두 같은 작업 접수 질문으로 대화형 세션을 시작한다. 이 첫 단계는 변경 없이 작업 유형·범위·완료 기준을 수집하는 단계다.
