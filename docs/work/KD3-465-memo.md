> 생성: 2026-09-08 11:00 · 최종 수정: 2026-09-08 16:10

# KD3-465 — 메모 기능 이관 (자유메모 · 상담 체크리스트)

| 항목 | 값 |
|---|---|
| Jira | `KD3-465` (하위 작업) |
| 브랜치 | 3슬라이스로 분리: `feat/KD3-465-free-memo` · `feat/KD3-465-memo-photos` · `feat/KD3-465-checklist` (전부 `epic/KD3-272-kindergarten-features` 기준). 아래 `## 슬라이스 분해` 참고 |
| 상위 에픽 | `KD3-403`(Epic, [리팩토링] daeng_v1_back → daeng_v2_back). 중간 작업 `KD3-272`(Task, 유치원 도메인 마이그레이션) — git 브랜치는 `epic/KD3-272-kindergarten-features` (스키마 이관분 `epic/KD3-272-kindergarten-schema`는 이미 dev에 머지됨. 이 브랜치는 그 위에 얹는 유치원 부가 기능용) |

## 현재 제어점

- 활성 workflow: `003-migration` (+ `005-new-feature` 일부 — 화면지시서 기반 신규 UX 요소가 있어 두 유형 조건을 함께 적용)
- 현재 공통 단계: `2`(계획 승인·작업 문서 생성) — 이 문서는 **초안**이다. 아래 `미결 질문`이 닫히고 사용자 승인이 기록되면 3단계(구현)로 전환한다.
- 다음 결정 또는 전환 조건: ① 전체 계획 사용자 승인 (Q1~Q5는 2026-09-08 프론트 대조로 해소, Q6은 검증 단계 항목) ② 승인 후 슬라이스 1(`feat/KD3-465-free-memo`)부터 구현 착수.
- 이 문서는 3슬라이스 공통 **우산 설계 문서**다. 슬라이스별 구현·검증 결과는 아래 `## 슬라이스 분해`의 각 항목에 이어 기록한다 (별도 work 문서를 만들지 않는다).
- 베이스: `epic/KD3-272-kindergarten-features`. 이 브랜치는 `dev`(유치원 스키마 포함) + `feat/KD3-478-s3-infra-image-upload`(media 도메인 = S3 인프라)를 머지한 상태다. KD3-478이 `dev`에 독립 머지되면 그 머지 커밋은 자연히 흡수된다.

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
  object_key   VARCHAR(...)    -- media commit 후 영구 key ("memo/{targetId}/{userCode}/{filename}")
  sort_order   INT             -- photoKeys 배열 순서
  created_at
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
    FreeMemo.kt                       -- 애그리게잇 1 (userCode, targetId, content, photos)
    MemoPhoto.kt
    ChecklistSubmission.kt            -- 애그리게잇 2 (userCode, targetId, templateVersion, answers)
    ChecklistTemplate.kt / ChecklistSection.kt / ChecklistQuestion.kt   -- 순수 VO (정적 템플릿 표현)
    ChecklistQuestionType.kt          -- TRI_STATE, INTEGER (레거시 QuestionType 7종 중 실사용 2종만)
    ChecklistAnswerValue.kt           -- 값 검증 (TRI_STATE ∈ {YES,NO,UNKNOWN}, INTEGER 범위)
  application/
    MemoErrorCode.kt
    port/input/
      GetFreeMemoUseCase.kt / SaveFreeMemoUseCase.kt / GetMemoedKindergartensUseCase.kt
      GetChecklistTemplateUseCase.kt / GetChecklistAnswersUseCase.kt / SaveChecklistAnswersUseCase.kt
    port/output/
      LoadFreeMemoPort.kt / SaveFreeMemoPort.kt
      LoadChecklistSubmissionPort.kt / SaveChecklistSubmissionPort.kt
      LoadChecklistTemplatePort.kt          -- 정적 템플릿 로드
      MemoPhotoStoragePort.kt               -- 사진 commit / 조회 URL (media 위임)    service/  (유스케이스별 1파일)
  adapter/
    inbound/web/   (유스케이스별 컨트롤러 분리 — hexagonal.md §1)
      FreeMemoController.kt / MemoListController.kt
      ChecklistTemplateController.kt / ChecklistAnswerController.kt
    outbound/
      persistence/  JPA 엔티티 · Repository · PersistenceAdapter · Mapper
      template/      ChecklistTemplateResourceAdapter.kt  (resources/checklists/*.json 파싱)
      media/         MediaMemoPhotoStorageAdapter.kt      (media UseCase 위임)```

- `HexagonalArchitectureTest` 규칙 4는 `domain.*.domain..` 와일드카드라 `memo.domain` 자동 포함(KD3-478에서 확인됨 — hexagonal.md §3의 stale 문구는 KD3-478이 정정).

### 계약 결정 (v1 재설계 — ADR 0012)

| # | 항목 | 결정 |
|---|---|---|
| C1 | 응답 엔벨로프 | 전부 `global/response/Response.kt` (`{status, code, message, data}`). 레거시의 raw DTO 3개 + `BasicInfoResponseDto` 3개 혼재를 통일 |
| C2 | 성공 응답 | `Response.success(data)` — `code: "SUCCESS"`. 레거시 커스텀 성공 코드(`MEMO_SAVED`, `CHECKLIST_SAVED` 등)·메시지 폐기 (프론트가 성공 시 `data`만 씀) |
| C3 | 오류 | `domain/memo/application/MemoErrorCode.kt` enum 신규 ([`error-handling.md`](../conventions/error-handling.md)). `code` 문자열은 레거시(`CHECKLIST_*`)와 맞추지 않는다 — 프론트가 `data.success`(boolean)/`data.message`만 보고 `code`로 분기하지 않음(프론트 `answers.tsx`·`getMemo.ts`·`questions.tsx` 대조) |
| C4 | 자유메모 upsert | `(user_code, target_id)` 유니크 1행. `PUT`이 없으면 생성 / 있으면 content(+사진) 교체. **레거시의 "매 저장 새 row + 히스토리" 폐기** (히스토리 노출하던 `GET /memo/list`는 `DROP`) |
| C5 | 자유메모 빈 상태 | `GET`에서 200 + `{ content: null, photos: [] }` (레거시 parity) |
| C6 | 날짜 필드 | 단건 조회(`GET /api/v1/memos/{targetId}`)에는 날짜 필드 **없음** (프론트 `MemoResponse`가 `content`·`photos`만 읽음). 목록(`GET /api/v1/memos`)의 `memoDate`는 **ISO date `YYYY-MM-DD`로 유지** — 프론트 `entities/kindergarten/model/mappers.ts`가 `memoDate.replace(/-/g, '.')`로 표시 포맷을 만들므로 대시 구분자 필수. 레거시 `getFreeMemoShopsList`의 `LocalDate.toString()`과 동일. (전체 서버 날짜 포맷 통일은 이 티켓 범위 밖 — 아래 참고) |
| C7 | 체크리스트 답변 저장 | `PUT` = 전체 교체 (제출된 answers가 그 submission의 전부, 빠진 문항은 삭제). required 검증 없음 (템플릿에 `required:true` 문항이 없음) — 부분 제출 허용 |
| C8 | 체크리스트 답변 `value` 타입 | 응답에서 **항상 문자열**. 레거시는 INTEGER를 숫자로 내려 프론트 타입(`value: string`)과 어긋났음 — 의도적 교정 |
| C9 | 체크리스트 빈 상태 | `GET`에서 200 + `{ sections: [] }`. **레거시는 `fail("CHECKLIST_NOT_FOUND")` 실패 응답을 내려 프론트가 throw함 — 의도적 교정** |
| C10 | 체크리스트 템플릿 라벨 | 문항 ID(`q_vaccine_proof_required` 등)는 레거시와 100% 동일(프론트 `QUESTION_MAP` 계약). 라벨·섹션 제목은 화면지시서(Figma) 기준으로 최신화 — 레거시 JSON과 차이: "강아지 성향 관리"→"강아지 맞춤 관리", "몇 마리까지 등록"→"등원", "가능한가요"→"가능할까요" 등 |
| C11 | INTEGER 문항 검증 | `q_max_dogs_per_day` `{min:0, max:500}` 유지 |
| C12 | 인증 | `/api/v1/memos/**`, `/api/v1/checklists/**` **전부 인증 필수** (SecurityConfig, ADR 0007 기본 deny). **레거시는 `GET /memo/checklist`(템플릿)만 공개였음 — 인증으로 변경** |
| C13 | 작성자 식별자 | `user_code` 문자열(`@AuthenticationPrincipal`이 주는 토큰 subject) 직접 저장. 레거시는 user PK(BIGINT) 저장이었으나 memo가 PK를 쓸 일이 없어 auth 도메인 의존을 제거 |
| C14 | 유치원 존재 검증 | **하지 않는다.** memo 응답에 유치원 데이터가 하나도 없어(이름·요금 등 없음) 크로스 도메인 의존이 불필요. 잘못된 `targetId`로 저장돼도 무해(FK 없음), 조회는 빈 응답. 가드가 필요해지면 후속으로 memo 자체 outbound 포트 추가 |
| C15 | 사진(첨부) | **이 티켓 포함** (사용자 확정 2026-09-08). **서버측 commit 방식**: 프론트가 `POST /api/v1/media/upload-urls`로 tmp key 확보 → S3 직접 PUT → `PUT /api/v1/memos/{targetId}` body `photoKeys`(순서 있는 배열)에 전달. memo가 각 key를 판별 — `tmp/{userCode}/…`면 `MemoPhotoStoragePort`(→ media `CommitObjectUseCase`, `targetPath="memo/{targetId}/{userCode}"`)로 commit해 영구 key 획득 / `memo/{targetId}/{userCode}/…`면 prefix 소유권 검증 후 유지 / 그 외 400. 최종 영구 key를 `memo_photos`에 배열 순서대로 전량 교체. `GET`은 각 key에 media `IssueDownloadUrlUseCase`로 `url` 생성 → `photos: [{key, url}]`. 사진 ≤ 5장. **프론트는 현재 레거시 `/s3/image/move`를 직접 호출해 client측 commit 중 — v1 전환 시 "업로드 → tmp key → PUT memo"로 단순화(프론트 작업)** |

### API 계약 요약

모든 응답은 `Response<T>`로 감싼다. 아래는 `data` 내부.

| 메서드·경로 | 요청 | 응답 `data` | 인가 |
|---|---|---|---|
| `GET /api/v1/memos/{targetId}` | — | `{ content: string\|null, photos: [{ key, url }] }` | 인증 |
| `PUT /api/v1/memos/{targetId}` | `{ content?: string, photoKeys?: string[] }` | 갱신된 표현 (`GET`과 동일 형태) | 인증 |
| `GET /api/v1/memos` | — | `{ memos: [{ shopId, content, memoDate }] }` — `shopId`=targetId, `memoDate`=`YYYY-MM-DD`. 프론트가 `shopId`로 유치원 카드에 조인 | 인증 |
| `GET /api/v1/checklists/template` | — | `{ template: { code, version, locale, title }, sections: [{ id, title, questions: [{ id, label, type }] }] }` | 인증 |
| `GET /api/v1/checklists/{targetId}` | — | `{ sections: [{ sectionId, title, answers: [{ questionId, question, value }] }] }` (없으면 `{ sections: [] }`) | 인증 |
| `PUT /api/v1/checklists/{targetId}` | `{ answers: [{ questionId, value }] }` | 갱신된 표현 (`GET`과 동일 형태) | 인증 |

- 저장(`PUT`) 응답 body는 프론트가 읽지 않는다(대조 결과) — RESTful 관례상 갱신된 표현을 반환하되, 프론트는 저장 후 쿼리 무효화로 재조회한다.

## 슬라이스 분해

3개 PR로 나눠 `epic/KD3-272-kindergarten-features`에 올린다. 전부 `Refs: KD3-465`.

| # | 브랜치 | 범위 | Flyway | media 의존 | 선행 | 상태 |
|---|---|---|---|---|---|---|
| 1 | `feat/KD3-465-free-memo` | `GET`·`PUT /api/v1/memos/{targetId}`(사진 제외, `photos`는 항상 `[]`), `GET /api/v1/memos`. `memos` 테이블, `FreeMemo` 애그리게잇, `MemoErrorCode` 신설, persistence 어댑터. 이 설계 문서 포함 | `V10__create_memos.sql` | 없음 | — | 착수 전 (브랜치 생성됨) |
| 2 | `feat/KD3-465-memo-photos` | 슬라이스 1의 `GET`/`PUT`에 `photos`/`photoKeys` 추가. `memo_photos` 테이블, `MemoPhoto`, `MemoPhotoStoragePort` + `MediaMemoPhotoStorageAdapter`(media `CommitObjectUseCase`·`IssueDownloadUrlUseCase` 위임) | `V12__create_memo_photos.sql` | 있음 | **슬라이스 1 머지** | 착수 전 |
| 3 | `feat/KD3-465-checklist` | `GET /api/v1/checklists/template`, `GET`·`PUT /api/v1/checklists/{targetId}`. `checklist_submissions` 테이블, `ChecklistSubmission` 애그리게잇, `ChecklistTemplate` VO, `LoadChecklistTemplatePort` + 리소스 어댑터, `resources/checklists/registration.ko-KR.json` | `V11__create_checklist_submissions.sql` | 없음 | 슬라이스 1과 독립(병렬 가능), `MemoErrorCode`만 공유 | 착수 전 |

- **머지 순서**: 1 → 3 → 2. Flyway 버전은 정수 오름차순이라, 각 PR의 `V__` 번호는 머지 직전에 epic에 이미 들어간 마이그레이션 기준으로 확정한다(위 번호는 잠정).
- **공유 파일**: `MemoErrorCode.kt`(먼저 머지되는 슬라이스가 생성, 다음이 코드 추가), `domain/memo` 패키지. 컨트롤러는 유스케이스별 분리라 안 겹침. ArchUnit은 `domain.*.domain..` 와일드카드라 등록 불필요.
- `feat/A` → `epic` squash merge, `epic` → `dev` 일반 merge (git.md §2).

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
- **서버 전체 날짜/시간 포맷 통일** — 레거시는 도메인·엔드포인트마다 제각각(`yyyy.MM.dd` 표시 문자열 / ISO date / 포맷 없음). memo는 날짜 필드가 목록 `memoDate` 하나뿐이고 프론트가 ISO `YYYY-MM-DD`에 결합돼 있어(C6) 여기서 통일할 대상이 없다. 서버 전역 규칙(응답 timestamp = ISO-8601, 표시 포맷은 프론트 담당, 백엔드는 pre-formatted 문자열 금지)은 `docs/conventions/api-contract.md`에 별도 항목으로 추가하는 게 맞고, repo-wide 컨벤션이라 별도 fast dev PR 대상. 이 티켓 범위 밖.

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
- (대기) — 전체 계획 승인 (프론트 대조 반영본 검토 후).

## 완료 확인 기준

- [ ] `GET /api/v1/memos/{targetId}`: 인증 없으면 401, 메모 없으면 200 + `{content:null, photos:[]}`, 있으면 content·photos 반환 — 서비스·컨트롤러 테스트.
- [ ] `PUT /api/v1/memos/{targetId}`: 신규 생성 / 기존 교체 모두 동작, `content` 2000자 초과 400(`MemoErrorCode`), `(user_code,target_id)` 유니크 보장 — 서비스 테스트 + 통합 테스트.
- [ ] `GET /api/v1/memos`: 내가 저장한 (유치원당 1건) 메모 목록, `{ shopId, content, memoDate }`, 정렬 기준 명시(예: `updated_at` 내림차순) — 서비스 테스트.
- [ ] `GET /api/v1/checklists/template`: 정적 리소스에서 5섹션·13문항(12 TRI_STATE + 1 INTEGER) 반환, 문항 ID가 레거시·프론트 하드코딩 목록과 일치, 인증 필요 — 테스트 + 리소스 파일 검증.
- [ ] `PUT /api/v1/checklists/{targetId}`: 모르는 questionId 400, TRI_STATE 값 검증, INTEGER 범위(0~500) 검증, upsert 전체 교체 — 서비스 테스트.
- [ ] `GET /api/v1/checklists/{targetId}`: 저장 없으면 200 + `{sections:[]}`, 있으면 템플릿 순서대로 섹션·문항·값, `value` 항상 문자열 — 서비스 테스트.
- [ ] `HexagonalArchitectureTest` 통과 (`memo.domain` 와일드카드 포함 확인).
- [ ] `./gradlew build` green — ktlint + ArchUnit + 전체 테스트.
- [ ] **로컬 응답 대조 (사람 몫 가능성)**: `KEEP` 6개 엔드포인트 — 레거시 `v0` 응답 형태(raw DTO / `BasicInfoResponseDto`)와 신규 `v1`(`Response<T>`)의 `data` 내부 필드가 기능적으로 동일한지 대조. 경로·엔벨로프 차이는 의도된 재설계(C1). 대조 결과·차이·허용 근거를 여기 기록.
- [ ] **Notion API 명세 등록 (사람 몫)**: v1 memo/checklist 6개 엔드포인트 ([`docs/rules/notion-api-spec-sync.md`](../rules/notion-api-spec-sync.md)).

### 계약 parity (003-migration §4)

- 6개 엔드포인트 전부 `KEEP`(응답 내용) — 레거시와 `data` 내부 필드 대조 대상.
- 의도적 차이(대조 실패 아님): C1(엔벨로프 통일), C2(성공 코드), C6(단건 응답에서 날짜 필드 없음), C8(`value` 문자열화), C9(빈 체크리스트 200 vs 레거시 실패 응답), C12(템플릿 인증화).
- `GET /api/v0/memo/list`는 `DROP` — 대조 제외.

## 작업 후 확인 목록

| 문서 | 판정 | 결과 |
|---|---|---|
| `docs/domains/memo.md` | 신설 | 새 도메인 — 경계·불변식(1유저 1유치원 1메모/1체크리스트, user_code 소유, 템플릿 정적, 문항 ID 불변), v1 엔드포인트 매핑, 스키마 3테이블, 체크리스트 템플릿 위치·버전 규칙, media 의존(사진 commit/download 위임) |
| `docs/inventory/api.md` | 갱신 | L247~253 memo 6개 행: 진척 `미착수`→`진행중`, `대상 버전` `v1`, 근거에 KD3-465 링크. `GET /memo/list`(L252)는 `DROP` 유지 |
| `docs/inventory/database.md` | 갱신 | `free_memo`/`free_memo_photo` 행: 진척 `진행중`, 신규 스키마(`memos`/`memo_photos`/`checklist_submissions`) 요지, `checklist_*` 관련 미결(`user_id 의미`→`user_code로 확정` 등) 반영. 탈퇴 전파 표(L113)·이미지 수명주기 표(L117)에 신규 테이블명 갱신 |
| `docs/inventory/integrations.md` | 갱신 | S3 행: 사용 위치에 `memo` 도메인 추가(media 포트 위임), memo 첨부 key 규칙(`memo/{targetId}/{userCode}/`), orphan 정리 미결 명시 |
| `docs/architecture/hexagonal.md` | 확인 | §3 규칙 4 문구는 KD3-478이 이미 정정 — 변경 없음 예상 |
| `docs/service.md` | 확인 필요 | §5 흐름도가 원장 중심이라 보호자 유치원 탐색(메모·체크리스트·북마크·비교) 노드가 없음. 별도 브랜치 추가 여부는 사용자 확인 (repo-wide 문서라 fast dev PR 대상일 수 있음) |
| `docs/conventions/api-contract.md` | 별건 검토 | 응답 timestamp 포맷 통일(ISO-8601, 표시 포맷은 프론트) 규칙 추가 후보 — repo-wide 컨벤션이라 memo 티켓과 분리해 fast dev PR로. memo 자체는 새 판단 기준 없음(템플릿 위치·JSON 저장은 `domains/memo.md`) |
| `docs/adr/` | 해당 없음 | 되돌리기 어렵거나 여러 도메인에 걸친 신규 결정 없음 — 버전·컷오버는 ADR 0011/0012 기존 결정을 그대로 적용 |
| Flyway migration | 신규 | `V10__create_memo_tables.sql` (번호는 착수 시점 확인) |
| Notion API 명세 | 미완(사람 몫) | v1 memo/checklist 6개 엔드포인트 |
