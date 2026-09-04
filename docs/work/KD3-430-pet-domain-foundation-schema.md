> 생성: 2026-09-02 19:24 · 최종 수정: 2026-09-04 15:49

# KD3-430 pet 도메인 기반 및 스키마 구축

| 항목 | 값 |
|---|---|
| Jira | `KD3-430` |
| 브랜치 | `feat/KD3-430-pet-domain-foundation-schema` |
| 상위 에픽 | `KD3-404` |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `5`
- 다음 결정 또는 전환 조건: 독립 리뷰 발견 사항 반영 완료. PR 생성.

## 작업 목표

후속 pet 유스케이스가 공유할 순수 도메인 모델, Flyway 스키마, persistence 어댑터와 소유권 확인 경계를 구축한다.

## 작업 범위

- `pets` 스키마와 `breed_id` 참조 컬럼을 추가한다.
- pet 식별자, 프로필 필드, 대표 여부, soft delete를 표현하는 도메인 모델을 만든다. 프로필 필드는 레거시(`daeng_v1_back`의 `pet/model/Pet.java`) 대조로 확정했다.
- 사용자 소유 확인, 활성 펫 조회·저장, 견종 존재 확인을 위한 포트를 정의한다.
- 최대 5마리와 대표견 단일성의 동시성 처리 방식을 구현·검증한다. 최초 등록 pet은 자동으로 대표견이 되는 레거시 규칙을 유지한다.

### `pets` 테이블 확정 설계

| 컬럼 | 타입 | NULL | 비고 |
|---|---|---|---|
| `id` | BIGINT PK | NOT NULL | AUTO_INCREMENT |
| `user_id` | BIGINT | NOT NULL | 소유자. `SocialUser.userId`와 동일 패턴 — `@ManyToOne` + `ConstraintMode.NO_CONSTRAINT` + `EntityManager.getReference()`로 다른 도메인(auth) 애그리게잇을 프록시 참조만 한다([`jpa-entity.md`](../conventions/jpa-entity.md) §3) |
| `name` | VARCHAR(100) | NOT NULL | |
| `profile_image` | VARCHAR(500) | NULL | |
| `relationship` | VARCHAR(20) | NOT NULL | enum, `@Enumerated(STRING)` |
| `relationship_text` | VARCHAR(100) | NULL | `relationship='ETC'`일 때만 필수(도메인 검증) |
| `breed_id` | BIGINT | NOT NULL | `breeds.id` 참조. `user_id`와 동일한 `@ManyToOne`+`getReference()` 패턴(다른 도메인 애그리게잇 참조) |
| `gender` | VARCHAR(20) | NOT NULL | enum |
| `birth_year` | INT | NULL | 연도만 |
| `weight` | DOUBLE | NULL | 도메인 레벨에서 1~99 범위 검증 |
| `is_neutered` | BOOLEAN | NULL | |
| `representative_user_id` | BIGINT | NULL, UNIQUE | 대표견이면 `user_id`와 같은 값, 아니면 NULL. 사용자당 대표견 1개를 DB가 무조건 보장하는 안전장치(구현 중 결정, 아래 참고) |
| `created_at`/`updated_at`/`deleted_at` | DATETIME(6) | 규칙대로 | `BaseEntity` 상속 |

실제 컬럼·제약의 최종 근거는 `V4__create_pets.sql`이며, 위 표와 어긋나면 SQL 파일이 우선한다.

## 작업 제외 범위

- HTTP API와 유스케이스 구현
- 기존 pet 데이터 backfill
- `school_pet_membership` 조회·갱신 및 `schoolConnectionBadge`
- **user-pet 다대다(공동 소유·가족 공유) 모델**: 하나의 pet을 여러 사용자가 등록해 동일 정보를 공유하는 설계를 검토했으나, 문서·레거시 어디에도 확정된 요구사항이 없고(레거시 `Pet`도 `user_id` 단일 FK), 초대·권한·연결해제 플로우 같은 UX·정책이 전혀 정의돼 있지 않아 지금 스키마에 반영할 근거가 없다. 이 티켓에 후속 4개 티켓(KD3-420~423)이 대기 중이라 미확정 기능으로 범위를 넓히는 비용도 크다. 기획이 확정되면 별도 티켓으로 설계·조사(007 workflow)부터 시작한다. 관련 제약은 `docs/domains/pet.md`에 남긴다.

## 방향 논의 및 결정 사항

### 확정 사항

