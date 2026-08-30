> 생성: 2026-08-31 01:05 · 최종 수정: 2026-08-31 01:05

# KD3-242 docs 하네스 후속 정비

| 항목 | 값 |
|---|---|
| Jira | `KD3-242` |
| 브랜치 | `docs/KD3-242-docs-harness-followup` |
| 상위 에픽 | `KD3-194` |

## 현재 제어점

- 활성 workflow: `008-docs`
- 현재 공통 단계: `5`
- 다음 결정 또는 전환 조건: 리뷰에서 폴더 경계(`conventions/` 부활)와 `docs-check` 검사 범위에 합의가 나오면 머지 준비

## 작업 목표

머지된 문서 체계(#6)를 실제로 써보며 나온 지적 네 가지를 반영한다 — 코드 작성 기준을 담을 자리 확보, 이름과 내용이 어긋난 문서 정리, 기획 요구사항 유실 방지, 문서 구조를 기계로 검증할 최소 장치.

## 작업 범위

**폴더 경계**
- `docs/conventions/` 신설. 코드를 쓸 때마다 참조하는 판단 기준을 둔다.
- `docs/architecture/common-response-error.md`를 분해해 `conventions/api-contract.md`(`Response<T>`)와 `conventions/error-handling.md`(`ErrorCode`·`BusinessException`·`GlobalExceptionHandler`)로 옮긴다.
- `docs/architecture/`는 시스템 전체 구조만 남긴다.

**이름 정리**
- `docs/rules/api-contract.md` → `docs/rules/api-migration.md`. 내용이 "레거시 계약 보존 정책"이고 `v0` 컷오버 후 폐기 대상이라, 영구 문서인 `conventions/api-contract.md`와 이름이 겹치지 않게 한다.

**기획 요구사항 인수**
- `workflows/005-new-feature.md` 1단계에 PRD(Notion)·화면지시서(Figma) 근거 확인과 화면별 상태 분기 확인 조건을 추가한다.
- `workflows/000-common.md` 2단계에 미결 질문 점검 조건("질문이 없는 것과 묻지 않은 것은 다르다")을 추가한다.

**공통 절차 선형성**
- `workflows/000-common.md`에 `단계 한눈에 보기` 표를 추가한다. 5단계 골격은 그대로 두고 각 단계 안의 순서만 드러낸다.
- 5단계에 리뷰 반복 종료 조건을 명시한다.

**기계 검증**
- `scripts/docs-check.mjs`와 `.github/workflows/docs-check.yml` 추가. 검사 3종 — 링크 무결성, 폴더 allowlist, `work/` 파일명 패턴.
- `#6`에서 깨진 ADR 링크 2건(`0003`, `0004` → `rules/workflow.md`)을 수정한다.
- `rules/documentation.md` §3에 "파일 이동으로 링크가 깨진 경우 경로만 수정한다" 예외를 추가한다. ADR append-only 규칙과의 충돌을 해소하기 위함이다.

**참조 갱신**: `docs/README.md`, `docs/rules/documentation.md`, `docs/inventory/api.md`

## 작업 제외 범위

- **`conventions/layering.md` 작성** — 계층별 책임과 정석형/실용형 판단 기준은 팀이 아직 정하지 않았다. 문서 자리만 만들고 내용을 먼저 쓰면 정해지지 않은 결정을 문서가 확정해버린다. 별도 티켓으로 분리한다.
- **`architecture/cutover.md` 작성** — 컷오버 전략은 신규 DB 전환(`0010`)으로 `0008`의 전제가 깨진 상태이고, 레거시 식별자 보존 여부·이관 스크립트 멱등성·`v0` 응답 호환 유지 여부가 미확정이다. `KD3-335`에서 다룬다.
- **인벤토리 판정 기준 4중복 제거** — `inventory/` 네 문서가 각자 `KEEP`/`REDESIGN`/`DROP`/`DEFER` 표를 갖고 있다. 대상별 해석이 실제로 달라서 통합 이득이 크지 않고, 이번 범위와 성격이 다르다.
- **PR 본문 섹션 검사·시간정보 헤더 검사** — `#6`의 `verify-documentation-evidence.yml`이 하던 PR 본문 섹션 존재 검사는 템플릿이 이미 헤더를 넣어주므로 통과 여부가 내용과 무관하다. 시간정보 헤더는 현재 위반이 0건이라 지금은 넣지 않는다.
- **코드 변경 일체** — 이번 작업은 문서와 CI 스크립트만 다룬다.

## 방향 논의 및 결정 사항

### 확정 사항

**`architecture/`와 `conventions/`는 "읽는 시점"으로 나눈다.** "무엇을 설명하는가"로 나누면 경계가 계속 모호해진다. `architecture/`는 프로젝트 구조를 파악할 때 한 번 읽고, `conventions/`는 코드를 작성·리뷰할 때마다 연다. `common-response-error.md`는 `Response<T>` 필드 제약, `ErrorCode` 배치 규칙, 예외 처리 우선순위처럼 코드를 쓸 때마다 참조해야 하는 내용이라 후자에 해당한다.

**`conventions/` 부활은 되돌리기가 아니다.** `#6`에서 이 폴더를 지운 이유는 Notion 도구 문서 2개만 남아 얇았기 때문이다. 코드 작성 기준이 들어오면서 폴더의 실질이 생겼다. ADR `0009`가 이 폴더를 만들며 명시한 용도("응답 포맷, API 경로 규칙 등")와도 일치한다.

**검토한 대안**: 코드 컨벤션을 `rules/` 하위에 두는 안. `rules/`가 문서 규칙·git·마이그레이션 정책·Notion 연동·코드 컨벤션이 뒤섞인 9개짜리 폴더가 되어 배제했다.

**CI는 대조로 판정 가능한 것만 검사한다.** "문서가 올바른 폴더에 있는가"는 판단이라 기계로 검증할 수 없다. "정의되지 않은 위치에 만들었는가"는 대조라 검증할 수 있다. 후자만 검사한다. 검사 3종은 실제로 위반을 만들어 실패를 확인했고, 현재 리포는 통과한다.

**링크 검사를 넣는 이유**: `#6`이 실제로 링크 2건을 깨뜨렸고 리뷰에서 아무도 잡지 못했다. AI 하네스는 `AGENTS.md` → `docs/README.md` → 개별 문서 링크 계층으로 서 있어서, 깨진 링크는 곧 "문서를 찾지 못함"이다.

### 미결 질문

- `KD3-242`는 이미 `완료` 상태다. 이 후속 작업을 같은 키로 묶을지, 새 티켓을 열지 확인이 필요하다.
- `conventions/`에 앞으로 둘 문서 목록(`layering`, `validation`, `persistence`, `transaction`, `testing`)과 작성 시점을 합의해야 한다. 첫 도메인 마이그레이션 착수 전에 필요한 것과 진행하며 확정할 것을 나누는 판단이 남아 있다.
- `docs-check`에 시간정보 헤더 갱신 검사를 넣을지. 지금은 위반이 0건이라 보류했다.

### 사용자 승인 기록

- 2026-08-30 — 폴더 통합 방향(`inventory/` 단일화, policy를 `rules/`로) 합의. `#6`에 반영됨.
- 2026-08-31 — 위 작업 범위 승인. `api-contract.md` 개명과 `conventions/` 신설은 사용자가 명시적으로 위임했다.

## 완료 확인 기준

- `node scripts/docs-check.mjs`가 통과한다. (검사 3종 전부, 문서 36개)
- 검사 3종이 실제 위반에서 실패한다 — 깨진 링크·미정의 폴더·잘못된 `work/` 파일명을 각각 만들어 `exit 1`을 확인했다.
- `api-contract`를 참조하던 곳(`inventory/api.md`, `rules/documentation.md`, `docs/README.md`)이 전부 갱신되어 참조가 남지 않는다.
- `docs/README.md` 폴더 트리와 실제 파일 목록이 일치한다.
- 코드 변경이 없으므로 빌드·테스트 영향은 없다.

## 작업 후 확인 목록

- `docs/README.md` — **갱신**. 폴더 트리에 `conventions/` 추가, `architecture/common-response-error.md` 제거, `api-migration.md` 개명 반영. 문서 지도에 `conventions/` 행과 `architecture/`와의 경계 설명 추가, 읽는 순서 4번 추가.
- `docs/rules/documentation.md` — **갱신**. §1 배치표에 "코드 작성 기준" 행 추가, §2에 `conventions/` 절 추가와 `architecture/` 절 범위 축소, §3에 링크 경로 수정 예외 추가.
- `docs/inventory/api.md` — **갱신**. `rules/api-contract.md` → `rules/api-migration.md` 참조 수정.
- `docs/adr/0003`, `docs/adr/0004` — **갱신**. 죽은 `rules/workflow.md` 경로만 `workflows/000-common.md`로 수정. 본문은 고치지 않았다.
- `docs/domains/auth.md` — 확인했지만 변경 없음. 이번 변경 대상 문서를 참조하지 않는다.
- Notion API 명세 — 해당 없음. API 변경이 없다.
- `docs/adr/` 신규 — 해당 없음으로 판단. `conventions/` 부활은 `0009`가 정한 폴더 용도를 되살리는 것이라 새 결정으로 보지 않았다. 리뷰에서 이견이 있으면 ADR로 올린다.
