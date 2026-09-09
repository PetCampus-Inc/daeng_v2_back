> 생성: 2026-09-09 18:00 · 최종 수정: 2026-09-09 18:00

# memo 도메인 마이그레이션 지시서

보호자가 유치원을 탐색·비교하며 남기는 **자유메모**(유치원당 1건, 사진 첨부)와 **상담시 체크리스트**(유치원당 1건, 고정 문항 답변)를 담당한다.

- 설계 근거: [`0003`](../adr/0003-헥사고날-정석형-통일.md) 헥사고날 정석형, [`0007`](../adr/0007-인가-기본-deny-전환.md) 인가 기본 deny, [`0010`](../adr/0010-신규-db-인스턴스-스키마-재작성.md) 신규 DB, [`0012`](../adr/0012-신규-서버-v0-미제공-원칙.md) 신규 서버는 `v0`를 만들지 않는다
- 원본: `daeng_v1_back`(`knockdog_server`)의 `memo/` 패키지 (컨트롤러 1개, 서비스 2개, 엔티티 8개)
- 착수 기록: [`docs/work/KD3-465-memo.md`](../work/KD3-465-memo.md) — 계약 결정 C1~C15, 프론트(`daeng_v2_front`) 대조 결과
- **KD3-465에서 전체 구현 완료** (자유메모 + 사진 + 체크리스트). 레거시 엔드포인트별 판정·진척은 [`docs/inventory/api.md`](../inventory/api.md)(memo 행)가 단일 기준이다.

## 0. 담당 데이터

| 저장소 | 이름 | 비고 |
|---|---|---|
| MySQL (JPA) | `memos` (V10) | `(user_code, target_id)` 유니크 1행. `user_code`는 auth 토큰 subject(`UserCode`), `target_id`는 `kindergartens.naver_place_id`. FK 제약 없음. 레거시 `free_memo`의 "매 저장 새 row + 히스토리"를 폐기하고 upsert 1행으로 재설계 |
| MySQL (JPA) | `memo_photos` (V11) | `memo` 저장 시 `photoKeys` 배열을 **전량 교체**(하드 삭제 후 삽입)하므로 `BaseEntity`(soft-delete)를 상속하지 않는다 — `user_agreements`와 같은 예외. `object_key`는 `media` commit 후 영구 key(`memo/{userCode}/{filename}`) |
| MySQL (JPA) | `checklist_submissions` (V12) | `(user_code, target_id)` 유니크 1행. `answers`는 `{questionCode: value}` JSON 컬럼(`AttributeConverter`). `template_version`은 답변 시점 템플릿 버전. 레거시 `checklist_submission` + `checklist_answer`(폴리모픽 4컬럼)를 통합 |
| 리소스 파일 | `resources/checklists/registration.ko-KR.json` | 상담 체크리스트 템플릿. **DB 테이블 없음** — 레거시 `checklist_template`/`checklist_section`/`checklist_question`/`question_option`은 미이관. 5섹션 13문항(TRI_STATE 12 + INTEGER 1). 기동 시 1회 로드해 (a) 템플릿 조회 응답 (b) 답변 저장 검증 (c) 답변 조회 시 문항 라벨 채우기에 모두 쓰인다 |

## 1. 불변식 · 제약

| 항목 | 규칙 |
|---|---|
| 자유메모 개수 | 한 사용자가 한 유치원에 자유메모 1건. `PUT`은 upsert |
| 자유메모 content | 2000자 이내(`FreeMemo.CONTENT_MAX_LENGTH`, 도메인 `require` + 서비스가 `MEMO_CONTENT_TOO_LONG`으로 선검사) |
| 자유메모 사진 | 최대 5장(`FreeMemo.PHOTO_MAX_COUNT`). `PUT`의 `photoKeys`는 순서 있는 배열이고, `tmp/{userCode}/…`는 `media`로 commit해 영구화, `memo/{userCode}/…`(호출자 소유)는 유지, 그 외는 `MEMO_INVALID_PHOTO_KEY` |
| 체크리스트 개수 | 한 사용자가 한 유치원에 제출 1건. `PUT`은 upsert이며 **전체 교체**(제출 answers가 곧 그 제출의 전부, 빠진 문항은 삭제) |
| 체크리스트 문항 ID | `q_vaccine_proof_required` 등 13개는 **레거시·프론트(`checklist-edit.api.ts`)와 100% 동일 — 절대 변경 금지**. 라벨·섹션 제목은 화면지시서(Figma) 기준이며 프론트가 API 응답을 그대로 렌더한다 |
| 체크리스트 값 | TRI_STATE ∈ {`YES`,`NO`,`UNKNOWN`}, INTEGER는 `q_max_dogs_per_day` 하나뿐이고 0~500. 응답의 `value`는 **항상 문자열**(레거시는 INTEGER를 숫자로 내려 프론트 타입과 어긋났음 — 의도적 교정) |
| required 검증 | 없음 — 템플릿에 `required:true` 문항이 없다. 부분 제출 허용 |
| 인가 | `/api/v1/memos/**`, `/api/v1/checklists/**` 전부 인증 필수(기본 deny). **레거시는 `GET /memo/checklist`(템플릿)만 공개였으나 신규는 인증화** |
| 유치원 존재 검증 | 하지 않는다 — memo 응답에 유치원 데이터가 없어 kindergarten 도메인 의존이 불필요. 잘못된 `target_id`는 무해(FK 없음), 조회는 빈 응답 |

## 2. 이 서버가 제공하는 API (전부 `v1` 신규 — ADR 0012)