- pet은 견종명을 저장하지 않고 `breed_id`를 참조한다. 컬럼명·참조 대상은 KD3-418이 실제로 만든 `breeds` 테이블·`domain/breed` 패키지 기준이다(`breed_catalog_id`는 레거시 서버의 옛 명칭이 잘못 남은 것이었다 — `docs/domains/pet.md`와 대조해 KD3-430 승인 직후 정정).
- pet 삭제는 후속 삭제 유스케이스에서 soft delete로 처리한다.
- DB FK 제약은 두지 않고 애플리케이션 경계에서 참조 정합성을 확인한다.
- 작업 브랜치는 `dev`가 아닌 `feat/KD3-418-breed-catalog-v1-api`에서 분기한 stacked 브랜치로 진행한다. `breed_id` 참조가 KD3-418 산출물에 기능적으로 의존하기 때문이다. PR base는 KD3-418이 `dev`에 머지되기 전까지 `feat/KD3-418-breed-catalog-v1-api`로 두고, 머지 후 `dev`로 재조정한다. KD3-418에 추가 커밋이 발생하면 그 위로 rebase한다.
- `birthYear`는 레거시와 동일하게 연도만 저장한다. 정확한 생년월일(date)로의 확장은 이번 작업 범위가 아니다.
- `relationship`/`relationshipText`는 레거시와 동일하게 pet 도메인에 포함한다. `relationship`은 값이 8개로 고정되고 값 자체에 딸린 메타데이터가 없어 `breed`(FCI 표준 참조 데이터, 385건, 자체 메타데이터 보유)와 달리 참조 테이블이 아니라 Kotlin enum으로 관리한다 — 이 프로젝트의 `AddressType`/`Gender` 등과 동일한 패턴. `ETC`일 때만 `relationshipText` 필수 검증은 도메인 모델이 담당한다.
- 최초 등록하는 pet은 자동으로 대표견이 되는 레거시 규칙(`PetService.registerPet`의 `isFirstPet`)을 그대로 유지한다.
- `breedId`는 NOT NULL로 강제한다. `breeds` 카탈로그에 믹스견(1번)·기타(385번)가 있어 견종을 특정할 수 없는 경우도 표현 가능하다.
- `profileImage`는 nullable로 설계한다. 레거시 엔티티 컬럼은 NOT NULL이지만 등록 요청 DTO의 `@NotBlank`가 주석 처리돼 있어 실제 운영에서 필수로 강제되지 않았다(레거시 자체 불일치) — 그 실질 동작을 따른다.
- `gender`는 레거시(등록 요청 DTO `@NotNull`)와 동일하게 NOT NULL로 강제한다. 레거시 엔티티 컬럼 자체는 nullable이지만, 실제 등록 경로는 항상 값을 요구했다.
- `weight`는 도메인 레벨에서 1~99 범위를 검증한다. 레거시는 API 요청 DTO에서만 검증했는데, 이번엔 도메인 모델(`Pet.create`/`update`)이 검증을 담당해 진입점과 무관하게 불변식을 지킨다.
- `name`(VARCHAR 100)·`profile_image`(VARCHAR 500)·`relationship_text`(VARCHAR 100)는 이 프로젝트의 기존 컬럼(`User.nickname` length 100, `profile_image` length 500)보다 좁지 않게 여유를 두고 정했다. 프론트 화면의 실제 입력 제한은 별도이며 이 값보다 항상 좁게 잡는다.
- `user_id`·`breed_id`는 `SocialUser.userId`와 동일한 패턴을 쓴다: `@ManyToOne` + `ConstraintMode.NO_CONSTRAINT` + `EntityManager.getReference()`로 다른 도메인(auth·breed) 애그리게잇을 전체 로딩 없이 프록시로만 참조한다([`jpa-entity.md`](../conventions/jpa-entity.md) §3). (구현 착수 시점에 "plain Long 컬럼"으로 잘못 안내했다가 `SocialUser` 코드를 다시 대조해 정정했다.)
- 견종 이름(`nameKo` 등)은 pet 도메인에 중복 저장하지 않는다. 표시용 이름이 필요한 조회 API(KD3-421)가 `breedId`로 breed 도메인의 조회 포트를 호출해 응답 시점에 조합한다.
- 견종 존재 확인은 breed 도메인의 `LoadBreedsPort`에 `existsById(id: Long): Boolean`을 추가(KD3-418 산출물 확장, `BreedPersistenceAdapter`가 `breedJpaRepository.existsById`로 구현)하고, pet 도메인은 자신의 `ExistsBreedPort`를 정의해 그 위에 위임하는 어댑터(`BreedExistenceAdapter`)로 연결한다. pet의 application 계층은 `LoadBreedsPort`를 직접 알지 못한다.
- **대표견 단일성은 `is_representative` 불리언 대신 `representative_user_id`(nullable, UNIQUE) 컬럼으로 구현한다** (구현 중 결정). 대표견이면 `user_id`와 같은 값을, 아니면 NULL을 저장한다 — 매핑은 어댑터(`PetMapper`)가 전담하고 도메인 모델은 여전히 `isRepresentative: Boolean`만 노출한다. MySQL 전용 문법(생성 컬럼 등) 없이 표준 UNIQUE 제약만으로 동작해 로컬 테스트(H2, `ddl-auto: create-drop`)와 운영(MySQL) 양쪽에서 동일하게 검증할 수 있다.
- **최대 5마리는 애플리케이션 레벨 잠금으로 처리한다**: `PetJpaRepository.findAllActiveByUserIdForUpdate`가 `@Lock(PESSIMISTIC_WRITE)`로 해당 사용자의 활성 pet 행을 잠그고, `PetPersistenceAdapter.registerWithinLimit`가 같은 트랜잭션에서 개수를 확인한 뒤 저장한다. 이 락은 기존 행이 있을 때만 신뢰할 수 있다 — MySQL InnoDB의 갭 락(0건일 때의 신규 삽입 직렬화)까지는 검증하지 못했다(아래 완료 확인 기준 참고).
- `Relationship`의 손윗형제 4종은 레거시 `ELDER_SISTER`/`ELDER_BROTHER`/`OLDER_SISTER`/`OLDER_BROTHER`를 쓰지 않고 `EONNI`/`NUNA`/`OPPA`/`HYUNG`(로마자 표기)로 바꿨다(구현 중 결정). 언니/누나/오빠/형은 "손윗형제의 성별 × 화자(보호자)의 성별" 조합이라 영어에 대응 단어가 없고, 레거시의 elder/older 구분은 실제로는 없는 의미 차이를 암시해 혼동을 준다. `@Enumerated(STRING)`이라 이 이름이 그대로 DB에 저장되고 향후 API 응답에도 노출될 값이라, 데이터·API가 없는 지금 정정하는 비용이 가장 낮다. `MOTHER`/`FATHER`/`GUARDIAN`/`ETC`는 정확한 영어 대응이 있어 그대로 유지한다.

