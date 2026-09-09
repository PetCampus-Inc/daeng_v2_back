> 생성: 2026-09-09 · 최종 수정: 2026-09-09 (PR 리뷰 반영: 범위 문구·검증 근거 정정)

# KD3-497 pet 도메인 락 로직 리치 도메인 모델로 재배치

| 항목 | 값 |
|---|---|
| Jira | `KD3-497` |
| 브랜치 | `refactor/KD3-497-pet-lock-logic-to-domain` (2026-09-09 생성, 사용자 지시로 AI가 생성 — `epic/KD3-404-pet-domain-migration`의 최신 origin tip 위에서 분기) |
| 상위 에픽 | `KD3-404`(이미 `dev`로 PR #23 대기 중 — 이 리팩터링은 그 이후 완료된 pet 도메인 코드를 대상으로 함) |
| PR | [#25](https://github.com/PetCampus-Inc/daeng_v2_back/pull/25) (`refactor/KD3-497-pet-lock-logic-to-domain` → `epic/KD3-404-pet-domain-migration`, 2026-09-09 생성) |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `5`(독립 리뷰·문서 동기화 완료, PR #25 생성 완료)
- 다음 결정 또는 전환 조건: PR 리뷰·머지 대기

## 작업 목표

`PetPersistenceAdapter`의 락 메서드(`registerWithinLimit`/`setRepresentativeWithinLock`/`deleteAndPromoteWithinLock`)에 남아있는 비즈니스 판단 로직(누가 대표견이 될지 등)을 리치 도메인 모델 원칙에 맞게 도메인(`Pet.kt`)과 서비스(오케스트레이션) 계층으로 재배치한다. **의도한 API 동작 변경은 없음이 목표** — 새 기능을 추가하거나 API 응답·검증 규칙을 바꾸는 게 아니라 순수 구조 개선이다. 다만 구현 과정에서 실제 동시성 결함이 발견되면 그 수정은 이 범위에 포함한다(아래 "작업 범위" 참고) — 구조만 옮기고 기존 결함을 그대로 방치하면 "동작 유지"라는 목표 자체가 깨지기 때문이다.

## 작업 범위

- `SavePetPort`에 `saveAndFlush(pet): Pet` 추가(`entityManager.flush()`를 감싼 프리미티브)
- `LoadPetPort`에 `findAllActiveByUserIdForUpdate(userId): List<Pet>` 추가(잠금 재조회를 포트로 노출)
- `Pet.kt` companion에 `selectNextRepresentative(candidates: List<Pet>): Pet?` 순수 함수 추가
- 락 패턴("users 행 잠그고 활성 pet 재조회")을 공유하는 `PetLockOperations` 헬퍼 추가
- `CreatePetService`/`SetRepresentativeService`/`DeletePetService`가 판단 로직을 직접 갖고 `PetLockOperations`·`Pet.selectNextRepresentative`·`SavePetPort`를 오케스트레이션하도록 재작성
- `PetPersistenceAdapter`에서 `registerWithinLimit`/`setRepresentativeWithinLock`/`deleteAndPromoteWithinLock` 제거 — 순수 I/O(`save`/`saveAndFlush`/조회)만 남김
- 관련 테스트 재구성(아래 "확정 사항"의 테스트 재구성 방식·동시성 테스트 전환 항목 참고)
- (구현 중 발견) 서비스 경유 동시성 테스트가 드러낸 실제 낙관적 락 충돌 결함 수정 — "구현 및 검증 결과" 참고. 계획 단계에는 없었으나, 고치지 않으면 리팩터링 전후 동작이 달라져 "동작 유지"라는 목표를 어기게 되므로 이번 범위에 포함한다.

## 작업 제외 범위

- 의도적인 새 기능 추가나 API 응답·검증 규칙 변경 — 이번 범위 아님, API 동작은 유지하는 게 목표다(리팩터링 과정에서 드러난 기존 결함의 수정은 제외 대상이 아니다 — 위 "작업 범위" 참고)
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

## 구현 및 검증 결과 (2026-09-09)

### 구조 변경

작업 범위에 기술한 대로 `SavePetPort`/`LoadPetPort`/`PetPersistenceAdapter`/`Pet.kt`/`PetLockOperations`(신규)/`CreatePetService`/`SetRepresentativeService`/`DeletePetService`와 관련 테스트 전부를 구현했다. `PetPersistenceAdapter`에는 순수 I/O(`findById`/`findAllActiveByUserId`/`findAllActiveByUserIdForUpdate`/`save`/`saveAndFlush`)만 남았고, 대표견 선정·최대 마릿수 판단은 각각 `Pet.selectNextRepresentative`(도메인)와 서비스 계층으로 이동했다.

### 리팩터링 중 발견한 실제 동시성 버그와 근본 원인

새 동시성 테스트가 (구 테스트와 달리) 서비스 계층을 통해 실제 프로덕션 요청 경로 전체를 실행하게 되면서, 리팩터링 이전부터 존재했지만 어댑터를 직접 호출하던 구 테스트로는 드러나지 않았던 실제 버그를 발견했다.

- **증상**: `SetRepresentativeService`/`DeletePetService`의 동시성 테스트에서 `ObjectOptimisticLockingFailureException` 산발적 발생
- **근본 원인**: 같은 트랜잭션 안에서 pet 엔티티가 (예: `findById`로) 먼저 영속성 컨텍스트에 관리 상태로 로드된 뒤, 이후 `FOR UPDATE` 락 재조회 쿼리가 DB 레벨 락은 정상적으로 획득하지만 Hibernate의 세션 identity map이 이미 관리 중인 엔티티의 필드값(`@Version` 포함)을 새로 조회한 결과로 갱신하지 않는다 — 그 결과 stale한 버전 값으로 저장을 시도해 낙관적 락 충돌이 발생한다.
- **1차 시도(폐기)**: `findAllActiveByUserIdForUpdate`에서 조회한 각 엔티티에 `entityManager.refresh(it)`(락 모드 없음)를 호출 → `EntityNotFoundException`이 새로 발생하며 악화됨. 원인: 락 모드 없는 `refresh()`는 MySQL REPEATABLE READ의 트랜잭션 최초 스냅샷에 묶인 비잠금 읽기라 `FOR UPDATE` 쿼리(항상 최신값)와 스냅샷 불일치가 생김.
- **2차 시도(폐기)**: `entityManager.refresh(it, LockModeType.PESSIMISTIC_WRITE)`로 변경 → `EntityNotFoundException`은 해결됐으나, 엔티티마다 개별 락 재조회를 반복하는 구조가 데드락과 유사한 현상을 유발 — 백그라운드로 5회 반복 실행한 결과 5회 전부 실패했고 소요 시간이 정상(수십 초) 대비 30분~3시간17분으로 폭증. 이 방식은 완전히 폐기.
- **최종 채택**: `findAllActiveByUserIdForUpdate` 시작 시 `entityManager.flush()` 후 `entityManager.clear()`를 한 번만 호출하고 나서 쿼리를 실행하도록 변경.
  - `flush()`: 같은 트랜잭션 안에서 아직 flush되지 않은 변경(예: `save()`로 persist만 되고 flush 전인 엔티티)을 먼저 DB에 반영 — 이게 빠지면 `clear()`가 flush 전 변경분을 그냥 버려서 데이터 유실이 생긴다(`PetPersistenceAdapterTest`의 `findAllActiveByUserIdForUpdate` 테스트로 실제 확인·수정함).
  - `clear()`: 영속성 컨텍스트를 통째로 detach시켜, 뒤이은 `FOR UPDATE` 쿼리가 완전히 새로운 엔티티 인스턴스를 만들게 함 — 엔티티별 반복 재조회 없이 identity map staleness 문제를 근본적으로 제거.

### 검증

**검증 가능성에 대한 안내**: 아래 항목 중 CI 링크가 있는 것(`./gradlew build`)은 PR 페이지에서 누구나 재확인할 수 있다. 나머지(동시성 테스트 반복 실행, 로컬 HTTP e2e, 독립 리뷰)는 이 세션이 로컬에서 직접 수행하고 그 결과를 여기 서술로 기록한 것으로, 로그·산출물이 저장소나 PR diff에 남지 않는다(로컬 e2e는 테스트 데이터를 검증 후 삭제했고, 독립 리뷰는 서브에이전트 실행 결과라 저장소에 커밋되지 않음) — 이전 pet 도메인 PR들(#17/#20/#21/#22)도 동일한 방식으로 기록해왔다. 이 기록만으로 독립 검증이 필요하면, 아래 각 항목에 적힌 재현 방법(테스트 클래스명, 실행 커맨드)으로 리뷰어가 직접 재실행해 확인할 수 있다.

- `./gradlew build`(ktlint + 전체 테스트) 통과, 실패·에러 0건 — CI(`build`) 체크로 재확인 가능: https://github.com/PetCampus-Inc/daeng_v2_back/actions/runs/34304207419
- (로컬 실행, 재현 가능하나 저장된 로그 없음) 이전에 실패했던 `PetSetRepresentativeConcurrencyTest`/`PetDeleteAndPromoteConcurrencyTest`를 포함한 4개 동시성 테스트를 `--rerun`으로 총 5회 이상 반복 실행 — 전부 통과, 소요 시간도 정상 범위(20~40초)로 회귀 없음 확인. 재현: `./gradlew test --tests "*.pet.adapter.outbound.persistence.Pet*ConcurrencyTest" --tests "*.pet.application.service.SetRepresentativeTransactionBoundaryTest" --rerun`
- **로컬 MySQL 실제 HTTP 엔드투엔드 검증(2026-09-09, 로컬 실행·재현 가능하나 저장된 로그·산출물 없음)**: 로컬 Docker MySQL/Redis(이미 기동 중인 `knockdog-mysql-local`/`knockdog-redis-local`)에 `--spring.profiles.active=local`로 실제 서버를 띄우고, 테스트 사용자 2명(`E2E497AA`/`E2E497BB`)을 DB에 직접 추가한 뒤 `.env.local`의 `JWT_SECRET_KEY`로 동일한 서명 방식(jjwt, HS256)의 액세스 토큰을 발급해 검증했다(검증 후 pets·users 테스트 데이터 전부 삭제):
  - 1번째 등록 시 자동으로 대표견 지정(`isRepresentative: true`) 확인
  - 2~5번째 등록은 대표견 아님으로 등록됨 확인, 6번째 등록 시도는 400 `PET-400-1`(최대 5마리) 확인
  - `PUT /{petId}/representative`로 대표견 변경 확인, 다른 사용자가 남의 pet에 같은 요청을 보내면 403 `PET-403-1` 확인
  - 대표견을 `DELETE`하면 남은 pet 중 이름순으로 자동 승격됨을 실제 응답으로 확인(가온·나비·라온·마루 중 가온이 재승격)
  - 삭제로 빈 자리에 새 pet 등록이 다시 성공함 확인(최대 마릿수 카운트가 삭제 반영됨)
  - 모든 pet을 순차 삭제해 마지막(대표견) 삭제 시에도 에러 없이 빈 목록으로 정상 처리됨(승격 대상 없음 케이스) 확인
  - 인증 없는 요청 401, 존재하지 않는 `petId` 조회 404 `PET-404-1` 확인
  - 리팩터링 전(KD3-431~434 e2e 기록)과 동일한 응답 코드·바디 형태로, 동작 회귀 없음을 실제 요청으로 확인
- **독립 리뷰(fresh subagent, 2026-09-09)**: 전체 diff를 처음부터 읽고 Testcontainers 동시성 테스트까지 직접 재실행해 검증. `flush()+clear()` 수정이 트랜잭션 내 다른 코드(락 이후 `save`/`saveAndFlush`, breed/user 조회)에 stale JPA 엔티티·지연 로딩 문제를 일으키지 않음을 별도로 확인(모든 포트가 순수 도메인 객체만 반환, `save()`는 항상 락 이후 새 `getReference`를 사용). 로직 버그는 발견되지 않음. 발견 사항 1건(테스트 커버리지 갭): 구 `PetPersistenceAdapterTest`에 있던 "잠금 재조회 시점에 이미 삭제된 pet이면 500이 아니라 NOT_FOUND" 테스트가 로직 이동 후 서비스 레벨에 재배치되지 않고 누락됨 — `DeletePetServiceTest`/`SetRepresentativeServiceTest`에 동일 시나리오 테스트를 추가해 반영(2026-09-09, `./gradlew build` 재통과 확인). 그 외 사소한 flush 횟수 차이 2건(구 코드는 대표견 해제 루프 후 1회 flush, 새 코드는 루프 안에서 매번 flush 등)은 낙관적 락 무결성에 영향 없어 그대로 둠.
- **DDD 적용 범위 보강(2026-09-09, 사용자 지적으로 추가 반영)**: 독립 리뷰 이후 사용자가 "`selectNextRepresentative`만 도메인으로 옮기고 같은 성격의 다른 두 규칙(최대 마릿수 체크, 최초 등록 대표견 자동 지정, 대표견 교체)은 서비스에 raw 조건문으로 남아있는 게 일관성이 없다"고 지적 — 타당한 지적으로 확인하고 같은 패턴(여러 `Pet`에 걸친 판단 = Domain Service 성격)으로 마저 추출했다.
  - `Pet.hasReachedActiveLimit(activePets): Boolean` 신설 — `CreatePetService`의 `activePets.size >= Pet.MAX_ACTIVE_COUNT` raw 비교를 대체
  - `Pet.assignRepresentativeIfFirst(activePets)` 인스턴스 메서드 신설 — `CreatePetService`의 `if (activePets.isEmpty()) markAsRepresentative() else clearRepresentative()`를 대체
  - `Pet.reassignRepresentative(target, activePets): List<Pet>?` 신설(이미 대표견이면 `null` 반환해 "아무것도 안 바꿈"을 표현, 아니면 해제된 기존 대표견 목록을 반환) — `SetRepresentativeService`의 대표견 해제·교체 절차를 대체, 서비스는 반환값을 저장만 함
  - `Pet.kt`에 세 함수 모두 순수 도메인 단위 테스트 추가(`PetTest.kt`), 서비스 테스트는 기존 assertion 그대로 통과(동작 동일함을 재확인)
  - 더 근본적인 지적(활성 pet 컬렉션에 대한 불변식은 원래 Aggregate Root가 책임져야 하고, 지금 `LockUserPort` 락은 그 경계를 수동으로 흉내 내고 있다는 점)은 "동작 변경 없음" 범위를 넘는 별도 설계 논의라 이번 티켓에서는 다루지 않고 후속 과제로만 남김
  - `./gradlew build` 재통과 확인(ktlint + 전체 테스트, 실패·에러 0건)
- **독립 리뷰(fresh subagent, 2026-09-09, DDD 초점)**: 위 보강 이후 DDD 관점으로만 다시 검토를 돌렸다. 발견 사항 3건.
  1. **반영**: `DeletePetService`에 같은 유형의 raw 조건문이 하나 더 남아있었음(`if (wasRepresentative) { Pet.selectNextRepresentative(...)... }`) — `selectNextRepresentative`(순정 선정)만 1차 리팩터링에서 도메인으로 옮겼고, "삭제된 pet이 대표견이었을 때만 승격한다"는 판단 자체는 서비스에 남아있던 것. `Pet.promoteReplacement(wasRepresentative, remainingActivePets): Pet?` 신설로 마저 추출, `DeletePetService`는 반환값을 저장만 함. `PetTest.kt`에 순수 도메인 테스트 3건 추가, `./gradlew build` 재통과 확인.
  2. **반려**: `reassignRepresentative`의 `List<Pet>?` 반환이 "도메인 사실이 아니라 저장 트리거용 구현 디테일"이라며 nullable 없이 "바뀐 pet 통합 리스트"로 바꾸자는 제안 — `representative_user_id` UNIQUE 제약 때문에 기존 대표견 해제분(`saveAndFlush`)과 신규 대표견 지정(`save`)은 저장 순서·방식이 원래 달라야 해서, 리스트로 통일해도 호출부는 결국 다시 나눠 처리해야 함(코드가 나아지지 않음). "이미 대표견이라 아무 변화 없음"도 멱등성을 표현하는 실제 도메인 사실로 판단해 현재 형태 유지.
  3. **후속 과제로 더 구체화**: `PetLockOperations`가 `users` 행을 잠그는 게 pet과 무관한 사용자 프로필 쓰기(`UserPersistenceAdapter.save`)와 불필요하게 직렬화될 수 있다는 지적 — 근거 있는 문제이나 KD3-497 이전부터 있던 구조를 그대로 옮긴 것이라 이번 범위는 아님. 리뷰어 제안대로 "Aggregate Root 재설계" 대신 **pet 컬렉션 전용 락 리소스(예: `user_pet_locks(user_id)` 행이나 MySQL named lock)로 교체**하는 더 작고 구체적인 개선안으로 후속 과제 문구를 갱신함(막연한 "Aggregate 도입 검토"보다 실행 가능한 형태로).

## 완료 확인 기준

- [x] `registerWithinLimit`/`setRepresentativeWithinLock`/`deleteAndPromoteWithinLock`이 `PetPersistenceAdapter`에서 사라지고, 같은 동작이 `CreatePetService`/`SetRepresentativeService`/`DeletePetService`에서 재현됨을 테스트로 확인한다.
- [x] `Pet.selectNextRepresentative`가 도메인 단위 순수 테스트로 검증된다.
- [x] 동시성 테스트 3개가 서비스 계층을 통해 실행되고, 기존과 동일한 안전성(스퓨리어스 없음, 유일성 보장)을 증명한다. (리팩터링 과정에서 발견한 실제 동시성 버그 포함 — 위 "구현 및 검증 결과" 참고)
- [x] `CreatePetServiceTest`/`UpdatePetServiceTest`/`SetRepresentativeServiceTest`/`GetPetServiceTest`/`DeletePetServiceTest`가 전부 `errorCode`까지 단언한다.
- [x] 전체 빌드(`./gradlew build`)가 리팩터링 전과 동일하거나 그 이상의 테스트 건수로 통과한다(실패·에러 0건).
- [x] 독립 리뷰(fresh subagent)와 로컬 HTTP e2e로 API 동작이 리팩터링 전후 동일함을 확인한다.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-497-pet-lock-logic-to-domain.md` | 갱신 | 결정 사항·구현·검증 결과 기록 |
| `SavePetPort.kt` | 갱신 | `saveAndFlush` 추가, `registerWithinLimit`/`setRepresentativeWithinLock`/`deleteAndPromoteWithinLock` 제거 |
| `LoadPetPort.kt` | 갱신 | `findAllActiveByUserIdForUpdate` 추가 |
| `PetPersistenceAdapter.kt` | 갱신 | 순수 I/O만 남김 |
| `Pet.kt` | 갱신 | `selectNextRepresentative`/`hasReachedActiveLimit`/`reassignRepresentative`/`promoteReplacement` companion 함수, `assignRepresentativeIfFirst` 인스턴스 메서드 추가 |
| `PetLockOperations.kt` | 신규 | 락+재조회 공유 헬퍼 |
| `CreatePetService.kt`/`SetRepresentativeService.kt`/`DeletePetService.kt` | 갱신 | 판단 로직 이동, 오케스트레이션 재작성, `errorCode` 관련 개선 반영 |
| `PetTest.kt` | 갱신 | `selectNextRepresentative`/`hasReachedActiveLimit`/`assignRepresentativeIfFirst`/`reassignRepresentative`/`promoteReplacement` 순수 테스트 추가 |
| `PetPersistenceAdapterTest.kt` | 갱신 | 이동된 비즈니스 로직 테스트 삭제, 순수 I/O 테스트만 남김 |
| `PetRegistrationConcurrencyTest.kt`/`PetSetRepresentativeConcurrencyTest.kt`/`PetDeleteAndPromoteConcurrencyTest.kt` | 갱신 | 서비스(유스케이스) 계층 호출로 전환 |
| `CreatePetServiceTest.kt`/`UpdatePetServiceTest.kt`/`SetRepresentativeServiceTest.kt`/`GetPetServiceTest.kt`/`DeletePetServiceTest.kt` | 갱신 | `errorCode` 단언 추가 |