| 신규 (`v1`) | 레거시 (`v0`) | 설명 |
|---|---|---|
| `GET /api/v1/memos/{targetId}` | `GET /api/v0/memo?targetId=` | 유치원별 자유메모 조회. 없으면 200 + `{content:null, photos:[]}` |
| `PUT /api/v1/memos/{targetId}` | `POST /api/v0/memo?targetId=` | 자유메모 upsert. body `{content?, photoKeys?}`. 갱신된 표현 반환 |
| `GET /api/v1/memos` | `GET /api/v0/memo/shops` | 내가 메모한 유치원 목록. `{memos:[{shopId, content, memoDate}]}` — `shopId`=targetId, `memoDate`=`LocalDate`. 프론트가 `shopId`로 유치원 카드에 조인 |
| `GET /api/v1/checklists/template` | `GET /api/v0/memo/checklist` | 상담 체크리스트 템플릿(정적). `{template:{code,version,locale,title}, sections:[{id,title,questions:[{id,label,type}]}]}` |
| `GET /api/v1/checklists/{targetId}` | `GET /api/v0/memo/checklist/answer?targetId=` | 유치원별 내 체크리스트 답변. `{sections:[{sectionId,title,answers:[{questionId,question,value}]}]}`. 없으면 200 + `{sections:[]}` |
| `PUT /api/v1/checklists/{targetId}` | `POST /api/v0/memo/checklist?targetId=` | 체크리스트 답변 upsert(전체 교체). body `{answers:[{questionId,value}]}` |
| — | `GET /api/v0/memo/list` | `DROP` (ADR 0004: `memo`, `memo/shops`만 사용) |

레거시 `v0`는 컷오버까지 레거시 서버가 계속 제공한다(ADR 0012).

## 3. 구조 (정석형 헥사고날)

```
domain/memo/
  domain/            FreeMemo·MemoPhoto·MemoId, ChecklistTemplate·ChecklistSection·ChecklistQuestion·ChecklistQuestionType·ChecklistSubmission (순수 모델/VO)
  application/
    MemoErrorCode.kt
    port/input/       GetFreeMemo·SaveFreeMemo·GetMemoedKindergartens / GetChecklistTemplate·GetChecklistAnswers·SaveChecklistAnswers UseCase
    port/output/      LoadFreeMemo·SaveFreeMemo·MemoPhotoStorage / LoadChecklistTemplate·LoadChecklistSubmission·SaveChecklistSubmission Port
    service/          FreeMemoService, ChecklistService
  adapter/
    inbound/web/      FreeMemoController·MemoListController·ChecklistTemplateController·ChecklistAnswerController
    outbound/persistence/  Memo·MemoPhoto·ChecklistSubmission JPA 엔티티/Repository/Adapter, ChecklistAnswersJsonConverter
    outbound/template/     ChecklistTemplateResourceAdapter (리소스 JSON → ChecklistTemplate)
    outbound/media/        MediaMemoPhotoStorageAdapter (MemoPhotoStoragePort → media CommitObjectUseCase·IssueDownloadUrlUseCase 위임)
```

- ArchUnit 규칙 4(`domain.*.domain..` 와일드카드)로 `memo.domain` 자동 포함. 도메인 모델은 에러코드를 모른다 — 값 검증은 `require`(도메인) + `BusinessException(MemoErrorCode)`(서비스). 체크리스트 값 정규화는 `ChecklistQuestion.normalize(raw): String?`(null = 무효)로 도메인이 담당하고 서비스가 null → `MEMO_INVALID_CHECKLIST_ANSWER`로 매핑.

## 4. media 의존

첨부 사진은 `media` 도메인에 위임한다.

- 업로드: 프론트가 `POST /api/v1/media/upload-urls` `{purpose:"MEMO_ATTACHMENT", contentType}` → `tmp/{userCode}/MEMO_ATTACHMENT/{uuid}.{ext}` key. `MediaPurpose.MEMO_ATTACHMENT`는 KD3-465에서 추가(→ `memo/{userCode}/{filename}`).
- 저장: `PUT /api/v1/memos/{targetId}`의 `photoKeys` 중 tmp key는 memo가 `CommitObjectUseCase.commit`으로 영구화, 결과 key를 `memo_photos`에 저장.
- 조회: `GET`에서 각 영구 key에 `IssueDownloadUrlUseCase.issue` → `photos[].url`.
- **재편집으로 참조가 끊긴 영구 object의 S3 삭제 주체는 미결** — [`integrations.md`](../inventory/integrations.md) S3 행 참고. 현재는 `memo_photos` row만 정리하고 S3 object는 방치.

## 5. 레거시 대비 의도적 차이 (parity 대조 시 참고)

`KEEP` 6개 엔드포인트는 응답 **내용**이 레거시와 기능적으로 같은지만 본다(경로·엔벨로프는 재설계). 아래는 의도된 차이다.

| 항목 | 레거시 | 신규 |
|---|---|---|
| 응답 엔벨로프 | raw DTO 3개 + `BasicInfoResponseDto` 3개 혼재 | `Response<T>` 단일 (C1) |
| 성공 코드 | `MEMO_SAVED`/`CHECKLIST_SAVED` 등 커스텀 | `SUCCESS` (C2) |
| 자유메모 히스토리 | 매 저장 새 row, `GET /memo/list`로 노출 | upsert 1행, `list` DROP (C4) |
| 단건 조회 날짜 | `memoDate`(`yyyy.MM.dd`) 포함 | 없음 (프론트가 안 읽음, C6) |
| 체크리스트 `value` | INTEGER를 숫자로 | 항상 문자열 (C8) |
| 빈 체크리스트 | `fail("CHECKLIST_NOT_FOUND")` (프론트가 throw) | 200 + `{sections:[]}` (C9) |
| 템플릿 조회 인가 | 공개 | 인증 필요 (C12) |
| 작성자 식별자 | user PK(BIGINT) | `user_code` 문자열 (C13) |
