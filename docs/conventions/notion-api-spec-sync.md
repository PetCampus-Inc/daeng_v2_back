> 생성: 2026-07-28 15:04 · 최종 수정: 2026-08-05

# API 명세서 (Notion) 연동 규칙

API 명세서는 코드 저장소가 아니라 Notion 데이터베이스에서 관리한다. 이 문서는 어떤 AI 도구를 쓰든 동일한 방식으로 그 데이터베이스에 페이지를 생성/수정하는 방법을 정의한다. [`workflow.md`](../rules/workflow.md) 2번(작업 후 확인 목록)·12번(문서 동기화)에서 "API 추가/변경" 작업일 때 이 규칙을 따른다.

## 1. 인증

- Notion Internal Integration 토큰을 환경변수 `API_NOTION_KEY`로 읽는다. 값 자체는 절대 코드/문서/커밋에 남기지 않는다.
- 모든 요청에 아래 헤더를 포함한다.

```
Authorization: Bearer $API_NOTION_KEY
Notion-Version: 2022-06-28
Content-Type: application/json
```

## 2. 데이터베이스

- 이름: **API 명세서**
- ID: `3ab6c15f-67fb-80e4-ba5c-edb14af226ca`
- 엔드포인트: `https://api.notion.com/v1/pages` (생성 — `POST`), `https://api.notion.com/v1/pages/{page_id}` (수정 — `PATCH`)

### 속성(property) 매핑

| 속성명 | 타입 | 이 저장소(백엔드) 작업에서 하는 일 |
|---|---|---|
| 이름 | title | 페이지 제목. API의 명칭(한국어, 예: `회원가입`, `로그인`) — `<Method> <엔드포인트 경로>`가 아니다. 그 정보는 `엔드포인트`/`Method` 속성이 따로 담당한다 |
| 엔드포인트 | rich_text | 실제 경로 (예: `/api/v1/users/me`) |
| Method | select | `GET`/`POST`/`PUT`/`PATCH`/`DELETE` 중 하나 (옵션 확정됨) |
| 도메인 | select | `docs/domains/*.md` 슬러그. 옵션에 없으면 새로 추가한다 |
| BE 개발 | status | 이 저장소가 갱신하는 유일한 상태값. `시작 전`/`수정 중`/`진행 중`/`완료` |
| FE 개발 | status | **건드리지 않는다** — 프론트 저장소 담당 |
| 명세서 업데이트 | date | 페이지 생성/수정 시각으로 갱신 |

**FE 개발은 갱신 대상이 아니다.** 백엔드 작업에서 프론트 상태를 임의로 바꾸지 않는다.

## 3. 페이지 본문 템플릿

Notion 앱의 "새 페이지" 기본 템플릿은 **API로 페이지를 만들 때 자동 적용되지 않는다** — API로 만든 페이지는 properties만 채워지고 본문은 완전히 빈 상태로 시작한다. 그래서 본문 구조를 코드로 고정해둔다.

**[`notion-api-page-template.json`](notion-api-page-template.json)**: 검증된 Notion block 배열. 페이지 생성 시 `children`에 그대로 넣는다. 블록 순서와 각 자리에 채울 내용:

