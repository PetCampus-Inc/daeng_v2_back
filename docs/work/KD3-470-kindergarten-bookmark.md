> 생성: 2026-09-16 23:03 · 최종 수정: 2026-09-17 00:16

# KD3-470 북마크 기능 개발

| 항목 | 값 |
|---|---|
| Jira | `KD3-470` |
| 브랜치 | `feat/KD3-470-kindergarten-bookmark` |
| 상위 에픽 | `KD3-272` |

## 현재 제어점

- 활성 workflow: `003-migration`, `005-new-feature`
- 현재 공통 단계: `5`
- 다음 결정 또는 전환 조건: 변경분을 커밋하고 기존 PR의 계약·검증 설명을 동기화

## 작업 목표

레거시 `knockdog_server`의 유치원 북마크를 신규 Kotlin 서버로 이관한다. 보호자는 관심 유치원을 저장·해제하고, 보관함에서 카드 목록을 조회해 이름·주소 검색, 메모 여부 필터, 저장 주소 기준 거리 정렬과 비교 대상을 선택할 수 있다.

기획 산출물은 별도 PRD가 없으며, 레거시 `bookmark` 구현과 2026-09-16에 제공받은 보관함 화면을 기능 근거로 사용한다. 담당 사용자 흐름은 v2 보호자 유치원 탐색·비교 경험이며, 현재 `service.md`의 원장 중심 흐름도에는 별도 노드가 없다.

## 작업 범위

| 구분 | 내용 |
|---|---|
| API | 인증 필수 `GET /api/v1/users/me/bookmarks`, `PUT /api/v1/kindergartens/{kindergartenId}/bookmark`, `DELETE /api/v1/kindergartens/{kindergartenId}/bookmark` |
| 저장 | `bookmarks` 테이블에 `(user_code, kindergarten_id)` 유니크 1행을 저장한다. 중복 생성은 멱등 성공으로 처리한다. |
| 목록 카드 | 유치원 ID·이름·카테고리·썸네일·주소·최저 요금·블로그 리뷰 수·저장 주소별 직선거리·메모 날짜를 반환한다. 최신 저장순이고, 삭제된 유치원은 레거시처럼 목록에서 제외한다. |
| 유효성 | 존재하지 않는 유치원은 404, 폐업 유치원 생성은 400, 없는 북마크 해제는 404로 응답한다. |
| 화면 근거 | 목록은 프론트에서 이름·주소 검색, 메모 필터, 거리 기준 정렬을 수행한다. 빈 목록·검색 결과·비교 선택 UI는 프론트 상태이며 추가 서버 API가 필요 없다. |

## 작업 제외 범위

- 프론트엔드의 v0 → v1 경로 전환 및 `memoAt` 배열 → `memoDate` ISO 날짜 파싱 변경
- 비교 기록 API 변경 (`comparison` 도메인의 기존 API 사용)
- 이동수단별 소요시간 계산·외부 지도 연동
- 레거시 운영 데이터 이관, 이중 쓰기, cutover, rollback 실행
- 레거시 v0 API 제거

## 방향 논의 및 결정 사항

### 확정 사항

- 신규 서버는 v0 경로를 만들지 않고 v1 API만 제공한다(ADR 0012).
- 북마크는 유저 소유 프라이빗 기록이므로 `memo`·`comparison`과 같은 독립 `bookmark` 헥사고날 슬라이스로 둔다.
- 유저 식별자는 auth 토큰 subject인 `user_code`를 저장하며 DB FK 제약은 두지 않는다.
- 삭제는 레거시와 같이 물리 삭제한다. 다시 저장하면 새 생성 시각으로 목록의 최근순에 반영된다.
- 목록 `memoDate`는 메모 목록과 같은 `YYYY-MM-DD` 문자열이다. 화면은 날짜만 표시하므로 시각을 만들지 않는다.
- 목록 거리 형식은 레거시와 같은 소수점 첫째 자리 `km` 문자열이며, 현재 주소 모델의 `HOME`·`OTHER`만 반환한다.
- 2026-09-16 — 사용자 승인: 제공한 보관함 화면을 기능 근거로 포함하고 제안 범위로 구현 진행.
- 2026-09-17 — 저장·해제는 유치원 하위의 관계 리소스(`PUT`/`DELETE /api/v1/kindergartens/{kindergartenId}/bookmark`)로, 보관함은 현재 사용자 컬렉션(`GET /api/v1/users/me/bookmarks`)으로 공개한다. 목록 카드는 `kindergarten`과 사용자별 `memoDate`·`distances`를 중첩해 소유 관계를 드러낸다. 단일 배치 조회는 유지한다.

### 미결 질문

- 없음. 프론트 v1 전환은 이번 범위에서 제외하고 작업 후 협의 항목으로 남긴다.

### 사용자 승인 기록

