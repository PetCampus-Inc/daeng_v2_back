> 생성: 2026-09-09 17:30 · 최종 수정: 2026-09-09 17:30

# KD3-500 pet 사용자 식별자 타입 경계 정리

| 항목 | 값 |
|---|---|
| Jira | `KD3-500` |
| 브랜치 | `refactor/KD3-500-jwt-principal-user-id` |
| 상위 에픽 | 해당 없음 |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `4` (검증 완료)
- 다음 결정 또는 전환 조건: 사용자 확인 후 커밋·PR 준비 여부 결정

## 작업 목표

pet 서비스에서 사용자 조회 결과의 내부 식별자를 원시 `Long`으로 즉시 풀지 않고 `UserId`로 유지한다. 실제 pet 포트가 `Long`을 요구하는 경계에서만 `.value`를 꺼낸다.

## 작업 범위

- `CreatePetService`, `UpdatePetService`, `GetPetService`, `GetPetsService`, `SetRepresentativeService`, `DeletePetService`의 `requireUserId` 반환 타입을 `UserId`로 변경한다.
- 각 서비스의 pet 포트 호출 직전에만 `UserId.value`를 전달한다.

## 작업 제외 범위

- JWT subject, Spring Security principal, access/refresh token, Redis refresh token 구조 변경
- controller·command·포트의 공개 인터페이스 변경
- pet 포트의 `Long` 파라미터를 `UserId`로 전환하는 리팩터링

## 방향 논의 및 결정 사항

### 확정 사항

- `UserId`는 현재 `Long`을 감싸지만, 사용자 식별자라는 타입 정보를 서비스 내부에서 유지한다.
- JPA·pet 포트가 현재 `Long`을 요구하므로 해당 경계에서만 `.value`를 사용한다.

### 사용자 승인 기록

- 2026-09-09: 사용자가 앞선 JWT/principal 전환 작업을 취소하고, `requireUserId`의 `Long → UserId` 반환 전환만 진행하도록 지시했다.

## 완료 확인 기준

- 6개 pet 서비스의 `requireUserId`가 `UserId`를 반환한다.
- pet 포트 호출은 동일한 `Long` 값을 전달한다.
- pet application service 테스트가 통과한다.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-500-user-id-value-object-boundary.md` | 갱신 | 범위·결정·검증 결과 기록 |
| `docs/domains/pet.md` | 확인했지만 변경 없음 | 장기 구조와 포트 타입은 바뀌지 않음 |
| `docs/inventory/api.md` | 확인했지만 변경 없음 | 공개 API 계약 변경 없음 |
