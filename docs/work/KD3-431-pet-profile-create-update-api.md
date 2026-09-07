> 생성: 2026-09-02 19:24 · 최종 수정: 2026-09-07

# KD3-431 pet 프로필 생성·수정 API 구축

| 항목 | 값 |
|---|---|
| Jira | `KD3-431` |
| 브랜치 | `feat/KD3-431-pet-profile-create-update-api` |
| 상위 에픽 | `KD3-404` |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `5`
- 다음 결정 또는 전환 조건: `KD3-430`은 이미 `epic/KD3-404-pet-domain-migration`에 머지됐고 PR #17도 그 위로 base가 맞춰진 상태로 이미 생성돼 있다(rebase 완료). 독립 리뷰(2026-09-07, 헥사고날 구조·도메인 모델·동시성·에러 처리 전수 확인)에서 나온 수정 사항을 코드·문서에 반영 완료, 빌드·테스트 통과 확인. 아직 커밋·푸시는 안 했다 — 사용자 승인 후 커밋 → 푸시 → PR #17 본문에 이번 리뷰 반영분 동기화 → 머지 준비 순서로 진행.

## 작업 목표

인증된 사용자가 자신의 pet 프로필을 생성하고 부분 수정할 수 있는 v1 REST API를 제공한다.

## 작업 범위

- `POST /api/v1/pets` 생성 API를 구현한다.
- `PATCH /api/v1/pets/{petId}` 부분 수정 API를 구현한다.
- 소유권, 최대 5마리, 견종 존재, `relationship=ETC`일 때 관계 직접 입력값 검증을 적용한다.
- 요청에서 누락한 수정 필드는 유지하는 PATCH 의미와 `null` 처리 규칙을 명세·테스트한다.

### 요청·응답 필드 계약

| 필드 | POST(생성) | PATCH(수정) | 응답 |
|---|---|---|---|
| `name` | 필수 | 선택(값 변경만, null 불가) | 포함 |
| `profileImage` | 선택 | 선택(`JsonNullable`, null로 지우기 가능) | 포함 |
| `relationship` | 필수 | 선택(값 변경만, null 불가) | 포함 |
| `relationshipText` | `relationship=ETC`일 때만 필수 | `relationship=ETC`일 때만 필수(값 변경만). `relationship`을 ETC 아닌 값으로 바꾸면 자동으로 `null`(위 확정 사항) | 포함 |
| `breedId` | 필수(`LoadBreedPort.findById`로 존재 검증 겸 조회) | 선택(값 변경만, null 불가, 변경 시 동일하게 존재 검증) | 포함 |
| `breedNameKo`/`breedAlias` | 해당 없음(요청에 없음) | 해당 없음 | 포함 — `breedId`로 breed 도메인을 조회해 조합. 화면 표시("한글명 (별칭)")는 breed 목록 API와 동일하게 프론트가 조합 |
| `gender` | 필수 | 선택(값 변경만, null 불가) | 포함 |
| `birthYear` | 선택 | 선택(`JsonNullable`, null로 지우기 가능) | 포함 |
| `weight` | 필수 | 선택(값 변경만, **null 불가** — `Pet` 도메인·DB 컬럼 전체에서 항상 non-null, 아래 확정 사항 참고) | 포함 |
| `isNeutered` | 선택 | 선택(`JsonNullable`, null로 지우기 가능) | 포함 |
| `id`/`isRepresentative` | 해당 없음 | 해당 없음 | 포함 |