| 순서 | 블록 | 채울 내용 |
|---|---|---|
| 1 | `heading_1` "1️⃣ /url" | "1️⃣ " 뒤에 실제 엔드포인트 경로로 텍스트 교체 |
| 2 | `heading_3` "API 설명" | 그대로 둠 |
| 3 | `quote` (빈 칸) | 이 API가 뭘 하는지 1~2문장 |
| 4 | `heading_1` "2️⃣ Request" | 그대로 둠 |
| 5~6 | `heading_3` "HTTP Header" + `table`(4열, 예시 행 1개: `Authorization` / `보유 중인 액세스 토큰`) | **실제 HTTP 헤더 이름만** Name에 적는다(예: `Authorization`, `Content-Type`). 쿠키는 이 표가 아니라 바로 아래 "Cookie" 표에 적는다 — `Cookie`를 헤더 이름처럼 적지 않는다. 필요한 헤더만 행 추가, 없으면 예시 행 삭제 |
| 7~8 | `heading_3` "Cookie" + `table`(4열) | 요청에 실려오는 쿠키(`OIDC_AUTH_TOKEN`, `REFRESH_TOKEN` 등)를 Name에 쿠키 이름 그대로 적는다. 없으면 표는 두되 행은 비워둔다 |
| 9~10 | `heading_3` "Path Variable" + `table` | 없으면 표는 두되 행은 비워둔다 |
| 11~12 | `heading_3` "Parameter" + `table` | 위와 동일 |
| 13~14 | `heading_3` "Body" + `table` | 위와 동일 |
| 15 | `heading_1` "3️⃣ Response" | 그대로 둠 |
| 16 | `heading_2` "✅ 성공" | 그대로 둠 |
| 17 | `code`(json, 빈 칸) | 실제 성공 응답 예시 JSON |
| 18 | `paragraph`(bold, "응답 필드 설명") | 그대로 둠 |
| 19 | `table`(3열: 필드명/타입/설명, 빈 행 2개) | 응답 JSON의 필드마다 한 행. 필요시 행 추가 |
| 20 | `heading_2` "❌ 실패" | 그대로 둠 |
| 21 | `table`(4열: status/code/field/reason, 예시 행 1개) | 실제 에러 케이스로 행 채우기/추가 |

행이 부족하면 `table_row` 블록을 같은 형식으로 복제해 추가한다. 표의 열 구성(4열 Request 표들, 3열 응답 필드 표, 4열 실패 표)은 절대 바꾸지 않는다.

**Header vs Cookie 구분 예시**: 액세스 토큰을 `Authorization: Bearer <토큰>` 헤더로 받는 API라면 HTTP Header 표에 `Authorization` / `보유 중인 액세스 토큰`으로 적는다. 반대로 OIDC 임시 토큰이나 리프레시 토큰처럼 쿠키로 전달되는 값은 Cookie 표에 그 쿠키 이름(`OIDC_AUTH_TOKEN` 등)으로 적는다 — 브라우저가 자동으로 실어 보내는 `Cookie` 헤더 자체를 HTTP Header 표의 항목으로 문서화하지 않는다.

## 4. 신규 생성 vs 기존 수정

- **신규 엔드포인트**: `POST /v1/pages`에 `parent.database_id`, 위 §2 속성, §3 template의 `children`을 실어 보낸다.
- **기존 엔드포인트 변경**: 먼저 `POST /v1/search`로 해당 페이지를 찾거나(제목 또는 엔드포인트 속성으로 필터), 이미 알고 있는 `page_id`가 있으면 바로 사용한다. `PATCH /v1/pages/{page_id}`로 속성을 갱신하고, 본문 내용이 바뀌었다면 `PATCH /v1/blocks/{block_id}` 또는 기존 표 행을 찾아 개별 수정한다 (표 전체를 지우고 §3 template으로 다시 만들지 않는다 — 기존 내용 유실 위험).

## 5. 검증

이 규칙의 데이터베이스 ID·속성 스키마·템플릿 블록 구조는 2026-07-28에 실제 API 호출로 생성→검증→삭제한 테스트 페이지를 기준으로 확정했다.

**2026-08-05 갱신 (KD3-258)**: 실제로 auth 도메인 5개 엔드포인트 페이지를 만들면서 두 가지를 바로잡았다 — (1) `이름` 속성은 `<Method> <경로>`가 아니라 API 명칭(예: `회원가입`)이어야 한다는 걸 놓쳤다가 뒤늦게 수정, (2) 쿠키로 전달되는 값(`OIDC_AUTH_TOKEN` 등)을 HTTP Header 표에 `Cookie`라는 이름으로 잘못 적었다가, 템플릿에 별도 "Cookie" 섹션(§3)을 추가하고 5개 페이지 모두 다시 정리했다.
