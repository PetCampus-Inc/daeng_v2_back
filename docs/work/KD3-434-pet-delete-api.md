> 생성: 2026-09-02 19:24 · 최종 수정: 2026-09-08 13:40

# KD3-434 pet 삭제 API 구축

| 항목 | 값 |
|---|---|
| Jira | `KD3-434` |
| 브랜치 | `feat/KD3-434-pet-delete-api` (2026-09-08 생성, 사용자 지시로 AI가 생성 — 통상 절차와 달리 사람이 Jira에서 미리 만들어 제공하지 않음) |
| 상위 에픽 | `KD3-404` |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `2`
- 다음 결정 또는 전환 조건: 구현·테스트·로컬 HTTP e2e 검증·문서 갱신 전부 완료(2026-09-08). 아래 "구현 및 검증 결과" 참고. 다음: 커밋·푸시 확인, PR 생성.

## 작업 목표

인증된 사용자가 자신의 pet을 안전하게 soft delete할 수 있는 v1 API를 제공한다.

## 작업 범위

- `DELETE /api/v1/pets/{petId}`를 구현한다.
- 소유권과 활성 상태를 검증한 뒤 soft delete한다.
- 삭제된 pet이 이후 목록·단건 조회와 대표견 설정에서 제외되도록 보장한다.
- 삭제 대상이 대표견이면, 같은 락 구간 안에서 남은 활성 pet 중 정렬 1순위를 새 대표견으로 승격한다(남은 pet이 없으면 대표견 없음 상태로 둔다).

## 작업 제외 범위

- 물리 삭제와 데이터 backfill
- 삭제된 pet 복구
- `school_pet_membership` 연결 해제·대기 신청 취소
- 레거시 v0 삭제 API 호환

## 방향 논의 및 결정 사항

### 확정 사항