### 미결 질문

- 없음. 대표견과 최대 마릿수의 경쟁 상태 방지 세부 구현은 이 작업에서 결정·기록한다.

### 사용자 승인 기록

- 2026-09-02: 사용자가 pet 도메인을 유스케이스 단위로 분리하고 견종 ID 참조를 승인했다.
- 2026-09-04: 레거시(`daeng_v1_back`) `Pet` 엔티티·`PetService` 대조로 프로필 필드 목록이 작업 범위에 없던 것을 발견했다. 사용자가 생년 필드(연도만 유지), `relationship`/`relationshipText` 포함(enum), 최초 등록 자동 대표견 유지, `breedId` NOT NULL, `profileImage` nullable을 확정했다. 이어서 `gender` NOT NULL, `weight` 1~99 범위 검증, 컬럼 길이(100/500/100), `user_id`/`breed_id`의 plain 컬럼 참조 방식을 확정했다.

## 완료 확인 기준

- Flyway 스키마가 빈 DB에 정상 적용된다.
- 도메인 불변식과 persistence 어댑터의 단위 테스트를 통과한다.
- 동시 등록·대표 변경 시 최대 마릿수와 대표견 단일성이 깨지지 않는 검증을 수행한다.
- 사용자의 첫 pet 등록 시 자동으로 대표견이 되고, 이후 등록에는 자동 지정되지 않는 동작을 검증한다.
- ArchUnit·ktlint를 포함한 관련 정적 검사를 통과한다.

## 검증 결과

