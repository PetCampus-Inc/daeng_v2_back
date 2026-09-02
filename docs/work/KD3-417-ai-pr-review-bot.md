> 생성: 2026-09-02 13:23 · 최종 수정: 2026-09-02 14:07

# KD3-417 AI PR 리뷰 봇 파일럿 설계

| 항목 | 값 |
|---|---|
| Jira | `KD3-417` |
| 브랜치 | `docs/KD3-417-ai-pr-review-bot` |
| 상위 에픽 | 확인 필요 |

## 현재 제어점

- 활성 workflow: `007-design-research`, `008-docs`
- 현재 공통 단계: `1`
- 다음 결정 또는 전환 조건: `.coderabbit.yaml`으로 한국어·비차단 자동 리뷰와 작업 문서 대조 지침을 버전 관리하고, KD3-417 PR에서 실제 발견사항을 확인한다.

## 작업 목표

PR의 작업 문서와 고정된 diff를 독립적으로 대조해 범위 누락·초과, 범위 밖 파일 혼입, 문서 간 직접 모순, 검증 기록 불일치를 발견하는 AI 리뷰 봇을 비차단 파일럿으로 도입한다.

## 작업 범위

- CodeRabbit GitHub App과 `.coderabbit.yaml`으로 AI 리뷰 설정을 저장소에서 버전 관리한다.
- 변경된 `docs/work/<JIRA-KEY>-*.md`와 PR diff를 대조하도록 CodeRabbit의 경로별 리뷰 지침을 구성한다.
- 봇이 남길 발견사항의 근거 형식(파일·위치·영향·개선 방향)과 비차단 운영 방식을 정의한다.
- PR 코드 실행, GitHub Actions 권한, fork PR, API key·소스코드 외부 전송 위험을 평가한다.
- 파일럿의 유효 발견률·오탐률·리뷰 지연을 측정할 기준을 정한다.

## 작업 제외 범위

- AI 봇을 merge 승인·필수 상태 검사로 즉시 지정하지 않는다.
- 승인된 보안·데이터 처리 정책 없이 소스코드 또는 secret을 외부 AI API에 전송하지 않는다.
- 기존 build·docs-check workflow의 검사 범위를 변경하지 않는다.

## 방향 논의 및 결정 사항

### 확정된 결정

- AI PR 리뷰 도구는 CodeRabbit으로 선택한다.
- GitHub App은 조직 전체가 아니라 `PetCampus-Inc/daeng_v2_back` 저장소에만 설치한다.
- 첫 파일럿은 자동 PR 리뷰를 사용하되, GitHub branch protection의 required check 또는 merge 승인 조건으로 지정하지 않는다.
- 파일럿 설정은 CodeRabbit UI에만 남기지 않고 `.coderabbit.yaml`으로 KD3-417 PR에서 검토·버전 관리한다.
- 리뷰 언어는 `ko-KR`로 설정하고, 기본 리뷰 강도는 `chill`로 시작한다.
- GitHub Checks 연동은 끈다. CI 실패 로그를 CodeRabbit 리뷰의 입력으로 사용하지 않는다.
- CodeRabbit 요약은 PR 본문이 아닌 Walkthrough 댓글에 표시하고, 관련 이슈·라벨·리뷰어 추천과 commit status는 파일럿에서 끈다.
- Jira 연동, 멀티레포 분석, 중앙 설정 저장소, CodeRabbit의 자동 수정 기능은 파일럿 범위에서 제외한다.
- 설치 직전에는 GitHub가 표시하는 실제 권한과 선택 저장소를 사용자에게 제시하고 최종 승인을 받는다.

### 확인된 사실

