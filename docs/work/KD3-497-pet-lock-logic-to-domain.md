> 생성: 2026-09-09 · 최종 수정: 2026-09-09

# KD3-497 pet 도메인 락 로직 리치 도메인 모델로 재배치

| 항목 | 값 |
|---|---|
| Jira | `KD3-497` |
| 브랜치 | `refactor/KD3-497-pet-lock-logic-to-domain` (2026-09-09 생성, 사용자 지시로 AI가 생성 — `epic/KD3-404-pet-domain-migration`의 최신 origin tip 위에서 분기) |
| 상위 에픽 | `KD3-404`(이미 `dev`로 PR #23 대기 중 — 이 리팩터링은 그 이후 완료된 pet 도메인 코드를 대상으로 함) |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `5`(구현 착수)
- 다음 결정 또는 전환 조건: 설계 논의·결정 전부 완료, 사용자 승인 받음(2026-09-09). 구현 착수.

## 작업 목표

`PetPersistenceAdapter`의 락 메서드(`registerWithinLimit`/`setRepresentativeWithinLock`/`deleteAndPromoteWithinLock`)에 남아있는 비즈니스 판단 로직(누가 대표견이 될지 등)을 리치 도메인 모델 원칙에 맞게 도메인(`Pet.kt`)과 서비스(오케스트레이션) 계층으로 재배치한다. **동작 변경 없음이 목표** — 새 기능이나 버그 수정이 아니라 순수 구조 개선.

## 작업 범위

- `SavePetPort`에 `saveAndFlush(pet): Pet` 추가(`entityManager.flush()`를 감싼 프리미티브)
- `LoadPetPort`에 `findAllActiveByUserIdForUpdate(userId): List<Pet>` 추가(잠금 재조회를 포트로 노출)
- `Pet.kt` companion에 `selectNextRepresentative(candidates: List<Pet>): Pet?` 순수 함수 추가
- 락 패턴("users 행 잠그고 활성 pet 재조회")을 공유하는 `PetLockOperations` 헬퍼 추가
- `CreatePetService`/`SetRepresentativeService`/`DeletePetService`가 판단 로직을 직접 갖고 `PetLockOperations`·`Pet.selectNextRepresentative`·`SavePetPort`를 오케스트레이션하도록 재작성
- `PetPersistenceAdapter`에서 `registerWithinLimit`/`setRepresentativeWithinLock`/`deleteAndPromoteWithinLock` 제거 — 순수 I/O(`save`/`saveAndFlush`/조회)만 남김
- 관련 테스트 재구성(아래 "확정 사항"의 테스트 재구성 방식·동시성 테스트 전환 항목 참고)

## 작업 제외 범위

- 동작 변경(새 기능·버그 수정) — 이번 범위 아님, behavior parity가 목표
- pet 도메인 밖(kindergarten, auth 등)의 유사 패턴 — 발견되면 후속 과제로만 기록
- `userCode`→`userId` 변환 중복 등 이미 문서화된 다른 후속 과제 — 이번 범위 아님

## 방향 논의 및 결정 사항

### 확정 사항

- `SavePetPort.saveAndFlush`/`LoadPetPort.findAllActiveByUserIdForUpdate` 신설, `Pet.selectNextRepresentative` 도메인 함수 신설, `PetLockOperations` 공유 헬퍼 신설(같은 패턴이 3번 반복되는 지점이라 rule of three 근거로 추출) — 이 대화에서 CTO 관점 설계 검토를 거쳐 확정(2026-09-08)
- 비용(브랜치 작업량·테스트 재작성)은 신경 쓰지 않고 진행 — "초반부터 아키텍처 틀을 제대로 잡고 싶다"는 사용자 판단(2026-09-08)
- `epic/KD3-404-pet-domain-migration`이 KD3-431~434 전부 머지된 뒤, 그 최신 상태 위에서 새 브랜치로 진행(2026-09-09) — epic 위에 다른 미머지 브랜치가 없음을 확인(PR #24는 `dev` 기반이라 무관)

- **테스트 재구성 방식(2026-09-09 확정)**: 테스트 피라미드 원칙대로 계층마다 자기 책임만 검증하도록 재배치한다.
  - 순수 규칙("누가 선정되는가")은 `Pet.selectNextRepresentative` 대상으로 `PetTest.kt`에 순수 도메인 테스트 추가
  - 오케스트레이션("서비스가 락 잡고 → 규칙 적용 → 순서대로 저장하는가")은 서비스 레벨 테스트(Fake 포트)
  - 영속성(SQL·JPA 매핑)은 어댑터 레벨(`@DataJpaTest`)
  - `PetPersistenceAdapterTest`의 기존 비즈니스 로직 테스트(예: "첫 등록은 자동으로 대표견이 된다")는 삭제하고 위 세 계층으로 커버리지 재배치
- **동시성 테스트 3개는 서비스(유스케이스 인터페이스)를 통해 호출하도록 변경(2026-09-09 확정)**: `PetRegistrationConcurrencyTest`/`PetSetRepresentativeConcurrencyTest`/`PetDeleteAndPromoteConcurrencyTest`가 지금은 `petPersistenceAdapter.registerWithinLimit(...)`처럼 어댑터를 직접 호출해 서비스 계층(소유권 검증 등)을 건너뛰고 있었다. `CreatePetUseCase`/`SetRepresentativeUseCase`/`DeletePetUseCase`를 오토와이어해 실제 프로덕션 요청 경로 전체를 동시에 실행하도록 바꾼다 — 어댑터 단위 안전성이 아니라 실제 요청 경로 전체의 동시성 안전성을 증명하는 더 정직한 테스트가 된다. `LoadUserPort` 등 추가 Spring 배선 필요.
- **`PetLockOperations`은 인터페이스로 빼지 않고 평범한 클래스로 둔다(2026-09-09 확정)**: 포트는 "구현체를 바꿔 낄 수 있어야 하거나 인프라에 직접 닿는" 경우에 쓰는 것인데, `PetLockOperations`는 인프라에 직접 안 닿고 이미 포트인 `LockUserPort`/`LoadPetPort`를 조합만 한다 — 테스트도 그 안의 두 포트를 Fake로 바꾸면 되니 추가 인터페이스가 불필요하다. `application` 패키지의 평범한 `@Component` 클래스로 둔다.
- **검증 범위는 기존 관례를 그대로 유지(2026-09-09 확정)**: "동작 변경 없음"이 목표인 리팩터링일수록 회귀가 조용히 숨어들기 쉬워서, 독립 리뷰(fresh subagent) + 로컬 HTTP e2e까지 다른 티켓과 동일하게 전부 수행한다.
- **`errorCode` 미검증 테스트 패턴도 이 작업에서 같이 고친다(2026-09-09 확정)**: `CreatePetServiceTest`/`UpdatePetServiceTest`/`SetRepresentativeServiceTest`/`GetPetServiceTest`가 `assertFailsWith<BusinessException>`만 확인하고 `errorCode`는 검증하지 않던 것(PR #23 Notes에 후속 과제로 기록) — 어차피 이 서비스들을 전부 손대게 되므로 같이 반영한다.

### 사용자 승인 기록

- 2026-09-09: 사용자가 위 5개 미결 질문(테스트 재구성 방식, 동시성 테스트 전환, `PetLockOperations` 클래스 형태, 검증 범위, `errorCode` 미검증 패턴 동시 수정) 전부 확정하고 구현 착수를 승인했다.

## 완료 확인 기준

- `registerWithinLimit`/`setRepresentativeWithinLock`/`deleteAndPromoteWithinLock`이 `PetPersistenceAdapter`에서 사라지고, 같은 동작이 `CreatePetService`/`SetRepresentativeService`/`DeletePetService`에서 재현됨을 테스트로 확인한다.
- `Pet.selectNextRepresentative`가 도메인 단위 순수 테스트로 검증된다.
- 동시성 테스트 3개가 서비스 계층을 통해 실행되고, 기존과 동일한 안전성(스퓨리어스 없음, 유일성 보장)을 증명한다.
- `CreatePetServiceTest`/`UpdatePetServiceTest`/`SetRepresentativeServiceTest`/`GetPetServiceTest`/`DeletePetServiceTest`가 전부 `errorCode`까지 단언한다.
- 전체 빌드(`./gradlew build`)가 리팩터링 전과 동일하거나 그 이상의 테스트 건수로 통과한다(실패·에러 0건).
- 독립 리뷰(fresh subagent)와 로컬 HTTP e2e로 API 동작이 리팩터링 전후 동일함을 확인한다.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-497-pet-lock-logic-to-domain.md` | 갱신 | 결정 사항·구현·검증 결과 기록 |
| `SavePetPort.kt` | 갱신 | `saveAndFlush` 추가, `registerWithinLimit`/`setRepresentativeWithinLock`/`deleteAndPromoteWithinLock` 제거 |
| `LoadPetPort.kt` | 갱신 | `findAllActiveByUserIdForUpdate` 추가 |
| `PetPersistenceAdapter.kt` | 갱신 | 순수 I/O만 남김 |
| `Pet.kt` | 갱신 | `selectNextRepresentative` companion 함수 추가 |
| `PetLockOperations.kt` | 신규 | 락+재조회 공유 헬퍼 |
| `CreatePetService.kt`/`SetRepresentativeService.kt`/`DeletePetService.kt` | 갱신 | 판단 로직 이동, 오케스트레이션 재작성, `errorCode` 관련 개선 반영 |
| `PetTest.kt` | 갱신 | `selectNextRepresentative` 순수 테스트 추가 |
| `PetPersistenceAdapterTest.kt` | 갱신 | 이동된 비즈니스 로직 테스트 삭제, 순수 I/O 테스트만 남김 |
| `PetRegistrationConcurrencyTest.kt`/`PetSetRepresentativeConcurrencyTest.kt`/`PetDeleteAndPromoteConcurrencyTest.kt` | 갱신 | 서비스(유스케이스) 계층 호출로 전환 |
| `CreatePetServiceTest.kt`/`UpdatePetServiceTest.kt`/`SetRepresentativeServiceTest.kt`/`GetPetServiceTest.kt`/`DeletePetServiceTest.kt` | 갱신 | `errorCode` 단언 추가 |