- **`./gradlew build`(2026-09-04, 독립 리뷰 반영 후 재실행)**: ktlint, 컴파일, 전체 테스트가 통과했다. `PetTest`(도메인 불변식) 12건, `PetPersistenceAdapterTest`(등록·대표견·최대 마릿수·유니크 제약·삭제 후 재등록) 6건, `HexagonalArchitectureTest` 4건(신규 `domain.pet.domain` 패키지 포함), `BreedQueryServiceTest`(신규 `existsById` 포함) 3건 전부 통과.
- **Flyway 로컬 MySQL 재적용 (2026-09-04, 이 세션에서 재현)**: `docker compose --env-file .env.local -f docker-compose.local.yaml up -d` 후 `./gradlew bootRun --args='--spring.profiles.active=local'`로 기동. 로그에 `Migrating schema knockdog to version "4 - create pets"` → `Successfully applied 1 migration ... now at version v4`가 남았다.
- **최대 마릿수 동시성 — 실제 MySQL 교차 검증 (2026-09-04)**: H2(`ddl-auto: create-drop`) 기반 멀티스레드 테스트를 처음 작성했으나 `PESSIMISTIC_WRITE` 락이 H2에서 MySQL InnoDB처럼 블로킹하지 않아 `expected: <1> but was: <3>`로 실패했다(H2가 실제 잠금 동작을 재현하지 못하는 KD3-418의 LIKE 이스케이프 사례와 같은 한계). 이 H2 테스트는 신뢰할 수 없어 제거하고, 로컬 MySQL에 4건을 미리 저장한 뒤 동일한 `SELECT ... FOR UPDATE` 패턴을 쓰는 저장 프로시저를 만들어 3개 세션에서 동시 호출했다 — 정확히 1건만 성공(`race_inserted=1`)하고 나머지 2건은 `LIMIT_EXCEEDED`로 거부됐으며 최종 5건에서 멈췄다. 기존 행이 있는 경우(실사용 시나리오 대부분)의 직렬화는 실제 MySQL에서 확인했다.
- **확인하지 못한 항목**: 활성 pet이 0건인 상태에서 동시에 여러 등록이 몰리는 "첫 pet 경쟁" 케이스는 MySQL InnoDB 갭 락에 의존하는데, 이번 검증에서는 재현하지 않았다. 다만 대표견 단일성은 `representative_user_id`의 DB UNIQUE 제약이 락 성공 여부와 무관하게 항상 보장하므로(두 번째 대표견 저장 시 `DataIntegrityViolationException`이 나는 것을 H2·MySQL 스키마 양쪽에서 확인), 이 잔여 케이스에서도 "대표견 2개"라는 결과는 나올 수 없다 — 다만 이론상 5마리 제한을 순간적으로 넘겨 등록될 가능성은 남아 있으며, 재현하려면 완전히 새 사용자에 대한 동시 등록을 로컬 MySQL에서 별도로 검증해야 한다.
- **ArchUnit·ktlint**: `domain.pet.domain` 패키지를 `HexagonalArchitectureTest`의 4번 규칙 대상에 등록했고, `ktlintCheck`가 통과했다.

## 독립 리뷰 (2026-09-04)

컨텍스트를 공유하지 않는 리뷰어에게 이 문서와 `feat/KD3-418-breed-catalog-v1-api..feat/KD3-430-pet-domain-foundation-schema` 커밋 범위를 전달해 대조했다.

- **(중간, 수정 완료) 대표견 soft delete 시 `representative_user_id` 미정리**: `Pet.delete()`가 `isRepresentative`를 해제하지 않아, 대표견을 soft delete한 뒤 활성 pet 0건 상태에서 새로 등록하면 `representative_user_id` UNIQUE 제약에 걸려 실패하는 결함을 발견했다. `Pet.delete()`가 `isRepresentative = false`도 함께 설정하도록 수정했고, `PetTest`(대표견 삭제 시 상태 해제)·`PetPersistenceAdapterTest`(대표견 삭제 후 재등록 성공)에 회귀 테스트를 추가했다. 삭제 유스케이스 자체는 KD3-423 범위이지만, 이 스키마·도메인 기반 위에서 재현되는 결함이라 이번 티켓에서 수정했다.
- **(경미, 수정 완료) `Pet.reconstitute`의 code-style.md 위반 주석**: `code-style.md`(2026-09-02, 주석 금지)를 위반하는 KDoc이 있었고 같은 패턴의 `User.reconstitute`/`SocialUser.reconstitute`에는 없던 것이라 제거했다.
- 그 외 항목(컬럼 설계, `SocialUser` 참조 패턴, 견종 존재 확인 위임 구조, ArchUnit 등록, 작업 제외 범위 준수, 검증 결과의 정직성)은 작업 문서와 실제 diff가 일치함을 확인받았다.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-430-pet-domain-foundation-schema.md` | 갱신 | 기반 설계·구현 결정·검증 결과 기록 |
| `docs/domains/pet.md` | 갱신 | "pet 프로필과 불변식" 절 신규 추가, "pet 소유 관계" 절의 대표견 컬럼명 정정 |
| `docs/inventory/database.md` | 갱신 | 레거시 `pet` 행 진척을 `진행중`으로, 신규 `pets` 행을 `REDESIGN`·`진행중`으로 추가 |