`createdAt`/`updatedAt`은 응답에 포함하지 않는다(레거시 `PetResponse`와 다름 — 아래 "구현 중 발견해 정정한 사항" 참고).

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
- **`relationship`이 `ETC`가 아닌 값으로 바뀌면 서버가 `relationshipText`를 자동으로 `null`로 지운다.** 레거시 `PetService.update`는 "값이 `null`로 들어오면 기존 값을 유지"하는 방식이라 필드를 명시적으로 지우는 경로가 아예 없었고, 그 결과 `ETC`에서 다른 관계로 바뀌어도 예전 `relationshipText`가 영구히 남는 결함이 있었다 — 이번엔 그 결함을 재현하지 않고 도메인이 "ETC가 아니면 `relationshipText`는 항상 `null`"이라는 양방향 불변식을 직접 보장한다. KD3-430의 `Pet.kt` 검증(`validateRelationshipText`)을 이 방향으로 보강 완료(KD3-430 `9c8deab`).
- **PATCH는 nullable 필드(`profileImage`, `birthYear`, `isNeutered`)를 명시적으로 지우는 것을 지원한다.** "필드 생략(유지)"과 "명시적 null(지우기)"을 구분해야 해서 `JsonNullable<T>`(`org.openapitools:jackson-databind-nullable`) 래퍼를 PATCH 요청 DTO에 사용한다. 생성(POST) 요청 DTO는 이 래퍼가 필요 없다(전체 객체를 새로 받으므로).
- **`weight`는 생성 시 필수이며, 수정 후에도 항상 non-null이어야 한다** (레거시 등록 API `RegisterPetRequest`와 동일하게 생성 시 필수이되, 이 프로젝트는 그 필수성을 생애주기 전체로 확장했다). 처음엔 "생성 시점만 필수, DB 컬럼·PATCH는 nullable"로 설계했으나, `weight`도 `profileImage` 등과 같은 nullable-후보 4개 중 하나로 취급한 것이 잘못이라는 지적을 받아 정정했다 — `weight`는 PATCH로도 `null`을 보낼 수 없는 유일한 필드다. `weight`는 `JsonNullable<Double>`(비-nullable 내부 타입)로 받아 "생략(유지)"과 "값 변경"만 구분하고, 클라이언트가 명시적으로 `null`을 보내면 `UpdatePetService`가 `IllegalArgumentException`으로 거부한다 — `JsonNullable<Double>`이라는 타입 선언만으로는 Jackson 역직렬화가 `null`을 막지 못하므로(Java 제네릭 소거로 Kotlin의 non-null 검사가 적용되지 않는 알려진 함정), 서비스 레이어에 `requireNotNull` 방어 코드를 별도로 둔다. KD3-430의 `Pet.create`/`update`/`reconstitute`와 DB 컬럼(`weight DOUBLE NOT NULL`) 전부 non-null `Double`을 요구하도록 보강 완료.
- **응답에 견종 표시 정보(`breedNameKo`/`breedAlias`)를 포함한다.** pet은 견종을 텍스트가 아니라 `breedId` 참조로 저장하는 걸 유지한다 — 레거시처럼 텍스트로 저장하면 오타·표기 불일치, 카탈로그 쪽 정정이 기존 pet 레코드에 반영 안 되는 문제(KD3-418에서 견종 카탈로그를 만든 이유 자체)가 재발한다. 응답 조합 비용(breed 조회 1회)이 훨씬 싸다. 표시 문자열("한글명 (별칭)")은 백엔드가 미리 합치지 않고 `nameKo`/`alias`를 따로 내려준다 — breed 자체 API(`GET /api/v1/breeds`)도 조합된 문자열이 아니라 `nameKo`/`alias`를 따로 내리고 프론트가 조합하는 관례라(`docs/domains/pet.md` 견종 기준 데이터 절), 여기서도 같은 조합 로직을 재사용할 수 있게 맞춘다.
- **breed 도메인에 "id로 단건 조회" 포트가 새로 필요하다.** 지금은 `LoadBreedsPort.existsById`(존재 확인)만 있다. `LoadBreedsPort`에 `findById(id: Long): Breed?`를 추가하고, pet은 `ExistsBreedPort`와는 별도로 `LoadBreedPort`(`id`/`nameKo`/`alias`만 담는 자체 타입 반환)를 새로 정의해 `BreedExistenceAdapter`와 같은 방식으로 위임한다. KD3-430 문서의 "표시용 이름은 조회 API(KD3-432)가 조합한다"는 기존 계획을 이 티켓(KD3-431)이 먼저 이 포트를 만들고 KD3-432가 재사용하는 것으로 변경한다.
- **`PetErrorCode`는 레거시(`daeng_v1_back`의 `common/response/ErrorCode.java`) 값을 그대로 재사용한다**: `NOT_FOUND`(404, `PET-404-1`), `NOT_AUTHORIZED`(403, `PET-403-1`), `LIMIT_EXCEEDED`(400, `PET-400-1`), `RELATIONSHIP_TEXT_REQUIRED`(400, `PET-400-2`). Kotlin enum 상수 이름은 `AuthErrorCode`와 동일하게 도메인 접두어 없이 짧게 쓴다(`code` 문자열만 프론트 계약이라 상수 이름은 자유). 프론트(`daeng_v2_front`)가 이 `code` 문자열로 실제 분기하는 곳은 없음을 확인했지만(`pet` 관련 에러는 전부 일시적 오류 토스트로만 처리), `error-handling.md`의 기본 규칙("프론트가 이미 참조 중인 문자열을 그대로 가져다 쓴다")을 그대로 따른다. 레거시엔 없던 신규 검증(견종 존재 확인)은 같은 포맷으로 `NOT_FOUND_BREED`(400, `PET-400-3`)를 새로 추가한다 — `breedId` 참조 방식으로 바뀌면서 생긴 신규 검증이라 레거시 대응 코드가 없다. `RELATIONSHIP_TEXT_REQUIRED`는 정의만 해두고 이번 구현에서는 실제로 던지지 않는다(아래 구현 중 발견 사항 참고). `weight` 명시적 null 거부는 전용 에러코드 없이 공통 `INVALID_INPUT_VALUE`(400, `IllegalArgumentException`)로 처리한다 — `relationshipText` 동시 지정 거부(E)와 같은 방식이다.
- **`docs/conventions/error-handling.md`에 `PetErrorCode`를 구조화 포맷(`<도메인>-<status>-<순번>`)의 실제 구현 사례로 추가했다.** 지금까지 `AuthErrorCode`(시맨틱 문자열) 하나만 실제 예시였고 구조화 포맷은 레거시 예시(`OWNER_VERIFICATION-401-1`)로만 언급돼 있었는데, `PetErrorCode.kt` 구현 완료 후 실제 코드로 반영했다.
- **PATCH에서 `relationship`을 ETC가 아닌 값으로 바꾸면서 동시에 `relationshipText`를 명시적으로 함께 보내면 400으로 거부한다** (자동으로 무시하고 지우지 않는다). 업계에서 클라이언트가 명시적으로 보낸 모순된 값은 조용히 버리기보다 거부해 클라이언트 버그를 초기에 드러내는 쪽이 정석이다. 이미 만든 `Pet`의 양방향 불변식(`validateRelationshipText`)이 이 케이스를 그대로 걸러내므로 별도 예외 처리나 신규 에러 코드가 필요 없다 — PATCH 핸들러가 클라이언트가 준 값을 그대로 도메인에 전달하기만 하면 된다. `relationshipText`를 생략한 경우(A의 자동 제거)와는 다른 케이스다.

