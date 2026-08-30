> 생성: 2026-07-28 11:43 · 최종 수정: 2026-08-30 19:09

# AGENTS

이 저장소에서 작업하는 모든 AI 에이전트가 항상 지켜야 하는 최소 원칙. 세부 규칙은 링크를 따라간다.

- 작업을 시작하기 전에 [`docs/README.md`](docs/README.md)로 `docs/` 폴더 전체 구조부터 파악한다.
- 변경 작업은 `./scripts/ai-start <codex|claude>`를 표준 진입점으로 사용한다. 직접 시작한 세션도 파일·Git·Jira·외부 시스템을 변경하기 전에 작업 유형을 판정하고, [`docs/workflows/000-common.md`](docs/workflows/000-common.md)와 해당 유형 workflow를 읽어 같은 절차를 적용한다.
- Jira 티켓을 맡으면 [`docs/workflows/000-common.md`](docs/workflows/000-common.md)의 절차를 따른다.
- 무언가 확정되면 그 자리에서 관련 문서에 반영한다 — 어디에 남길지는 [`docs/README.md`](docs/README.md)를 참고한다.
- 모르는 건 짐작하지 말고 사람에게 묻는다.
- 커밋·PR·문서 어디에도 AI 생성 트레일러(`Co-Authored-By: Claude` 등)를 남기지 않는다.
