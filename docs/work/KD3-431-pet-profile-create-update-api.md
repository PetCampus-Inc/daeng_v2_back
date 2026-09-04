> 생성: 2026-09-02 19:24 · 최종 수정: 2026-09-04 19:29

# KD3-431 pet 프로필 생성·수정 API 구축

| 항목 | 값 |
|---|---|
| Jira | `KD3-431` |
| 브랜치 | `feat/KD3-431-pet-profile-create-update-api` |
| 상위 에픽 | `KD3-404` |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `2`
- 다음 결정 또는 전환 조건: 미결 질문(`relationship` ETC→비ETC 전환 시 `relationshipText` 자동 제거 여부) 확정 후 구현을 시작한다.

## 작업 목표

인증된 사용자가 자신의 pet 프로필을 생성하고 부분 수정할 수 있는 v1 REST API를 제공한다.

## 작업 범위

- `POST /api/v1/pets` 생성 API를 구현한다.
- `PATCH /api/v1/pets/{petId}` 부분 수정 API를 구현한다.
- 소유권, 최대 5마리, 견종 존재, `relationship=ETC`일 때 관계 직접 입력값 검증을 적용한다.
- 요청에서 누락한 수정 필드는 유지하는 PATCH 의미와 `null` 처리 규칙을 명세·테스트한다.

## 작업 제외 범위

- 레거시 v0 생성·수정 API 호환
- 대표견 설정, 삭제, 목록·단건 조회
- 유치원 소속·연결 상태

## 방향 논의 및 결정 사항

### 확정 사항

- 생성은 `POST`, 프로필 부분 수정은 `PATCH`를 사용한다.
- 단순 필드 변경은 POST 명령 API로 만들지 않는다.
- 신규 프론트는 최종 전환 때 v1 계약으로 연결한다.
- 작업 브랜치는 `dev`가 아닌 `feat/KD3-430-pet-domain-foundation-schema`에서 분기한 stacked 브랜치로 진행한다. 이 티켓의 생성·수정 API가 KD3-430의 도메인 모델·포트·persistence 어댑터에 기능적으로 의존하기 때문이다. PR base는 KD3-430이 `dev`에 머지되기 전까지 `feat/KD3-430-pet-domain-foundation-schema`로 두고, 머지 후 재조정한다. KD3-430에 추가 커밋이 발생하면 그 위로 rebase한다.

### 미결 질문

- `relationship`이 `ETC`가 아닌 값으로 바뀔 때 기존 `relationshipText`를 자동 제거할지 구현 전에 확정한다.

### 사용자 승인 기록

- 2026-09-02: 사용자가 신규 pet API를 RESTful v1로 설계하는 방향을 승인했다.

## 완료 확인 기준

- 생성·수정 정상 경로와 소유권 위반, 마릿수 초과, 미존재 견종, 관계 입력 오류를 테스트한다.
- PATCH의 필드 누락·명시적 null 처리와 응답 계약을 테스트한다.
- API 계약 문서와 인벤토리 영향을 판정·기록한다.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-431-pet-profile-create-update-api.md` | 갱신 | API 결정·검증 결과 기록 |
| `docs/inventory/api.md` | 구현 시 갱신 | 신규 v1 공개 API |
| `docs/domains/pet.md` | 구현 시 갱신 | 생성·수정 불변식 |
