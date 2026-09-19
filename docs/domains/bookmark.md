> 생성: 2026-09-16 23:11 · 최종 수정: 2026-09-17 00:11

# bookmark 도메인 마이그레이션 지시서

보호자가 탐색한 유치원을 보관함에 저장하는 유저 소유 프라이빗 기록을 담당한다. 보관함 화면은 이름·주소 검색, 메모 여부 필터, 저장 주소 기준 거리 정렬, 비교 대상 선택에 이 목록을 사용한다.

- 설계 근거: [`0003`](../adr/0003-헥사고날-정석형-통일.md), [`0010`](../adr/0010-신규-db-인스턴스-스키마-재작성.md), [`0012`](../adr/0012-신규-서버-v0-미제공-원칙.md)
- 원본: `knockdog_server`의 `bookmark` 패키지
- 착수 기록: [`KD3-470`](../work/KD3-470-kindergarten-bookmark.md)

## 0. 담당 데이터

| 저장소 | 이름 | 비고 |
|---|---|---|
| MySQL (JPA) | `bookmarks` (V14) | `(user_code, kindergarten_id)` 유니크 1행. `user_code`는 auth 토큰 subject(`UserCode`), `kindergarten_id`는 `kindergartens.naver_place_id`. `BaseEntity`를 상속하고 FK 제약은 없다. 해제는 물리 삭제이며 재저장하면 새 행의 생성 시각으로 최근순에 반영된다. |

## 1. 불변식 · 제약

| 항목 | 규칙 |
|---|---|
| 대상 | 존재하고 `ACTIVE`인 유치원만 저장한다. 없으면 404 `RESOURCE_NOT_FOUND`, 폐업이면 400 `BOOKMARK_CLOSED_SCHOOL`이다. |
| 중복 | DB 유니크와 `INSERT ... ON DUPLICATE KEY UPDATE id = id`로 중복 저장을 멱등 성공 처리한다. 기존 생성·수정 시각은 바꾸지 않는다. |
| 해제 | 현재 사용자와 유치원 ID가 일치하는 행만 물리 삭제한다. 없으면 404 `BOOKMARK_NOT_FOUND`다. |
| 목록 | `created_at DESC, id DESC` 순서를 보존한다. 유치원이 이후 사라졌다면 행은 남겨도 응답 카드에서는 제외한다. |
| 카드 조립 | 유치원은 `LoadKindergartenPort.findCardSummariesByNaverPlaceIds`로 루트·카테고리·요금을 배치 조회하고, 메모 날짜는 `LoadMemoPort`, 저장 주소는 `LoadUserPort`를 각 outbound adapter에서 조회한다. 목록 유스케이스는 다른 도메인의 JPA 구현에 직접 의존하지 않는다. |
| 거리 | 유치원 좌표와 현재 사용자의 `HOME`·`OTHER` 주소로 직선거리를 소수점 첫째 자리 `km` 문자열로 계산한다. 좌표가 없으면 `distances`는 빈 배열이다. |
| 인가 | `/api/v1/users/me/bookmarks`, `/api/v1/kindergartens/{kindergartenId}/bookmark`는 `SecurityConfig.PUBLIC_ENDPOINTS`에 포함하지 않아 인증이 필수다. |

## 2. 이 서버가 제공하는 API

모든 응답은 `Response<T>`로 감싼다. 아래는 `data` 내부다.

| 신규 (`v1`) | 레거시 (`v0`) | 설명 |
|---|---|---|
| `GET /api/v1/users/me/bookmarks` | `GET /api/v0/bookmark` | 최신 저장순 카드 목록. `[{kindergarten:{id,name,thumbnailS3Key,categories,location,price,reviewCount},memoDate,distances:[{referencePoint,distance}]}]` |
| `PUT /api/v1/kindergartens/{kindergartenId}/bookmark` | `POST /api/v0/bookmark/{id}` | 유치원에 대한 관심 관계를 저장한다. 중복 저장도 200 성공 |
| `DELETE /api/v1/kindergartens/{kindergartenId}/bookmark` | `DELETE /api/v0/bookmark/{id}` | 유치원에 대한 관심 관계를 해제한다 |

## 3. 레거시 대비 계약 변경

| 항목 | 레거시 | v1 |
|---|---|---|
| 경로 | `/api/v0/bookmark` | 목록은 `/api/v1/users/me/bookmarks`, 관계 변경은 `/api/v1/kindergartens/{kindergartenId}/bookmark` |
| 응답 래퍼 | 목록은 `Response`, 저장·해제는 `BasicInfoResponseDto` | 전부 `Response<T>` |
| 메모 날짜 | `memoAt` 날짜·시각 배열 | `memoDate` ISO 날짜 문자열. 보관함은 날짜만 표시하므로 시각을 만들지 않는다 |
| 거리 | `HOME`·`WORK`·`OTHER` 가능, `transitTimes` 프론트 타입에만 존재 | 신규 주소 모델에 맞춰 `HOME`·`OTHER`만 반환, 이동수단 시간은 미제공 |
| 삭제 없음 | 200 실패 엔벨로프 `BOOKMARK_NOT_FOUND` | 404 `BOOKMARK_NOT_FOUND` |
| 생성 중복 | 별도 성공 코드 `BOOKMARK_ALREADY_EXISTS` | 일반 200 성공. 프론트는 성공 코드로 분기하지 않는다 |

## 4. 후속

- 프론트가 `bookmark` API 경로를 v1으로 전환하고 `memoAt` 배열 대신 `memoDate` 문자열을 사용해야 한다.
- Notion API 명세서에 v1 엔드포인트 3개를 등록해야 한다. 현재 작업 환경에는 `API_NOTION_KEY`가 없다.
- 레거시 운영 데이터 이관과 cutover는 신규 DB 단발 전환 방침에 따라 별도 작업에서 결정한다.