- 현재 `build.yml`과 `docs-check.yml`은 `pull_request` 이벤트에서 `contents: read` 권한으로 실행된다.
- `docs-check.yml`은 문서·검사 스크립트·workflow 변경 PR에만 실행되고, `build.yml`은 모든 PR에서 실행된다.
- KD3-415 독립 리뷰에서 범위 밖 KD3-416 작업 문서 혼입 위험과 검증 기록 대조 필요성이 확인됐다. 이 둘은 작업 문서와 diff를 비교하는 자동 리뷰 후보다.
- GitHub는 `pull_request_target`에서 PR 코드를 checkout·실행하면 base 저장소 token과 secrets가 노출될 수 있다고 경고한다. 이 이벤트는 PR 코드를 실행하는 AI 리뷰 workflow에 사용하지 않는다. [GitHub 보안 가이드](https://docs.github.com/en/actions/reference/security/securely-using-pull_request_target)
- 2026-09-02 기준 CodeRabbit은 public 저장소에 설치하면 PR 리뷰를 무료로 제공한다고 안내한다. private 저장소의 Essentials 요금은 연간 결제 시 개발자당 월 \$24이다. [CodeRabbit 가격](https://www.coderabbit.ai/pricing)
- 2026-09-02 기준 GitHub Copilot Pro는 사용자당 월 \$10이며 PR 리뷰를 포함한다. AI Credit 사용량과 상위 플랜 비용은 별도 관리 대상이다. [GitHub Copilot 가격](https://github.com/features/copilot/plans)
- CodeRabbit은 `AGENTS.md`를 기본 감지하는 코드 가이드라인 파일로 지원한다. 따라서 이 저장소의 AI 작업 규칙은 별도 복제 없이 리뷰 기준에 포함될 수 있다. [Code guidelines](https://docs.coderabbit.ai/knowledge-base/code-guidelines)
- CodeRabbit 경로별 지침은 해당 경로가 변경된 PR에 추가 리뷰 기준을 제공한다. PR 전체 변경과 작업 문서의 완전한 기계적 대조를 보장하지는 않으므로, 파일럿에서 실제 발견률을 검증한다. [Path instructions](https://docs.coderabbit.ai/configuration/path-instructions)
- GitHub Checks 연동을 꺼도 자동 PR 리뷰 자체는 계속 실행된다. CI 성공 뒤에만 리뷰하려면 자동 리뷰를 끄고 CI 완료 후 수동으로 `@coderabbitai review`를 호출하거나, 별도 자동화가 필요하다. [GitHub Checks](https://docs.coderabbit.ai/tools/github-checks)
- CodeRabbit은 설정 파일을 새로 추가·변경하는 PR에서 해당 파일을 적용하지 않고 대상 브랜치 또는 Repository UI 설정으로 리뷰할 수 있다. KD3-417 PR의 실제 리뷰도 `Repository UI` 설정으로 실행됐으므로, 이 파일의 변경은 병합 후 PR부터 검증한다.

### 검토한 대안

| 대안 | 비용 (2026-09-02 기준) | 장점 | 위험·제약 | 판단 |
|---|---|---|---|---|
| GitHub Actions `pull_request` + 외부 AI API | GitHub Actions와 선택 API의 사용량 과금. API 공급자·입력량 미정이라 총액 산정 불가 | 기존 CI와 통합이 단순함 | fork PR에는 secret이 전달되지 않아 외부 API 호출이 불가하며, 소스코드 외부 전송 정책이 필요함 | 파일럿 후보 |
| CodeRabbit GitHub App | 공개 \$0 / 비공개 \$24/인·월* | PR 코멘트 권한과 API key를 Actions에서 분리하고, PR 자동 리뷰를 제공 | 앱 권한·데이터 처리·공급망을 별도로 심사해야 함 | 선택 |
| GitHub Copilot PR 리뷰 | Pro 사용자당 월 \$10부터. AI Credit·상위 플랜 비용은 별도 | GitHub 기본 제품이라 설치·운영 접점이 적음 | Copilot 구독·AI Credit 예산 및 기능 정책 관리가 필요함 | 후보 미선택 |
| 사람이 매 PR마다 독립 리뷰 | 도구 구독료 없음. 리뷰어 투입 시간 비용 발생 | 도메인 판단에 강함 | 처리 지연과 담당자 가용성에 의존 | 고위험 변경에 유지 |
| AI 봇을 즉시 필수 승인 게이트로 적용 | 선택 도구 비용에 추가 비용 없음 | 누락을 강제할 수 있음 | 오탐과 모델 장애가 배포를 막을 수 있고, 품질 지표가 없음 | 배제 |

\* CodeRabbit Essentials의 비공개 저장소 요금은 연간 결제 기준이다.

### 권고안

- 첫 파일럿은 비차단으로 운영한다. 봇은 PR 요약 또는 review comment를 남기되, merge를 막지 않는다.
- CodeRabbit GitHub App을 선택 저장소 하나에만 설치한다. 자동 리뷰는 켜고 draft PR은 제외하는 설정을 우선 검토한다.
- `.coderabbit.yaml`에 한국어 응답, 비차단 자동 리뷰, 불필요한 외부 컨텍스트 비활성화, 작업 문서 경로의 대조 지침을 명시한다.
- 봇에는 원 작성 대화가 아닌 작업 문서·고정된 PR diff·CI 결과만 제공해 작성자의 가정과 분리된 검토를 유도한다.
- 봇은 범위·문서 정합성·검증 기록에 한정하고, 보안·동시성·도메인 설계의 최종 승인 역할을 맡지 않는다.
- 권한은 최소화한다. PR 코드를 실행하지 않고, `pull_request_target`과 write token·secret을 결합하지 않는다.
- 파일럿 기간의 유효 발견사항, 오탐, 수정으로 이어진 비율, 리뷰 완료 시간을 기록한 뒤에만 required check 전환을 판단한다.

### 미결 질문

- fork PR 지원 여부와 CodeRabbit의 실제 동작 범위를 설치 화면·공식 문서로 확인해야 한다.
- 파일럿 기간·성공 기준·required check 전환 기준을 정해야 한다.
- 장기적인 PR 승인·검증 절차 변경으로 확정하면 ADR이 필요한지 사용자와 판단해야 한다.

### 사용자 승인 기록

- 2026-09-02: 사용자가 AI 코드리뷰 자동화의 후속 작업 문서 작성과 설계를 요청했다. 구현·외부 서비스 연결·권한 부여는 승인되지 않았다.

## 완료 확인 기준

- CodeRabbit GitHub App의 설치 범위·권한, `.coderabbit.yaml`의 입력 범위, fork PR 처리를 확인하고 기록한다.
- 봇의 발견사항 형식과 비차단 파일럿의 성공·중단 기준을 정의한다.
- 현재 build·docs-check workflow와의 중복·권한 충돌이 없다.
- required check 또는 ADR 필요 여부를 근거와 함께 판단한다.

## 작업 후 확인 목록

| 문서 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-417-ai-pr-review-bot.md` | 갱신 | KD3-417의 설계·승인·검증 결과 단일 기록 |
| `.github/workflows/` | 변경 없음 | CodeRabbit GitHub App 파일럿은 자체 GitHub Actions workflow를 추가하지 않음 |
| `.coderabbit.yaml` | 추가 | KD3-417 파일럿의 한국어·비차단 자동 리뷰·작업 문서 대조 지침을 버전 관리 |
| `docs/workflows/000-common.md` | 구현 시 검토 예정 | 자동 AI 리뷰를 독립 리뷰로 인정할 조건이 정해질 경우 영향 확인 |
| `docs/adr/` | 구현 시 판단 예정 | 장기적인 PR 승인·검증 절차 변경의 ADR 필요 여부 |