### 구현 중 발견해 정정한 사항

- **응답에 `createdAt`/`updatedAt`을 포함하지 않는다** (레거시 `PetResponse`와의 차이). KD3-430의 `Pet` 도메인 모델은 `User`/`SocialUser`처럼 이 프로젝트 관례대로 audit 타임스탬프를 도메인에 담지 않는다(`deletedAt`처럼 행동에 의미 있는 것만 도메인이 가짐) — 응답 필드 계약을 정할 때 레거시를 그대로 옮기며 놓쳤던 부분이라 구현 중 바로잡았다.
- **`ExistsBreedPort`(KD3-430)는 이번 구현에서 실제로 쓰이지 않아 삭제했다.** 응답에 `breedNameKo`/`breedAlias`가 필요해 어차피 `LoadBreedPort.findById`를 호출해야 하는데, 이 한 번의 호출이 존재 여부(null이면 미존재)와 표시 정보 조회를 동시에 해결한다 — `existsById`를 별도로 호출하면 같은 정보를 얻으려고 조회를 두 번 하는 셈이라 애초에 호출하지 않았다. 처음엔 "향후 필요해지면 쓰면 된다"며 미사용 상태로 남겨뒀으나(YAGNI 위반), 독립 리뷰(2026-09-07)에서 실제 호출부가 전혀 없음을 확인해 `ExistsBreedPort.kt`·`BreedExistenceAdapter.kt`를 삭제했다. breed 도메인(KD3-418)의 `LoadBreedsPort.existsById`/`BreedPersistenceAdapter.existsById`도 이 어댑터가 유일한 호출부였고 전용 테스트도 없어(호출부·테스트 부재를 grep으로 재확인) 함께 삭제했다(테스트 더블인 `BreedQueryServiceTest`의 fake 구현도 같이 제거). breed 도메인 소유 코드지만 삭제 근거가 이번 리뷰에서 나온 것이라 KD3-431 브랜치에서 함께 정리했다.
- **`PetResponse.weight`가 `Double?`(nullable)로 선언돼 있던 것을 `Double`로 고쳤다.** `Pet.weight`는 도메인이 항상 non-null을 보장하고 이 문서·`docs/domains/pet.md`도 그렇게 명시하는데, 응답 DTO 타입만 nullable이라 자체 계약과 어긋났다(독립 리뷰에서 발견, 실제 런타임 오류는 없었음).
- **`docs/architecture/hexagonal.md`의 ArchUnit 규칙4 설명이 코드와 어긋나 있던 것을 정정했다.** 문서는 "규칙4는 auth만 등록, 새 도메인은 수동 등록 필요"라고 적혀 있었으나 실제 `HexagonalArchitectureTest.kt`는 `domain.*.domain..` 와일드카드라 이미 전 도메인에 자동 적용된다(pet도 포함, 위반 없음 확인). 독립 리뷰에서 발견해 문서를 코드에 맞춰 정정했다.
- **`GlobalExceptionHandler`에 `IllegalStateException` → 409(`CommonErrorCode.CONFLICT`) 전역 핸들러를 추가했다(pet 범위를 넘는 공통 변경).** `Pet.delete()`/`Pet.markAsRepresentative()`(KD3-433/434에서 쓰일 예정)의 `check()` 검증 실패가 지금까지 전용 핸들러 없이 catch-all(500)로 떨어지던 구멍을 독립 리뷰에서 발견했다. 추가 전 `check()` 호출부 3곳(`Pet.delete`/`Pet.markAsRepresentative`/`User.withdraw`)을 grep으로 전수 확인한 결과 프로덕션에서 실제로 도달하는 경로가 없어(전부 미구현 API) 기존 동작에 대한 영향은 없음을 검증했다. 상세 규칙·근거는 `docs/conventions/error-handling.md` §3에 기록.
- **`RELATIONSHIP_TEXT_REQUIRED`(PET-400-2)를 실제로는 던지지 않는다.** `relationship=ETC`인데 `relationshipText`가 없는 경우는 `Pet.create`/`Pet.update`의 `validateRelationshipText`가 이미 `IllegalArgumentException`으로 막고, `GlobalExceptionHandler`가 이를 400 `INVALID_INPUT_VALUE`로 처리한다. 레거시는 이 케이스에 전용 코드를 던졌지만, 서비스 레이어에서 도메인 검증보다 먼저 이 조건만 따로 체크해 전용 에러코드로 바꾸는 건 도메인 로직을 서비스에 중복시키는 것이라 하지 않았다 — enum 값 자체는 향후 필요해지면 쓸 수 있게 남겨둔다.
- **`weight`를 처음엔 PATCH로 지울 수 있는 nullable 필드 4개(`profileImage`/`birthYear`/`weight`/`isNeutered`) 중 하나로 설계했다가 정정했다.** `weight`는 생성 시에만 필수이고 이후엔 지울 수 있다고 잘못 판단한 것으로, 사용자가 "수정할 때도 non-null이어야 한다"고 지적해 바로잡았다. KD3-430(도메인·스키마)과 KD3-431(API) 양쪽 모두 수정해, `Pet` 도메인 모델·DB 컬럼·`UpdatePetCommand`/`UpdatePetRequest`의 `weight` 타입(`JsonNullable<Double>`, 비-nullable 내부 타입)과 `UpdatePetService`의 명시적 null 거부 로직까지 전부 반영했다.

