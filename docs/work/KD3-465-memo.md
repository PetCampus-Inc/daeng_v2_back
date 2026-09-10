> 생성: 2026-09-08 11:00 · 최종 수정: 2026-09-10 12:30

# KD3-465 — 메모 기능 이관 (자유메모 · 상담 체크리스트)

| 항목 | 값 |
|---|---|
| Jira | `KD3-465` (하위 작업) |
| 브랜치 | `feat/KD3-465-memo` (단일 브랜치 · 단일 PR). 자유메모/사진/체크리스트를 한 브랜치에 구현하고 아래 `## 구현 순서`대로 커밋을 나눈다 |
| 상위 에픽 | `KD3-403`(Epic, [리팩토링] daeng_v1_back → daeng_v2_back). 중간 작업 `KD3-272`(Task, 유치원 도메인 마이그레이션) — git 브랜치 `epic/KD3-272-kindergarten-features` |

## 현재 제어점

- 활성 workflow: `003-migration` (+ `005-new-feature` 일부 — 화면지시서 기반 신규 UX 요소가 있어 두 유형 조건을 함께 적용)
- 현재 공통 단계: `5`(독립 리뷰·PR·문서 동기화). 구현(3) 완료 — `## 구현 순서` A·B·C·D 전부, `./gradlew clean build` green(ArchUnit 포함). 문서 동기화 완료. [PR #27](https://github.com/PetCampus-Inc/daeng_v2_back/pull/27) (`feat/KD3-465-memo` → `epic/KD3-272-kindergarten-features`) 생성.
- 다음 결정 또는 전환 조건: 리뷰(CodeRabbit + 사람) 반영 → 머지. 사람 몫: Q6 로컬 응답 대조, Notion 명세 등록, 배포 컨테이너 `TZ=Asia/Seoul` 확인, 프론트 v1 전환.
- 베이스: `epic/KD3-272-kindergarten-features` = `origin/dev`(`ff71aaf`). 유치원 스키마 + media(S3, KD3-478) + 응답 날짜포맷 컨벤션(KD3-495) 전부 포함. KD3-478은 dev에 squash 머지돼서 예전에 epic에 직접 머지했던 커밋은 제거하고 dev 기준으로 다시 맞췄다.

## 작업 목표

레거시(`daeng_v1_back` = `knockdog_server`)의 `memo` 패키지 — 보호자가 유치원을 탐색·비교하며 남기는 **자유메모**와 **상담시 체크리스트** — 를 신규 코틀린 서버로 이관한다. 이관 후 신규 서버는:

- `memo` 도메인이 `/api/v1/memos/**` · `/api/v1/checklists/**` 로 자유메모 조회/저장, 체크리스트 템플릿 조회, 체크리스트 답변 조회/저장을 제공하고,
- 저장 구조는 레거시 8개 테이블(`free_memo`, `free_memo_photo`, `checklist_template`/`section`/`question`/`question_option`, `checklist_submission`, `checklist_answer`)을 신규 3개 테이블 + 정적 리소스 1개로 재설계한다.

담당 사용자 흐름([`docs/service.md`](../service.md) §1): "v2.0까지의 보호자 유치원 탐색·비교 경험"(`kindergarten` 도메인 주변 기능). §5 흐름도는 원장 중심이라 보호자 탐색 경험 노드가 명시돼 있지 않다 — 아래 `작업 후 확인 목록` 참고.

## 작업 범위

### 이관 대상 — 판정 (근거: [`docs/inventory/api.md`](../inventory/api.md) L247~253, 프론트 `daeng_v2_front` 호출 코드 대조)

| 레거시 (`daeng_v1_back`) | 신규 서버 (`v1`) | 계약 판정 | 비고 |
|---|---|---|---|
| `GET /api/v0/memo?targetId=` | `GET /api/v1/memos/{targetId}` | `KEEP`(응답 내용) + 경로·엔벨로프 재설계 | 유치원별 자유메모 1건 조회 |
| `POST /api/v0/memo?targetId=` | `PUT /api/v1/memos/{targetId}` | `KEEP`(요청·결과) + `POST`→`PUT`(멱등 upsert) | 자유메모 저장 |
| `GET /api/v0/memo/shops` | `GET /api/v1/memos` | `KEEP` + 재설계 | 내 메모 요약 목록. 프론트가 유치원 목록·지도·상세 카드에 "이 유치원에 내 메모 있음 + 날짜 + 내용" 배지로 결합(`useKindergartenMainQuery`, `kindergarten-map`). 전용 화면 아님 |
| `GET /api/v0/memo/checklist` | `GET /api/v1/checklists/template` | `KEEP` + 재설계 | 상담 체크리스트 템플릿(정적, 단일 `registration`/`ko-KR`) |
| `POST /api/v0/memo/checklist?targetId=` | `PUT /api/v1/checklists/{targetId}` | `KEEP`(요청) + `POST`→`PUT` | 체크리스트 답변 저장 |
| `GET /api/v0/memo/checklist/answer?targetId=` | `GET /api/v1/checklists/{targetId}` | `KEEP`(응답 구조) + 재설계 | 유치원별 내 체크리스트 답변 |
| `GET /api/v0/memo/list` | — | `DROP` | ADR 0004 삭제 확정(프론트 호출 0건). `memo`, `memo/shops`만 사용 |

`{targetId}` = `kindergartens.naver_place_id` (레거시 `targetId`는 유치원 placeId였다). `manual_` 접두사 유치원 포함.

### 신규 스키마 (Flyway — REDESIGN, [`docs/inventory/database.md`](../inventory/database.md) `free_memo`/`free_memo_photo` 행)

```
memos
  id           BIGINT PK AUTO_INCREMENT
  user_code    VARCHAR(...)   -- 작성자 (토큰 subject = @AuthenticationPrincipal 문자열)
  target_id    VARCHAR(100)   -- kindergartens.naver_place_id
  content      TEXT NULL      -- 2000자 이내(애플리케이션 검증)
  created_at / updated_at / deleted_at   -- BaseEntity
  UNIQUE (user_code, target_id)

memo_photos
  id           BIGINT PK
  memo_id      BIGINT
  object_key   VARCHAR(...)    -- media commit 후 영구 key ("memo/{userCode}/{filename}")
  sort_order   INT             -- photoKeys 배열 순서
  created_at                   -- BaseEntity 미상속 (전량 교체, user_agreements 선례)
  UNIQUE (memo_id, object_key)

checklist_submissions
  id                BIGINT PK
  user_code         VARCHAR(...)
  target_id         VARCHAR(100)
  template_version  VARCHAR(50)   -- 답변한 시점 템플릿 버전
  answers           JSON          -- {"q_vaccine_proof_required":"YES", "q_max_dogs_per_day":"30", ...}
  created_at / updated_at / deleted_at
  UNIQUE (user_code, target_id)
```

- FK 제약 없음([`docs/conventions/jpa-entity.md`](../conventions/jpa-entity.md) §3). `target_id`/`user_code`는 값만 저장.
- 체크리스트 템플릿은 **DB 테이블 없음** — `src/main/resources/checklists/registration.ko-KR.json` 정적 파일 1개가 (a) 템플릿 응답 (b) 제출 검증 (c) 답변 조회 시 문항 라벨 채우기에 모두 쓰인다.
- 체크리스트 답변은 **개별 answer 테이블 없음** — 항상 한 (유저, 유치원) 단위로 통째로 읽고 쓰므로 `answers` JSON 컬럼. 통계성 조회가 필요해지면 그때 JSON→테이블 백필.

### `memo` 도메인 (정석형 헥사고날 — ADR 0003)

```
domain/memo/
  domain/
    FreeMemo.kt                       -- 애그리게잇 1 (userCode, targetId, content, photos). require 불변식
    MemoPhoto.kt / MemoId.kt
    ChecklistSubmission.kt            -- 애그리게잇 2 (userCode, targetId, templateVersion, answers)
    ChecklistTemplate.kt / ChecklistSection.kt / ChecklistQuestion.kt   -- 순수 VO (정적 템플릿 표현)
    ChecklistQuestionType.kt          -- TRI_STATE, INTEGER (레거시 QuestionType 7종 중 실사용 2종만)
                                         값 정규화·검증은 ChecklistQuestion.normalize(raw): String? (null=무효)
  application/
    MemoErrorCode.kt
    port/input/
      GetFreeMemoUseCase.kt / SaveFreeMemoUseCase.kt / GetMemoedKindergartensUseCase.kt
      GetChecklistTemplateUseCase.kt / GetChecklistAnswersUseCase.kt / SaveChecklistAnswersUseCase.kt
    port/output/
      LoadFreeMemoPort.kt / SaveFreeMemoPort.kt
      LoadChecklistSubmissionPort.kt / SaveChecklistSubmissionPort.kt
      LoadChecklistTemplatePort.kt          -- 정적 템플릿 로드
      MemoPhotoStoragePort.kt               -- 사진 commit / 조회 URL (media 위임)
    service/                                -- FreeMemoService, ChecklistService
  adapter/
    inbound/web/   (유스케이스별 컨트롤러 분리 — hexagonal.md §1)
      FreeMemoController.kt / MemoListController.kt
      ChecklistTemplateController.kt / ChecklistAnswerController.kt
    outbound/
      persistence/  JPA 엔티티 · Repository · PersistenceAdapter (매퍼는 확장함수)
                    ChecklistAnswersJsonConverter (Map<String,String> ↔ JSON)
      template/     ChecklistTemplateResourceAdapter.kt  (resources/checklists/*.json 파싱)
      media/        MediaMemoPhotoStorageAdapter.kt      (media UseCase 위임)
```

- `HexagonalArchitectureTest` 규칙 4는 `domain.*.domain..` 와일드카드라 `memo.domain` 자동 포함(KD3-478에서 확인됨 — hexagonal.md §3의 stale 문구는 KD3-478이 정정).

### 계약 결정 (v1 재설계 — ADR 0012)

| # | 항목 | 결정 |
|---|---|---|
| C1 | 응답 엔벨로프 | 전부 `global/response/Response.kt` (`{status, code, message, data}`). 레거시의 raw DTO 3개 + `BasicInfoResponseDto` 3개 혼재를 통일 |
| C2 | 성공 응답 | `Response.success(data)` — `code: "SUCCESS"`. 레거시 커스텀 성공 코드(`MEMO_SAVED`, `CHECKLIST_SAVED` 등)·메시지 폐기 (프론트가 성공 시 `data`만 씀) |
| C3 | 오류 | `domain/memo/application/MemoErrorCode.kt` enum 신규 ([`error-handling.md`](../conventions/error-handling.md)). `code` 문자열은 레거시(`CHECKLIST_*`)와 맞추지 않는다 — 프론트가 `data.success`(boolean)/`data.message`만 보고 `code`로 분기하지 않음(프론트 `answers.tsx`·`getMemo.ts`·`questions.tsx` 대조) |
| C4 | 메모 텍스트 upsert | `memos` `(user_code, target_id)` 유니크 1행. `PUT /api/v1/memos/{targetId}` body `{content}` — **content만** upsert(사진 무관, C15). **원자적** — 네이티브 `INSERT ... ON DUPLICATE KEY UPDATE`(동시 최초 저장 race 제거, CodeRabbit #6·#7). **레거시의 "매 저장 새 row + 히스토리" 폐기** (`GET /memo/list`는 `DROP`) |
| C5 | 자유메모 빈 상태 | `GET`에서 200 + `{ content: null, photos: [] }` (레거시 parity) |
| C6 | 날짜 필드 | 단건 조회(`GET /api/v1/memos/{targetId}`)에는 날짜 필드 **없음** (프론트 `MemoResponse`가 `content`·`photos`만 읽음). 목록(`GET /api/v1/memos`)의 `memoDate`는 도메인/DTO에서 `LocalDate` 타입으로 두면 전역 컨벤션(KD3-495, `api-contract.md §2`)이 `"2026-09-08"`로 직렬화 — 수동 포맷팅 없음. 프론트 `entities/kindergarten/model/mappers.ts`가 `.replace(/-/g, '.')`로 표시 포맷을 만들므로 대시 ISO가 그대로 맞다. `memoDate`의 원본은 `memos.updated_at`의 날짜 부분 |
| C7 | 체크리스트 답변 저장 | `PUT` = 전체 교체 (제출된 answers가 그 submission의 전부, 빠진 문항은 삭제). required 검증 없음 (템플릿에 `required:true` 문항이 없음) — 부분 제출 허용 |
| C8 | 체크리스트 답변 `value` 타입 | 응답에서 **항상 문자열**. 레거시는 INTEGER를 숫자로 내려 프론트 타입(`value: string`)과 어긋났음 — 의도적 교정 |
| C9 | 체크리스트 빈 상태 | `GET`에서 200 + `{ sections: [] }`. **레거시는 `fail("CHECKLIST_NOT_FOUND")` 실패 응답을 내려 프론트가 throw함 — 의도적 교정** |
| C10 | 체크리스트 템플릿 라벨 | 문항 ID(`q_vaccine_proof_required` 등)는 레거시와 100% 동일(프론트 `QUESTION_MAP` 계약). 라벨·섹션 제목은 화면지시서(Figma) 기준으로 최신화 — 레거시 JSON과 차이: "강아지 성향 관리"→"강아지 맞춤 관리", "몇 마리까지 등록"→"등원", "가능한가요"→"가능할까요" 등 |
| C11 | INTEGER 문항 검증 | `q_max_dogs_per_day` `{min:0, max:500}` 유지 |
| C12 | 인증 | `/api/v1/memos/**`, `/api/v1/checklists/**` **전부 인증 필수** (SecurityConfig, ADR 0007 기본 deny). **레거시는 `GET /memo/checklist`(템플릿)만 공개였음 — 인증으로 변경** |
| C13 | 작성자 식별자 | `user_code` 문자열(`@AuthenticationPrincipal`이 주는 토큰 subject) 직접 저장. 레거시는 user PK(BIGINT) 저장이었으나 memo가 PK를 쓸 일이 없어 auth 도메인 의존을 제거 |
| C14 | 유치원 존재 검증 | **하지 않는다.** memo 응답에 유치원 데이터가 하나도 없어(이름·요금 등 없음) 크로스 도메인 의존이 불필요. 잘못된 `targetId`로 저장돼도 무해(FK 없음), 조회는 빈 응답. 가드가 필요해지면 후속으로 memo 자체 outbound 포트 추가 |
| C15 | 사진 첨부 | **이 티켓 포함** (2026-09-08). **텍스트와 독립된 기능** (2026-09-10 사용자 지시): `memo_photos`는 `memos` row에 매이지 않고 `(user_code, target_id)` 직접 키. 개별 추가/삭제, 유치원당 최대 5장. 프론트가 `POST /api/v1/media/upload-urls` `{purpose:"MEMO_ATTACHMENT"}` → `tmp/{userCode}/MEMO_ATTACHMENT/…` key → `POST /api/v1/memos/{targetId}/photos` `{photoKey}`. memo가 `MemoPhotoStoragePort`(→ media `CommitObjectUseCase`)로 commit(`MediaPurpose.MEMO_ATTACHMENT` → `memo/{userCode}/{filename}`), `memo_photos`에 `sort_order = max+1`로 insert. tmp 아니면 400. `DELETE /api/v1/memos/{targetId}/photos/{photoId}` = 소유자 확인 후 DB row + S3 object(`ObjectStoragePort.delete`) 제거, 아니면 404(`MEMO_PHOTO_NOT_FOUND`). `GET /api/v1/memos/{targetId}`가 텍스트와 사진을 합쳐 반환 |

### API 계약 요약

모든 응답은 `Response<T>`로 감싼다. 아래는 `data` 내부.

| 메서드·경로 | 요청 | 응답 `data` | 인가 |
|---|---|---|---|
| `GET /api/v1/memos/{targetId}` | — | `{ content: string\|null, photos: [{ id, key, url }] }` (텍스트 + 사진 합침) | 인증 |
| `PUT /api/v1/memos/{targetId}` | `{ content?: string }` | 갱신된 표현 (`GET`과 동일 형태) | 인증 |
| `POST /api/v1/memos/{targetId}/photos` | `{ photoKey: string }` (tmp key) | `{ id, key, url }` | 인증 |
| `DELETE /api/v1/memos/{targetId}/photos/{photoId}` | — | `null` | 인증 |
| `GET /api/v1/memos` | — | `{ memos: [{ shopId, content, memoDate }] }` — `shopId`=targetId, `memoDate`=`YYYY-MM-DD`. 프론트가 `shopId`로 유치원 카드에 조인 | 인증 |
| `GET /api/v1/checklists/template` | — | `{ template: { code, version, locale, title }, sections: [{ id, title, questions: [{ id, label, type }] }] }` | 인증 |
| `GET /api/v1/checklists/{targetId}` | — | `{ sections: [{ sectionId, title, answers: [{ questionId, question, value }] }] }` (없으면 `{ sections: [] }`) | 인증 |
| `PUT /api/v1/checklists/{targetId}` | `{ answers: [{ questionId, value }] }` | 갱신된 표현 (`GET`과 동일 형태) | 인증 |

- 저장(`PUT`) 응답 body는 프론트가 읽지 않는다(대조 결과) — RESTful 관례상 갱신된 표현을 반환하되, 프론트는 저장 후 쿼리 무효화로 재조회한다.

## 커밋 이력

단일 브랜치 `feat/KD3-465-memo` → 단일 PR. `Refs: KD3-465`. 커밋 범위 `ff71aaf..HEAD`.

| 커밋 | 범위 | Flyway |
|---|---|---|
| A. 자유메모 v1 이관 (`b0cc1c6`) | `domain/memo/` 정석형, `Memo` 애그리게잇, `MemoErrorCode`, persistence. `GET`·`PUT /api/v1/memos/{targetId}`, `GET /api/v1/memos` | `V10` |
| media `MEMO_ATTACHMENT` (`151b9ef`) | `MediaPurpose`에 enum 값 추가 (→ `memo/{userCode}/`) | — |
| B. 메모 사진 (`a339900`, 후 `81aa380`에서 재설계) | `memo_photos` 테이블. `MemoPhotoStoragePort` + `MediaMemoPhotoStorageAdapter` | `V11` |
| C. 상담 체크리스트 (`7c4b2f1`) | `ChecklistSubmission`, `ChecklistTemplate` VO, `LoadChecklistTemplatePort` + 리소스 어댑터, `registration.ko-KR.json`. `GET /api/v1/checklists/template`, `GET`·`PUT /api/v1/checklists/{targetId}` | `V12` |
| D. 문서 동기화 (`3da954a`) | `docs/domains/memo.md` 신설, 인벤토리 갱신 |
| 독립 리뷰 반영 (`e1686b6`) | KDoc 제거·`updated_at` touch·photoKeys 선검증 |
| CodeRabbit 문서 지적 (`b93b873`) | api.md 열 수·진척, database.md DROP 진척 |
| **`FreeMemo` → `Memo` 리네임 (`bfc838e`)** | 신규 서버 식별자 전부 (2026-09-10 사용자 지시) |
| **메모 텍스트/사진 분리 (`81aa380`)** | `memo_photos` `(user_code, target_id)` 직접 키. `POST`/`DELETE /api/v1/memos/{targetId}/photos` 신설. `MemoPhotoService`·`MemoPhotoController` 추가 (2026-09-10 사용자 지시) |
| **원자적 upsert (`5fb9ccd`)** | `memos`·`checklist_submissions` 네이티브 `ON DUPLICATE KEY UPDATE`. `answers` JSON→TEXT. `MemoUpsertConcurrencyTest` (CodeRabbit #6·#7) |

- 도메인 구조의 최신·정확한 기준은 [`docs/domains/memo.md`](../domains/memo.md) §3.
- ArchUnit은 `domain.*.domain..` 와일드카드라 `memo.domain` 자동 포함.
- PR: [#27](https://github.com/PetCampus-Inc/daeng_v2_back/pull/27) `feat/KD3-465-memo` → `epic/KD3-272-kindergarten-features` (squash merge). `epic` → `dev`는 일반 merge (git.md §2).

## 작업 제외 범위

- **레거시 `free_memo` 히스토리 동작**(매 저장마다 새 row, `findFirst...OrderByCreatedAtDesc`로 최신 조회) — upsert 1행으로 대체(C4).
- **실데이터 이관** (레거시 운영 DB의 `free_memo`/`checklist_submission` → 신규 DB) — ADR 0010에 따라 최후순위. 이번은 코드·스키마·로컬 검증까지.
- **프론트(`daeng_v2_front`) v1 전환** — `src/entities/memo/*`, `src/entities/checklist/*`, `src/features/checklist/*`의 경로·엔벨로프·타입 수정은 별도 작업(ADR 0012 전제).
- **레거시 `v0` memo 엔드포인트 제거** — 컷오버까지 레거시 서버가 계속 서비스(ADR 0012).
- **rollback 설계** — 유치원 도메인과 동일 방침(ADR 0011: 완전 분리 + 단발 전환, 즉시 롤백 요구 안 함).
- **레거시 미사용 값 재현** — `checklist_answer`의 `value_boolean`/`value_json`, `QuestionType` 중 `BOOLEAN`/`TEXT`/`SELECT`/`MULTI_SELECT`/`DATE`, `question_option` 테이블, `ChecklistAdminService`(관리자 템플릿 CRUD), `ChecklistSeeder`.
- **관리자용 체크리스트 템플릿 편집·유치원별 템플릿** — 계획 없음(YAGNI). 템플릿은 정적 리소스 1개.
- **`GET /api/v0/memo/list`** — `DROP`.
- **S3 orphan object 정리** — 사진 재편집으로 참조가 끊긴 영구 object의 S3 삭제 주체는 [`integrations.md`](../inventory/integrations.md) 미결. 이번은 DB row만 정리, S3 object는 방치.
- **서버 전체 날짜/시간 포맷 통일** — 이미 KD3-495(`api-contract.md §2`, dev 반영)로 확정됨. memo는 `memoDate`를 `LocalDate` 타입으로 두면 컨벤션이 `"YYYY-MM-DD"` 직렬화(C6). 이 티켓에서 추가로 할 일 없음.

## 방향 논의 및 결정 사항

### 확정 사항

- 브랜치 전략: `epic/KD3-272-kindergarten-features`(dev + KD3-478 media 머지) → `feat/KD3-465-memo`. KD3-272의 남은 하위작업(KD3-459 지도, KD3-466 원장인증, KD3-469 비교, KD3-470 북마크)도 이 epic 브랜치 위에서 진행. (사용자 승인 2026-09-07)
- 버전: `v1` 단독, `v0` 미생성 (ADR 0012). (사용자 승인 2026-09-08)
- 스키마: 적극 재설계 — 레거시 8테이블 → 신규 3테이블 + 정적 리소스. (사용자 승인 2026-09-08)
- 위 `계약 결정` 표 C1~C15. (사용자 승인 2026-09-08)
- 사진 첨부 이 티켓 포함, 서버측 commit 방식(C15). (사용자 확정 2026-09-08)
- 도메인 구조: 단일 `memo` 도메인, 애그리게잇 2개. (사용자 승인 2026-09-08)
- media(S3) 인프라를 epic 브랜치에 선반영. (사용자 지시 2026-09-08)

### 프론트(`daeng_v2_front` `develop`) 대조 결과 (2026-09-08)

| 항목 | 확인 내용 |
|---|---|
| `GET /api/v1/memos` 유치원 이름 | **불필요.** `entities/kindergarten/model/mappers.ts`가 `new Map(memo.map(m => [String(m.shopId), m]))`로 `shopId`(=targetId) 조인 후 유치원 카드에 결합. 응답은 `{ shopId, content, memoDate }`면 충분. C14(유치원 의존 없음) 유지 |
| 목록 `memoDate` 포맷 | 프론트가 `memoDate.replace(/-/g, '.')` → ISO date `YYYY-MM-DD` 필수(C6) |
| 체크리스트 라벨 하드코딩 | **아니오.** `features/checklist/ui/ChecklistEditor.tsx`가 API의 `section.title`·`question.label`을 그대로 렌더. 화면지시서 기준 라벨 최신화가 편집 화면에 그대로 반영됨(C10). `QUESTION_MAP`(3개 축약 라벨)은 상세 탭 요약 칩 전용 프론트 표시이고 미완성 — 프론트 소관 |
| 체크리스트 문항 ID | 프론트 `features/dog-school/api/checklist-edit.api.ts`(구 mock 경로, `@TODO`)가 13개 ID를 하드코딩. `q_max_dogs_per_day` 포함. ID 절대 불변 |
| INTEGER 입력 상한 | 프론트 `ChecklistEditor`가 `value.length <= 2`로 2자리(99)까지만 입력. 백엔드 검증은 `max:500`(C11) — 백엔드가 더 관대, breaking 아님. 프론트 상한과 불일치는 프론트 소관 |
| TRI_STATE 값 | 프론트 라디오 `value={key}` where key ∈ `YES`/`NO`/`UNKNOWN` (`vaccinationOptions`). 우리 값과 일치 |
| 저장 응답 body | `updateMemo`·`updateAnswers` 결과를 프론트가 읽지 않음(`onSuccess`가 `data` 무시). 저장 후 쿼리 무효화로 재조회. 저장 응답 형태는 자유(C-요약: 갱신된 표현 반환) |
| 사진 조회 URL | 프론트 `FreeMemoSection`이 응답의 `photos[].url`을 무시하고 `${NEXT_PUBLIC_IMAGE_BASE_URL}${photo.key}`(공개 CDN base)로 직접 조합. `url` 필드는 계약상 유지하되(media presigned) 프론트 현재 구현은 key만 사용 |
| 사진 업로드 흐름 | 프론트 현재: `getUploadImage`(레거시) 업로드 → 저장 시 `useMoveImageMutation`(레거시 `/s3/image/move`, `path=kindergarten/{id}/memo/{userId}`)로 client측 이동 → 이동된 key를 `photoKeys`로 전달. v1 전환 시 서버측 commit(C15)으로 단순화 — 프론트 작업 |

### 미결 질문

| # | 질문 | 현재 상태 |
|---|---|---|
| Q6 | 로컬 응답 대조(레거시 v0 vs 신규 v1) | `KEEP` 항목이라 003-migration §4 절차 필요. 레거시 서버 로컬 기동이 막히면(다른 워크트리 Flyway 체크섬 이슈 등) 사람이 별도 수행. 검증(4) 단계 항목 |

### 사용자 승인 기록

- 2026-09-07 — epic 브랜치 `epic/KD3-272-kindergarten-features` 생성, KD3-478 media 머지 지시.
- 2026-09-08 — v1 경로 초안 승인 ("v1 경로 좋아"). 크로스 도메인(C14 검증 안 함), 템플릿 정적 서빙+JSON 저장(C7·C10), 인증 전면(C12), ErrorCode(C3)·성공응답(C2) 컨벤션 확인, 도메인 구조 7a 승인.
- 2026-09-08 — 사진 첨부 이 티켓 포함(C15), Q2/Q4/Q5 프론트 대조 지시.
- 2026-09-09 — 3-PR 분리안 철회, **단일 브랜치 `feat/KD3-465-memo` + 단일 PR**로 전체(자유메모+사진+체크리스트) 구현 후 PR ("2번이지"). 베이스는 최신 `dev`(media·날짜컨벤션 포함).

## 완료 확인 기준

- [x] `GET /api/v1/memos/{targetId}`: 인증 없으면 401, 메모 없으면 200 + `{content:null, photos:[]}`, 있으면 content + photos(`{id,key,url}`) 합쳐서 반환 — `MemoEndpointsTest`, `MemoServiceTest`.
- [x] `PUT /api/v1/memos/{targetId}`: content만 upsert, `content` 2000자 초과 400(`MEMO_CONTENT_TOO_LONG`), 동시 최초 저장도 원자적 — `MemoPersistenceAdapterTest`, `MemoUpsertConcurrencyTest`, `MemoEndpointsTest`.
- [x] `GET /api/v1/memos`: 텍스트 있는 유치원 목록 `{shopId, content, memoDate}`, `updated_at`·`id` 내림차순 — `MemoPersistenceAdapterTest`, `MemoServiceTest`.
- [x] 사진 추가·삭제: `POST .../photos` tmp key만 받아 media commit → `memo/{userCode}/` (그 외 400 `MEMO_INVALID_PHOTO_KEY`), 6장째 400(`MEMO_TOO_MANY_PHOTOS`). `DELETE .../photos/{id}` 소유자 확인 후 DB row + S3 object 제거, 남의 것 404(`MEMO_PHOTO_NOT_FOUND`) — `MemoPhotoServiceTest`, `MemoPhotoPersistenceAdapterTest`, `MemoPhotoEndpointsTest`. `MediaPurpose.MEMO_ATTACHMENT` — `MediaPurposeTest`, `MediaEndpointsTest`.
- [x] `GET /api/v1/checklists/template`: 5섹션·13문항, 문항 ID가 레거시·프론트 목록과 일치, 인증 필요 — `ChecklistTemplateResourceAdapterTest`, `ChecklistEndpointsTest`.
- [x] `PUT /api/v1/checklists/{targetId}`: 모르는 questionId 400(`MEMO_INVALID_CHECKLIST_ANSWER`), TRI_STATE·INTEGER(0~500) 검증, 전체 교체 — `ChecklistServiceTest`, `ChecklistEndpointsTest`.
- [x] `GET /api/v1/checklists/{targetId}`: 저장 없으면 200 + `{sections:[]}`, 템플릿 순서대로, `value` 항상 문자열 — `ChecklistServiceTest`, `ChecklistEndpointsTest`.
- [x] `HexagonalArchitectureTest` 통과 — `memo.domain` 와일드카드 포함. 도메인 모델이 `MemoErrorCode`에 의존하지 않도록 값 검증을 `require`(도메인) + `BusinessException`(서비스)로 분리.
- [x] `./gradlew clean build` green — ktlint + ArchUnit + 전체 테스트.
- [ ] **로컬 응답 대조 (사람 몫)**: `KEEP` 6개 엔드포인트 — 레거시 `v0` 응답의 `data` 내부 필드와 신규 `v1` 대조. 경로·엔벨로프·아래 `계약 parity`의 의도적 차이는 제외. 미실행(로컬 레거시 기동 필요).
- [ ] **Notion API 명세 등록 (사람 몫)**: v1 memo/checklist 6개 엔드포인트 ([`docs/rules/notion-api-spec-sync.md`](../rules/notion-api-spec-sync.md)).
- [ ] **배포 컨테이너 `TZ=Asia/Seoul` (사람 몫)**: `memoDate`가 KST 날짜로 나오려면 필요(KD3-495 전제).

### 리뷰 반영

**독립 리뷰 (2026-09-09, 컨텍스트 미공유 에이전트)** — "머지 가능, 블로킹 없음". 파리티·헥사고날·프론트 계약·날짜 컨벤션·인가 OK. 반영(`e1686b6`): `MemoErrorCode` KDoc 제거, `save` 시 `updated_at` touch, photoKeys 선검증.

**CodeRabbit (PR #27, actionable 9)** — 문서 5건 반영(`b93b873`): api.md 열 수(MD056)·진척 `완료`→`진행중`, database.md DROP 진척 `해당없음`, work doc 사진 key 경로·코드블록. 코드 4건: #6·#7(동시 최초 저장 race) → 원자적 upsert(`5fb9ccd`). #9(보조 정렬 없음) → `ORDER BY updated_at DESC, id DESC`. #8(사진 commit이 DB 저장 전 → tx 실패 시 orphan) → 사진이 개별 기능으로 분리되며 커밋 단위가 사진 1장 단위로 축소됨(영향 감소), S3 orphan 정리 주체는 여전히 미결로 문서화.

**2026-09-10 사용자 지시 반영**: `FreeMemo`→`Memo` 리네임(`bfc838e`), 메모 텍스트/사진 완전 분리(`81aa380`), 원자적 upsert 방식은 JDBC 네이티브 `ON DUPLICATE KEY UPDATE`(`5fb9ccd`).

### 계약 parity (003-migration §4)

- `KEEP` 엔드포인트(응답 내용) — 레거시와 `data` 내부 필드 대조 대상.
- 의도적 차이(대조 실패 아님): C1(엔벨로프 통일), C2(성공 코드), C4(메모 텍스트·사진 분리 — 레거시 `POST /memo`의 `{content, photoKeys}`가 `PUT /memos/{id}` + `POST`/`DELETE /memos/{id}/photos`로), C6(단건 응답 날짜 필드 없음), C8(`value` 문자열화), C9(빈 체크리스트 200 vs 레거시 실패 응답), C12(템플릿 인증화).
- `GET /api/v0/memo/list`는 `DROP` — 대조 제외.

## 작업 후 확인 목록

| 문서 | 판정 | 결과 |
|---|---|---|
| `docs/domains/memo.md` | 신설함 | 경계·불변식, v1 6개 엔드포인트 매핑, 스키마(`memos` V10 / `memo_photos` V11 / `checklist_submissions` V12 + 정적 템플릿), 구조, media 의존, 레거시 대비 의도적 차이 |
| `docs/inventory/api.md` | 갱신함 | memo 6개 행: 진척 `미착수`→`진행중`(로컬 응답 대조 미완료), `대상 버전` `v1`, 근거·후속 확인 열 분리(MD056), 신규 경로·KD3-465 링크·의도적 차이. 진척 카운트 표(7/9/106/137)·설명 갱신 |
| `docs/inventory/database.md` | 갱신함 | `free_memo`→`memos`(V10), `free_memo_photo`→`memo_photos`(V11, user+target 직접 키), `checklist_submission`→`checklist_submissions`(V12), `checklist_answer`→`answers` TEXT 컬럼. `checklist_template`/`section`/`question`/`question_option` → `DROP`/`해당없음`. 소유 도메인 `checklist`→`memo`. 위험 표 신규 테이블명·미결 갱신 |
| `docs/inventory/integrations.md` | 갱신함 | S3 행: memo 소비 이관(KD3-465, `MediaMemoPhotoStorageAdapter`, `MediaPurpose.MEMO_ATTACHMENT` → `memo/{userCode}/`), orphan 정리 미결 명시 |
| `docs/domains/media.md` | 갱신함 | §1 업로드 purpose 행에 `MEMO_ATTACHMENT`(→ `memo/{userCode}/{filename}`) 추가 |
| `docs/architecture/hexagonal.md` | 확인, 변경 없음 | 규칙 4 와일드카드로 `memo.domain` 자동 포함 — 문구 stale 아님 |
| `docs/service.md` | 확인, 변경 없음 | §5 흐름도는 원장 중심이고 보호자 탐색 경험(메모·체크리스트·북마크·비교) 노드가 없음. 이 티켓에서 추가하지 않음 — 유치원 부가 기능 슬라이스가 다 들어온 뒤 한 번에 반영 여부 판단(별도) |
| `docs/conventions/api-contract.md` | 확인, 변경 없음 | 날짜포맷 규약은 KD3-495(`§2`)로 이미 dev에 존재 — memo는 `LocalDate`만 씀(C6). memo 전용 판단 기준(템플릿 위치·JSON 저장)은 `domains/memo.md` |
| `docs/adr/` | 해당 없음 | 신규 결정 없음 — 버전·컷오버는 ADR 0011/0012 그대로 적용. `MediaPurpose` 확장·슬라이스 분해는 이 문서·`domains/memo.md`에 기록 |
| Flyway migration | 신규 | `V10__create_memos.sql`, `V11__create_memo_photos.sql`, `V12__create_checklist_submissions.sql` |
| Notion API 명세 | 미완(사람 몫) | v1 memo/checklist 6개 엔드포인트 |