- 2026-09-16 — 레거시 코드와 보관함 화면을 근거로 제안한 v1 API·데이터·검증 범위를 승인함("이 범위로 진행해줘").
- 2026-09-17 — 관계 중심 경로와 중첩 목록 계약으로 변경을 승인함("그렇게 수정해줘").

## 완료 확인 기준

- 단위 테스트로 중복 저장, 없는·폐업 유치원 생성 거부, 최근순 목록 구성, 메모 날짜·거리 구성, 없는 북마크 해제를 확인한다.
- 통합 API 테스트로 인증 필수, 관계 경로의 생성·목록·해제, 중복 생성의 단일 행 보장, 중첩 카드 계약과 오류 응답을 확인한다.
- `./gradlew clean test ktlintCheck`와 ArchUnit을 통과한다.
- 레거시 서버 로컬 기동이 가능하면 GET·POST·DELETE의 응답 의미를 대조한다. 실행할 수 없으면 소스 대조 결과와 제한을 기록한다.

## 구현 결과

- `domain/bookmark` 정석형 헥사고날 슬라이스를 추가했다. 저장·해제·목록 유스케이스와 JPA 영속성, kindergarten·memo·auth 주소 조회 어댑터를 분리했다.
- `V14__create_bookmarks.sql`은 `bookmarks` 테이블과 `(user_code, kindergarten_id)` 유니크, 사용자별 최신 저장순 인덱스를 만든다. `BaseEntity` 컬럼을 따르되 해제는 물리 삭제한다.
- 목록은 bookmark 저장 순서를 보존하면서 현재 존재하는 유치원만 카드로 조립한다. 유치원 카드 루트·카테고리·요금은 배치 조회해 목록 크기에 비례한 쿼리를 만들지 않는다. 메모는 `memoDate`, 거리는 `HOME` 우선의 `HOME`·`OTHER` 직선거리 문자열로 반환한다.
- 관계 변경은 `PUT`·`DELETE /api/v1/kindergartens/{kindergartenId}/bookmark`로, 보관함 목록은 `GET /api/v1/users/me/bookmarks`로 변경했다. 목록 응답은 `kindergarten` 카드와 사용자별 `memoDate`·`distances`를 중첩한다.

## 검증 결과

- 2026-09-16 — `./gradlew clean test ktlintCheck` 통과. 새 단위·통합 테스트와 ArchUnit·ktlint를 포함한다.
- 2026-09-17 — 관계 경로와 중첩 목록 계약 변경 후 `./gradlew clean test ktlintCheck`를 다시 통과했다.
- 레거시 서버 로컬 HTTP 응답 대조는 실행하지 못했다. 레거시 Redis·신규 서버의 로컬 실행 환경이 분리되어 있어 컨트롤러·서비스·DTO 소스와 현재 프론트 소비처를 대조했다.
- Notion API 명세는 `API_NOTION_KEY`가 현재 환경에 없어 등록하지 못했다. 키가 주입된 환경에서 v1 엔드포인트 3개를 등록해야 한다.

## 독립 리뷰

- 2026-09-16 — 구현과 분리된 리뷰에서 bookmark 슬라이스, V14 마이그레이션, 유치원 카드 배치 조회, 테스트·문서와 레거시·프론트 소비 경로를 대조했다.
- 인증·사용자 소유 삭제·중복 저장·정렬·폐업/미존재 오류 처리가 계획과 일치하고, 배치 조회가 목록 조립의 N+1을 피하면서 헥사고날 경계를 유지함을 확인했다. blocking/required finding 없이 승인했다.
- 2026-09-17 — 계약 변경분을 별도 리뷰에서 다시 대조했다. 새 경로가 기본 인증 경계를 유지하고, 사용자 소유 삭제·배치 조회·정렬·누락 유치원 제외가 회귀하지 않으며, 중첩 JSON 계약과 문서가 일치함을 확인했다. blocking finding 없이 승인했다.

## 작업 후 확인 목록

| 문서 | 판정 | 근거 |
|---|---|---|
| `docs/domains/bookmark.md` | 갱신 | 도메인 경계·불변식·v1 계약·데이터 제약을 기록했다. |
| `docs/inventory/api.md` | 갱신 | 레거시 bookmark API의 v1 이관 경로·진척을 반영했다. |
| `docs/inventory/database.md` | 갱신 | 레거시 `bookmark`의 `bookmarks` 재설계·진척을 반영했다. |
| Notion API 명세 | 해당 없음 | `API_NOTION_KEY`가 없어 v1 엔드포인트 3개를 등록하지 못했다. |
| `docs/service.md` | 확인했지만 변경 없음 | 보관함은 기존 보호자 탐색 경험의 일부라 제품 범위 변경은 없다. |
| `docs/conventions/*`, `docs/architecture/*`, `docs/rules/*` | 확인했지만 변경 없음 | 기존 정석형 헥사고날·응답·예외·JPA 규칙을 적용했다. |