### 미결 질문

- 없음.

### 사용자 승인 기록

- 2026-09-02: 사용자가 신규 pet API를 RESTful v1로 설계하는 방향을 승인했다.
- 2026-09-04: 사용자가 A~D를 하나씩 확정했다 — (A) `relationship`이 ETC에서 다른 값으로 바뀌면 `relationshipText` 자동 제거(레거시는 이 경로 자체가 없던 결함이었음을 확인 후 승인), (B) PATCH의 nullable 필드 명시적 null 지원(`JsonNullable`) 및 `weight` 생성 시 필수화, (C) 응답에 `breedNameKo`/`breedAlias` 포함(레거시 텍스트 저장 대신 `breedId` 참조 유지가 맞다는 근거 확인 후 승인) 및 요청·응답 필드 계약 표, (D) `PetErrorCode`를 레거시 값 그대로 재사용 + 신규 `NOT_FOUND_BREED` 추가, `error-handling.md`에 구조화 포맷 실제 사례로 반영.
- 2026-09-04: 구현 완료 후 `docs/inventory/api.md`를 뒤늦게 대조하다 `POST /api/v0/pet/register`가 원래 `KEEP`(v0 계약 유지)으로 판정돼 있던 것을 발견했다 — 이 티켓의 전제(생성도 v1 RESTful로 재설계)와 인벤토리 판정이 어긋난 채로 구현을 시작한 것이었다. 사용자가 v1 RESTful 재설계가 맞다고 확정해, 인벤토리 판정을 `KEEP` → `REDESIGN`(v0+v1)으로 정정했다.
- 2026-09-04: 사용자가 `weight`는 생성 시점뿐 아니라 수정 후에도 항상 non-null이어야 한다고 정정했다(B에서 "생성 시 필수, PATCH로 지울 수 있음"으로 잘못 확정했던 것을 철회) — `weight`를 nullable-후보 4개 필드 목록에서 제외하고, `UpdatePetCommand`/`UpdatePetRequest`의 `weight` 타입을 `JsonNullable<Double?>`에서 `JsonNullable<Double>`로 바꾸고 `UpdatePetService`에 명시적 null 거부 로직을 추가했다. KD3-430의 도메인·스키마도 같은 방향으로 함께 수정했다(KD3-430 문서 참고).

