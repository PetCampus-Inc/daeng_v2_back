> 생성: 2026-07-28 11:43 · 최종 수정: 2026-08-30 15:10

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
│   └── git.md
├── workflows/
│   ├── common.md
│   ├── new-feature.md
│   └── migration.md
├── work/
│   └── <JIRA-KEY>-<slug>.md
├── domains/
│   └── <domain>.md
├── api/
│   ├── inventory.md
│   └── policy.md
├── database/
│   ├── inventory.md
│   └── policy.md
├── integrations/
│   └── inventory.md
├── operations/
│   └── inventory.md
├── architecture/
│   ├── hexagonal.md
│   ├── common-response-error.md
│   └── migration-strategy.md
├── conventions/
│   └── notion-api-spec-sync.md
└── adr/
    └── NNNN-<slug>.md
```

| 폴더/파일 | 용도 |
|---|---|
| `service.md` | 서비스/제품 맥락 (단일 파일) |
| `rules/` | 반드시 지킬 규칙 (git, documentation 등) |
| `workflows/` | 공통·작업 유형별 필수 절차, 검증 기준, `work/` 문서 양식 |
| `work/` | 티켓별 논의·결정·구현·검증 기록 |
| `domains/` | 후속 작업에도 유지할 도메인 지식과 제약 |
| `adr/` | 의사결정 기록 (ADR, 1건 1파일) |
| `architecture/` | 설계 문서 자체 |
| `api/` | API 인벤토리와 계약/버전 정책 |
| `database/` | 데이터 인벤토리와 DB/Flyway/이관 정책 |
| `integrations/` | Redis, S3, OIDC, 크롤링 등 외부 연동 인벤토리 |
| `operations/` | 배포, 인프라, 관찰성, 컷오버 관련 운영 인벤토리 |
| `conventions/` | 프로젝트 기술 관례 (응답 포맷, API 규칙 등) |

어떤 문서 종류를 어느 폴더에 두는지, 폴더별로 문서를 쓸 때 지켜야 할 것은 [`docs/rules/documentation.md`](rules/documentation.md) §1·§2가 유일한 기준(single source of truth)이다. 폴더가 늘거나 용도가 바뀌면 그 문서와 아래 각 폴더 설명을 함께 갱신한다.

### `rules/`

예외 없이 지켜야 하는 규칙. "이렇게 하는 게 낫다"가 아니라 "지켜야 하는 것"만 담는다.

### `workflows/`

Jira 티켓 작업의 공통 절차와 작업 유형별 필수 절차를 관리한다. 모든 티켓은 `common.md`를 따르고, 신규 기능은 `new-feature.md`, 마이그레이션은 `migration.md`를 추가로 따른다.

### `domains/`

도메인 하나를 AI에게 맡길 때 필요한 모든 것(원본 위치, 대상 엔드포인트, 데이터 스키마, 불변식)을 한 파일로 제공해, 코드베이스를 처음부터 탐색하지 않아도 작업을 시작할 수 있게 한다.

### `adr/`

프로젝트 전반에 영향을 주는 결정과 그 이유를 append-only로 남겨, 나중에 "왜 이렇게 했더라"를 다시 논의하지 않게 한다.

### `architecture/`

특정 결정 하나가 아니라, 시스템/모듈이 지금 어떻게 설계돼 있는지에 대한 지속적인 참조 문서.

### `api/`

레거시와 신규 서버 사이의 API 목록, 사용 여부, `KEEP`/`REDESIGN`/`DROP`/`DEFER` 판정, 계약 보존 기준을 관리한다.

### `database/`

레거시/초안 데이터 객체 목록과 신규 DB 스키마/Flyway/데이터 이관 원칙을 관리한다. 실제 스키마 확정은 구현 슬라이스별 migration과 함께 진행한다.

### `integrations/`

Redis, S3, OIDC, 크롤링, 외부 API처럼 코드 밖 의존성을 목록화하고 이관/보류/폐기 판정을 관리한다.
애플리케이션 관점의 사용 위치, 인증/권한 방식, 장애 시 사용자 영향, fallback 여부를 적는다. 인스턴스 구성, 배포, 모니터링, 백업, 컷오버, rollback 같은 운영 제공 방식은 `operations/`에 둔다.

### `operations/`

EC2, MySQL, Redis, secret, 로그, 알람, 배포, 컷오버, rollback처럼 운영 전환에 필요한 항목을 관리한다.
외부 연동을 실제 환경에서 어떻게 제공·감시·복구·전환할지를 적는다. 해당 연동을 어떤 도메인/API가 왜 쓰는지는 `integrations/`에 둔다.

### `conventions/`

이 프로젝트가 실제로 쓰고 있는 기술적 패턴(응답 포맷, API 경로 규칙 등)을 서술한다. "지켜야 할 규칙"이 아니라 "지금 이렇게 되어 있다"는 사실 기록에 가깝다.

### `work/`

Jira 티켓 하나를 맡은 AI가 구현 방향 논의 결과를 정리해두는 문서. 컨텍스트를 공유하지 않는 리뷰어에게 "무엇을, 왜, 어디까지" 작업했는지 전달하는 역할을 한다 ([`common.md`](workflows/common.md) §5 참고).
사람과의 논의, 작업 범위, 선택지, 구현·검증 결과처럼 **티켓에 종속되는 기록**을 남긴다. 후속 작업에도 유지해야 할 도메인 경계·불변식·API/데이터/권한 제약·이관 상태가 확정되거나 바뀌면, 이 문서에만 남기지 않고 해당 `domains/` 문서도 갱신한다. 상세 배치 기준은 [`documentation.md`](rules/documentation.md)를 따른다.