- 삭제는 HTTP DELETE와 soft delete를 사용한다.
- 유치원/owner-member 부수효과는 해당 도메인 후속 작업에서 추가한다.
- **삭제된 대표견은 자동으로 다른 pet으로 승격한다(2026-09-08 확정, 기존 "안 함" 기본안 뒤집음)**: 남은 활성 pet 중 정렬 1순위를 새 대표견으로 지정한다. 정렬 기준은 `GetPetsService`(KD3-432)가 쓰는 것과 동일한 `대표견 우선 → 이름순` 비교자를 값으로 재사용한다(코드 재사용은 아님 — 432와 434가 서로 다른 브랜치 계보라 직접 import 불가, `PetPersistenceAdapter` 안에 같은 식을 독립적으로 둠. epic 병합 시점에 `Pet.kt`로 추출해 중복 제거하는 걸 후속으로 남긴다) — 대표견 자신이 삭제 대상이라 실질적으로는 "남은 pet 중 이름순 1번"과 같다. 레거시 3단계 정렬 중 아직 구현 안 된 유치원 연결 기준은 그대로 미반영 상태로 둔다(아래 별도 항목 참고). **레거시 확인(2026-09-08)**: `daeng_v1_back`의 `PetService.removePet`은 자동 승격 로직이 전혀 없다(대표견 삭제 시 그냥 대표견 없음 상태로 남고, 사용자가 직접 재설정해야 함) — 이번 자동 승격은 레거시 재현이 아니라 v2에서 새로 추가하는 개선이다.
- **삭제는 대표견 여부로 두 경로로 분기한다(2026-09-08 확정, 구현 완료)**: 대표견이 아닌 pet은 `UpdatePetService`와 같은 방식으로 `@Version` 낙관적 락만으로 가볍게 삭제한다(`SavePetPort.save`). 대표견인 pet만 KD3-433 `setRepresentativeWithinLock`과 같은 락 패턴(`users` 행 비관적 락 → 활성 pet 재조회 → 처리)을 새 메서드 `SavePetPort.deleteAndPromoteWithinLock`으로 재사용해 삭제+승격을 하나의 트랜잭션·락 구간으로 묶는다. 근거: 이미 존재하는 행을 수정하는 삭제는 register(행이 아직 없어 잠글 대상이 없는 경우)와 달리 InnoDB가 대상 행에 거는 암묵적 배타 락만으로도 `registerWithinLimit`의 최대 마릿수 검사와 안전하게 상호작용한다(최악의 경우 스퓨리어스 거절 정도, 데이터 오염 없음). Testcontainers 동시성 테스트(`PetDeleteAndPromoteConcurrencyTest`)로 실증.
- **레거시 3단계 정렬용 컬럼(유치원 연결 상태)은 지금 미리 확장하지 않는다(2026-09-08 확정)**: 아직 도메인에 없는 개념이라 채울 로직이 없고, 이 상태가 `pets` 컬럼이 아니라 `school_pet_membership` 같은 별도 연결 테이블에서 파생될 가능성이 커 미리 넣으면 위치가 틀릴 수 있다. `docs/domains/pet.md`에 이미 남겨둔 후속 과제("유치원 연결 상태 도메인 들어오면 재검토")에서 스키마·로직을 함께 결정한다.
- **남은 pet이 하나도 없으면 대표견 없음 상태로 둔다(2026-09-08 확정)**: 승격 대상이 없을 뿐, 별도 처리 불필요.
- 작업 브랜치는 `feat/KD3-433-pet-representative-api` 위에 stacked 브랜치로 진행한다(2026-09-08 정정 — 상세는 위 "현재 제어점" 참고). 소유권·미존재 검증엔 KD3-431에서 만들어져 433이 그대로 이어받은 `PetErrorCode`/`LoadPetPort`/`SavePetPort`/`PetResponse`를 쓴다.
- **응답 형식은 `204 No Content`로 확정(2026-09-08, 구현 중 결정)**: 삭제 API는 표준 REST 관례를 따라 바디 없이 상태 코드만 반환한다. 승격 결과(누가 새 대표견이 됐는지)를 클라이언트에 알려줄 필요가 있는지는 실제 프론트 요구가 확인되면 별도로 다룬다.
- **`representative_user_id` UNIQUE 제약은 soft delete와 무관하게 걸린다(2026-09-08 발견)**: `V11__create_pets.sql`의 `uk_pets_representative_user`는 `deleted_at` 조건이 없는 순수 UNIQUE라, 대표견을 삭제할 때 그 pet의 `representative_user_id`를 null로 안 지우면 그 값이 영구히 슬롯을 차지해 이후 승격이 전부 UNIQUE 충돌로 깨진다. `Pet.delete()`가 `isRepresentative`를 `false`로 같이 지우고(`PetMapper`가 `representativeUserId = if (isRepresentative) userId else null`로 매핑) `deleteAndPromoteWithinLock`이 삭제 저장과 승격 저장 사이에 명시적 `flush()`를 넣어(기존 `setRepresentativeWithinLock`의 flush-순서 버그와 같은 클래스) 처리했다.

### 사용자 승인 기록

- 2026-09-02: 사용자가 pet 삭제를 독립 유스케이스로 분리하고 유치원 연계를 후속으로 미뤘다.

## 구현 및 검증 결과

- `DeletePetUseCase`/`DeletePetService`/`DeletePetController`(`DELETE /api/v1/pets/{petId}`, 204) 추가. `SavePetPort`에 `deleteAndPromoteWithinLock(pet): Pet?` 추가, `PetPersistenceAdapter`에 구현.
- 단위 테스트: `DeletePetServiceTest` 5건(대표견/비대표견 경로 분기, NOT_FOUND, 삭제된 pet, NOT_AUTHORIZED).
- `PetPersistenceAdapterTest`에 4건 추가: 이름순 승격, 남은 pet 없을 때 대표견 없음, 승격 후 UNIQUE 제약 안 걸림, 비대표견 삭제는 기존 대표견에 영향 없음.
- Testcontainers 동시성 테스트(`PetDeleteAndPromoteConcurrencyTest`) 신규: 대표견 삭제와 다른 pet의 `setRepresentativeWithinLock`을 동시에 실행해도 예외 없이 대표견 1건만 남고 삭제된 pet의 `representative_user_id`가 정상적으로 null임을 확인. 1회 실행에 바로 통과(flush-순서 버그 재현 없음).
- 전체 빌드(`./gradlew build`) 162건 전부 통과(기존 152건 + 신규 10건), 실패·에러 0건.