## 완료 확인 기준

- 생성·수정 정상 경로와 소유권 위반, 마릿수 초과, 미존재 견종, 관계 입력 오류를 테스트한다.
- PATCH의 필드 누락·명시적 null 처리와 응답 계약을 테스트한다.
- PATCH에서 `relationship`을 ETC 아닌 값으로 바꾸며 `relationshipText`를 동시에 보내면 400으로 거부되는지 검증한다.
- PATCH에서 `weight`에 명시적 `null`을 보내면 400으로 거부되는지 검증한다.
- API 계약 문서와 인벤토리 영향을 판정·기록한다.

## 검증 결과

- **`./gradlew build`(2026-09-04, `weight` non-null 정정 반영 후 재실행)**: ktlint, 컴파일, 전체 테스트, ArchUnit 통과. `CreatePetServiceTest` 4건, `UpdatePetServiceTest` 8건(생략 유지, nullable 필드 명시적 null 지우기, `weight` 명시적 null 거부, 미존재/삭제된 pet, 소유권 위반, 미존재 breed, A의 자동 제거, E의 거부 케이스 포함) 통과, 기존 pet·breed 테스트 전부 회귀 없이 통과.
- **로컬 MySQL 실제 HTTP 엔드투엔드 검증 (2026-09-04, `weight` non-null 정정 이전 설계 기준)**:
  - 인증 없이 `POST`/`PATCH` 호출 → 401 `UNAUTHORIZED_REQUEST` 확인
  - 정상 생성(`relationship=ETC`) → 201, `breedNameKo`가 실제 breed 조회로 조합됨, 최초 등록이라 `isRepresentative=true` 확인(KD3-430 로직이 API 계층까지 정상 연결됨)
  - PATCH로 다른 필드만 보내면 나머지 필드가 그대로 유지됨(`JsonNullable` "생략" 처리) 확인
  - PATCH로 `relationship`만 `MOTHER`로 바꾸면 `relationshipText`가 자동으로 `null`이 됨(A) 확인
  - PATCH로 `relationship=FATHER`와 `relationshipText`를 동시에 보내면 400 `INVALID_INPUT_VALUE`로 거부됨(E) 확인
  - 존재하지 않는 `breedId`로 생성 시도 → 400 `PET-400-3` 확인
  - 존재하지 않는 `petId`로 PATCH → 404 `PET-404-1` 확인
  - 5마리까지 정상 등록 후 6번째 등록 시도 → 400 `PET-400-1` 확인
  - 다른 사용자의 pet을 PATCH 시도 → 403 `PET-403-1` 확인
- **로컬 MySQL 실제 HTTP 엔드투엔드 재검증 (2026-09-04, `weight` non-null 정정 반영 후)**: `weight` 관련 변경분만 다시 검증했다(다른 경로는 이번 정정과 무관해 재검증하지 않음). 로컬 MySQL에 이미 반영돼 있던 구 스키마(`weight` nullable)의 `pets` 테이블·Flyway 이력을 지우고 `V4__create_pets.sql`(현재 버전, `weight NOT NULL`)을 처음부터 재적용한 뒤, 테스트 사용자 1명을 추가하고 동일한 방식으로 액세스 토큰을 서명해 검증했다(테스트 데이터는 검증 후 삭제):
  - 정상 생성(`weight: 10.0`) → 200, `weight` 정상 저장 확인
  - PATCH로 `weight: null`을 명시 → 400 `INVALID_INPUT_VALUE`("weight는 null일 수 없습니다.")로 거부됨 확인 — nullable 필드였을 때와 달리 더 이상 지워지지 않는다
  - PATCH로 `weight: 15`(값 변경만) → 200, `weight`가 정상적으로 갱신됨 확인
- **독립 리뷰 후 재검증(2026-09-07)**: §방향 논의 및 결정 사항의 정정 사항(`PetResponse.weight` non-null화, `ExistsBreedPort`/`BreedExistenceAdapter`·`LoadBreedsPort.existsById`/`BreedPersistenceAdapter.existsById` 삭제, `GlobalExceptionHandler`의 `IllegalStateException` 전역 핸들러 추가) 반영 후 `./gradlew build`(ktlint, 컴파일, 전체 테스트, ArchUnit 포함) 재실행해 통과 확인. `UpdatePetServiceTest`에 `relationship이 이미 ETC가 아닌 상태에서 relationshipText만 명시적으로 보내면 거부된다` 케이스를 추가해(기존엔 관계를 함께 바꾸는 경우만 테스트) `validateRelationshipText`가 관계 변경 여부와 무관하게 동일하게 동작함을 명시적으로 커버.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-431-pet-profile-create-update-api.md` | 갱신 | API 결정·구현·검증 결과 기록, `weight` non-null 정정 반영 |
| `docs/inventory/api.md` | 갱신 | `/api/v0/pet/register`·`/pet/update` 판정을 `REDESIGN`·`진행중`으로 갱신, v1 엔드포인트 링크 추가 |
| `docs/domains/pet.md` | 갱신 | "pet 생성·수정 API" 절 신규 추가, 견종 표시 이름 조합 방식을 `LoadBreedPort` 구현 내용으로 갱신, `weight` non-null 정정 반영 |
| `docs/conventions/error-handling.md` | 갱신 | `PetErrorCode`를 구조화 포맷의 실제 구현 사례로 추가 완료 |
| `docs/architecture/hexagonal.md` | 갱신 | ArchUnit 규칙4가 이미 와일드카드로 전 도메인 자동 적용됨을 반영(독립 리뷰에서 발견한 문서-코드 불일치 정정) |
| `PetResponse.kt` | 코드 수정 | `weight` 타입을 `Double?` → `Double`로 정정(도메인 불변식·문서 계약과 일치) |
| `ExistsBreedPort.kt`/`BreedExistenceAdapter.kt` | 삭제 | 미사용 확인(grep 근거) 후 삭제 |
| `LoadBreedsPort.existsById`/`BreedPersistenceAdapter.existsById` | 삭제 | 유일한 호출부(`BreedExistenceAdapter`) 삭제로 미사용 확인, 전용 테스트 없음 확인 후 삭제. `BreedQueryServiceTest`의 fake 구현도 함께 제거 |
| `GlobalExceptionHandler.kt`/`CommonErrorCode.kt` | 코드 추가 | `IllegalStateException` → 409(`CONFLICT`) 전역 핸들러 추가(pet 범위를 넘는 공통 변경, 근거는 `docs/conventions/error-handling.md` §3) |
| `docs/conventions/error-handling.md` | 갱신 | `IllegalStateException` 처리 우선순위·`check()` 사용 기준(상태 위반 전용, 내부 버그 어설션 금지) 추가 |