## 로컬 HTTP e2e 검증 결과

로컬 MySQL(Docker)·`./gradlew bootRun --args='--spring.profiles.active=local'`로 실제 서버를 띄우고, HS256 JWT를 직접 서명해 실제 HTTP 요청으로 검증했다(테스트 사용자·pet은 검증 후 DB에서 직접 삭제).

- 사용자 1명에게 pet 4마리(가온·나비·다롱·라울, 가온이 자동 대표견) 등록.
- **비대표견 삭제**: `다롱` 삭제 → 204. 대표견(가온) 영향 없음.
- **대표견 삭제**: `가온` 삭제 → 204. DB 직접 확인 — 가온은 `deleted_at` 세팅·`representative_user_id` null, 남은 `나비`·`라울` 중 이름순으로 `나비`가 새 대표견(`representative_user_id` = user id)으로 승격됨. `라울`은 그대로 비대표견.
- **이미 삭제된 pet 재삭제**: 404 `PET-404-1`.
- **존재하지 않는 pet**: 404 `PET-404-1`.
- **인증 없이 요청**: 401.
- **타인 pet 삭제 시도**: 403 `PET-403-1`.

## 완료 확인 기준

- [x] 정상 삭제, 타인 pet 거부, 이미 삭제된 pet 처리, 삭제 후 목록·단건 비노출을 테스트한다. — 단위·H2 테스트 + 로컬 HTTP e2e로 확인.
- [x] 대표견 삭제 시 남은 pet 중 정렬 1순위로 자동 승격되는지, 승격 대상이 없을 때 대표견 없음 상태가 되는지 테스트한다.
- [x] 삭제+승격이 동시 요청(예: 삭제와 동시에 다른 pet에 `PUT .../representative`)에도 대표견 유일성을 유지하는지 검증한다. — Testcontainers 동시성 테스트로 확인.
- [x] API·데이터·pet 도메인 문서 영향을 판정·기록한다.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-434-pet-delete-api.md` | 갱신 | API 결정·구현·검증 결과 기록 |
| `docs/inventory/api.md` | 갱신 | `/api/v0/pet/remove/{petId}` 판정을 `미착수`→`진행중`으로 갱신, v1 구현 완료 기록 |
| `docs/inventory/database.md` | 갱신 불필요 | 신규 마이그레이션 없음 — 기존 `deleted_at`/`representative_user_id`(V11) 재사용 |
| `docs/domains/pet.md` | 갱신 | "pet 삭제 API" 절 추가, 삭제 행에서 KD3-434로 링크 |
| `SavePetPort.kt` | 갱신 | `deleteAndPromoteWithinLock` 추가 |
| `PetPersistenceAdapter.kt` | 갱신 | `deleteAndPromoteWithinLock` 구현 |
| `DeletePetUseCase.kt`/`DeletePetService.kt`/`DeletePetController.kt` | 신규 | 삭제 유스케이스·서비스·컨트롤러 |
| `DeletePetServiceTest.kt` | 신규 | 서비스 단위 테스트 5건 |
| `PetPersistenceAdapterTest.kt` | 갱신 | `deleteAndPromoteWithinLock` 테스트 4건 추가 |
| `PetDeleteAndPromoteConcurrencyTest.kt` | 신규 | 삭제+승격 vs 대표견 설정 동시 요청 Testcontainers 테스트 |
