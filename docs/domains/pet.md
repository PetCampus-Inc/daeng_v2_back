> 생성: 2026-09-02 22:02 · 최종 수정: 2026-09-08 18:10

# pet 도메인

## 견종 기준 데이터

| 항목 | 현재 결정 |
|---|---|
| 소유 | pet 도메인이 `breeds` 기준 데이터를 소유한다 |
| 식별자 | `breeds.id`를 사용하며, 이후 pet은 `breed_id`만 저장한다 |
| 데이터 원본 | `daeng_v1_back/scripts/migrations/KD3-370-create-breed.sql`의 UTF-8 385건. CP949 CSV는 특수문자가 손상된 출처 확인용 사본으로만 보관한다 |
| 노출 순서 | 전체 목록은 `display_order` 오름차순. 1번 믹스견, 마지막 기타 |
| 검색 | 검색어·한글명·별칭 양쪽 모두 공백을 제거한 뒤 부분 일치. 시작 일치, 포함 일치, 한글명 가나다순으로 정렬 |
| 공개 API | `GET /api/v1/breeds?query=`. v0는 신규 서버에 구현하지 않는다 |
| 인증 | `GET /api/v1/breeds`는 인증 없이 공개(permitAll). 품종 조회는 로그인 여부와 무관하며 회원가입·반려견 등록 등 비로그인 화면에서도 필요하다 |
| 컬럼 제약 | `display_order`는 UNIQUE(제품 고정 노출 순서, 중복 불가). `fci_standard_number`·`alias`는 nullable — `fci_standard_number`는 FCI 미등록 품종(믹스견·기타)만 NULL, `alias`는 품종당 단일 값이며 없으면 NULL |
| 명칭 출처 | `name_en`·`name_ko`는 FCI 등록 품종은 FCI 공식 영문·국문 명칭, 믹스견·기타는 제품이 정한 명칭이다 |

상세 구현과 검증 상태는 [`KD3-418`](../work/KD3-418-breed-catalog-v1-api.md)을 참고한다.

## pet 소유 관계

| 항목 | 현재 결정 |
|---|---|
| 소유 모델 | 현재 1:N(사용자 1명이 pet 여러 마리 소유, pet은 소유자 1명). 레거시 `Pet`도 `user_id` 단일 FK다 |
| 다대다(가족 공유) 확장 시 주의점 | 대표견 여부(`pets.representative_user_id`)·최대 5마리 제약이 지금은 pet/user 단위로 직접 걸려 있다. 다중 소유자로 확장하면 이 컬럼을 pet이 아니라 소유 관계 테이블로 옮겨야 하고, 최대 마릿수 카운트도 관계 테이블 기준으로 다시 짜야 한다 — 스키마 마이그레이션(관계 테이블 추가 + backfill) 자체는 기계적이지만, 이 리팩터링은 그렇지 않다 |
| 확장 여부 | 확정된 요구사항 없음(KD3-430 검토 결과, [`docs/work/KD3-430-pet-domain-foundation-schema.md`](../work/KD3-430-pet-domain-foundation-schema.md) 참고). 착수 시 UX·권한 설계(초대, 연결 해제, 쓰기 권한)를 먼저 정한다 |

## pet 프로필과 불변식

| 항목 | 현재 결정 |
|---|---|
| 필드 | `name`·`profileImage`·`relationship`(+`relationshipText`)·`breedId`·`gender`·`birthYear`(연도만)·`weight`·`isNeutered`. 레거시(`daeng_v1_back`의 `pet/model/Pet.java`) 대조로 확정했다 |
| 문자열 필드 길이·blank 검증 | `name`은 blank 불가·100자 이하(`pets.name` `VARCHAR(100)`), `profileImage`는 500자 이하(`pets.profile_image` `VARCHAR(500)`), `relationshipText`는 100자 이하(`pets.relationship_text` `VARCHAR(100)`)를 `Pet.create`/`Pet.update`가 검증한다 — DB 컬럼 길이를 그대로 상한으로 쓴다. 이 검증이 없으면 blank `name`이 그대로 저장되거나(DB `NOT NULL`은 빈 문자열을 막지 못함) 컬럼 길이 초과 시 `DataIntegrityViolationException`이 `GlobalExceptionHandler`의 catch-all(500)로 떨어진다(KD3-431 구현 완료 후 발견해 정정). `profileImage`가 빈 문자열(`""`)일 때 유효한 값으로 볼지는 결정하지 않았다 |
| `relationship` | 보호자와의 관계 8종 고정값 Kotlin enum: `MOTHER`(엄마)·`FATHER`(아빠)·`EONNI`(언니)·`NUNA`(누나)·`OPPA`(오빠)·`HYUNG`(형)·`GUARDIAN`(보호자)·`ETC`(기타). 손윗형제 4종(언니/누나/오빠/형)은 "손윗형제의 성별 × 화자(보호자)의 성별" 조합이라 영어로 정확히 대응되는 단어가 없어 로마자 표기를 그대로 쓴다(레거시는 `ELDER_SISTER`/`OLDER_SISTER`처럼 억지로 영어 대응시켜 의미가 왜곡돼 있었다). `breed`(FCI 참조 데이터, 385건, 자체 메타데이터 보유)와 달리 참조 테이블로 두지 않는다 — 값이 고정이고 늘리려면 코드 배포가 필요하기 때문. `relationshipText`는 `ETC`일 때만 필수이고, 그 외에는 반드시 NULL이어야 한다(양방향 도메인 검증) — `relationship`을 `ETC`가 아닌 값으로 바꾸면 기존 `relationshipText`는 자동으로 지워진다(레거시는 필드를 지우는 경로 자체가 없어 값이 영구히 남는 결함이 있었다) |
| `weight` | 컬럼 타입은 DOUBLE(반려동물 체중은 소수점 단위가 실제로 의미 있어 확장성을 열어둠), 컬럼은 **NOT NULL**. 현재 기획(1~99 정수)에 맞춰 범위와 "소수점 없음"을 검증한다. 생성 시 필수이며(레거시 등록 API와 동일) **수정 후에도 절대 지울 수 없다** — `profileImage`/`birthYear`/`isNeutered`와 달리 PATCH로도 null을 허용하지 않는 유일한 nullable-후보 필드다 |
| `breedId` | NOT NULL. `breeds`에 믹스견(1번)·기타(385번)가 있어 견종을 특정할 수 없는 경우도 표현 가능해 견종 미상 상태를 별도로 두지 않는다 |
| 대표견 단일성 | `pets.representative_user_id`(nullable, UNIQUE — 대표견이면 `user_id`와 같은 값, 아니면 NULL)로 DB가 보장한다. 최초 등록하는 pet은 자동으로 대표견이 되는 레거시 규칙을 유지한다. **대표견을 교체할 때는 반드시 기존 대표견을 먼저 해제(`clearRepresentative`+저장)한 뒤 새 대표견을 지정(`markAsRepresentative`+저장)해야 한다** — 순서를 바꾸면 UNIQUE 제약 위반으로 실패한다 |
| 최대 마릿수 | 사용자당 5마리. `SELECT ... FOR UPDATE`로 활성 pet 행을 잠근 뒤 등록하는 애플리케이션 레벨 잠금으로 처리한다. 활성 pet이 0건이라 잠글 행이 없는 상태의 동시 등록도, 항상 존재하는 `users` 행을 먼저 잠그는 `LockUserPort`로 직렬화한다(Testcontainers 기반 자동화 테스트로 검증 — [`KD3-430`](../work/KD3-430-pet-domain-foundation-schema.md) 검증 결과 참고) |
| 삭제 | soft delete(`deleted_at`). 사용자가 직접 삭제하는 유스케이스는 `DELETE /api/v1/pets/{petId}`(KD3-434, 구현 완료) — 아래 "pet 삭제 API" 참고 |
| 탈퇴 회원 pet 정리(미착수) | 레거시엔 `WithdrawnUserDeleteService`라는 스케줄러가 있어, 회원 탈퇴 후 유예 기간이 지나면 그 사용자의 pet을 전부 물리 삭제한다(앨범·북마크·메모·알림 등과 함께 계정 탈퇴 정리 작업 일부, `petRepository.deleteAllByUserPks(...)`). v2엔 이 메커니즘 자체가 없다 — KD3-434(사용자가 직접 pet 하나를 지우는 것)와는 다른, **계정 탈퇴가 트리거하는 예약 작업**이라 auth 도메인 쪽에서 발동돼야 한다. 개인정보 보관 정책과 관련된 사안이라 방치하면 탈퇴한 사용자의 pet 데이터가 계속 안 지워진 채 남는다. 2026-09-08 레거시 전체 대조 조사에서 발견 — 아직 어느 티켓에도 안 걸려 있다. auth 쪽 문서·티켓화는 후속으로 미룬 상태(사용자 확인, 2026-09-08) |
| 견종 표시 이름 | pet 테이블에 중복 저장하지 않는다. `LoadBreedPort.findById`(breed 도메인의 `LoadBreedsPort.findById`에 위임)로 응답 시점에 `nameKo`/`alias`를 조합한다(KD3-431) |

상세 구현과 검증 상태는 [`KD3-430`](../work/KD3-430-pet-domain-foundation-schema.md)을 참고한다.

## pet 생성·수정 API

| 항목 | 현재 결정 |
|---|---|
| 엔드포인트 | `POST /api/v1/pets`(생성), `PATCH /api/v1/pets/{petId}`(부분 수정). 레거시 `POST /api/v0/pet/register`는 인벤토리에서 원래 `KEEP`으로 판정돼 있었으나, RESTful URL로 재설계하기로 확정하며 `REDESIGN`으로 정정했다(KD3-431) |
| PATCH의 null 처리 | "필드 생략"(유지)과 "명시적 null"(지우기)을 구분해야 하는 nullable 필드(`profileImage`/`birthYear`/`isNeutered`)는 `JsonNullable<T>`(`org.openapitools:jackson-databind-nullable`)로 받는다. 레거시는 이 구분 자체가 없어 필드를 지우는 경로가 없었다. `weight`는 이 그룹에 포함되지 않는다 — 항상 non-null이라 PATCH도 값 변경만 허용하고 명시적 `null`은 400으로 거부한다(위 "pet 프로필과 불변식"의 `weight` 행 참고) |
| 소유권·상태 검증 | `petId`가 없거나 soft delete된 pet이면 404(`PET-404-1`), 본인 소유가 아니면 403(`PET-403-1`) |
| 에러 코드 | `PetErrorCode`(`domain/pet/application/PetErrorCode.kt`) — 레거시(`daeng_v1_back`의 `ErrorCode.java`) 값을 그대로 재사용(`PET-404-1`/`PET-403-1`/`PET-400-1`/`PET-400-2`). `breedId` 참조 방식으로 생긴 신규 검증(`NOT_FOUND_BREED`, `PET-400-3`)만 새로 추가했다. 단순 필드 형식 오류(예: `weight` 범위, PATCH의 `weight` 명시적 null)는 전용 코드 없이 도메인·서비스 검증 실패 → 공통 `INVALID_INPUT_VALUE`(400)로 처리한다 |
| 응답 | pet 전체 필드 + `breedNameKo`/`breedAlias`(견종 표시 정보, 위 "견종 표시 이름" 참고). 레거시 `PetResponse`와 달리 `createdAt`/`updatedAt`은 포함하지 않는다 — `Pet` 도메인 모델이 `User`/`SocialUser`처럼 audit 타임스탬프를 도메인에 담지 않는 이 프로젝트 관례를 따른다 |

상세 구현과 검증 상태는 [`KD3-431`](../work/KD3-431-pet-profile-create-update-api.md)을 참고한다.

## pet 목록·단건 조회 API

| 항목 | 현재 결정 |
|---|---|
| 엔드포인트 | `GET /api/v1/pets`(본인 소유 활성 pet 목록), `GET /api/v1/pets/{petId}`(단건). 레거시 `GET /api/v0/pet/list`는 원래 `KEEP`, `GET /api/v0/pet/{petId}`는 `DEFER`로 판정돼 있었으나, 둘 다 RESTful `v1`로 재설계하기로 확정하며 `REDESIGN`으로 정정했다(KD3-432) |
| 목록 정렬 | **대표견 우선 → 나머지는 이름 오름차순**(Java 기본 `String` 비교, 별도 로케일 처리 없음). 레거시 `PetService.getPets()`(커밋 `8dcaee89`, KD3-299)의 3단계 규칙("대표견 → 연결된(ACTIVE) 강아지 가나다순 → 미연결 강아지 가나다순")에서 유치원 연결 여부 기준(2·3단계)만 이 도메인 스코프(유치원 연결 제외) 밖이라 자연히 빠지고, 범용 1단계만 계승했다. **후속 과제**: 유치원 연결 상태(`school_pet_membership` 상당)가 pet 도메인에 들어오는 티켓에서, 레거시 2·3단계(연결된 pet 우선순위)를 이 목록 정렬에 다시 반영할지 제품 요구를 확인해야 한다 — 지금은 그 데이터 자체가 없어서 뺀 것이지, "필요 없다"고 결정한 게 아니다 |
| 정렬의 알려진 한계 | 완성형 한글 이름은 기본 `String` 비교로도 가나다순이 정확히 나오지만(완성형 음절 블록 U+AC00~D7A3의 코드 배정이 초성 순서와 일치), 이름이 완성형 음절이 아닌 낱자모(예: "ㅋㅋ")로 시작하면 순서가 사전과 어긋난다. 이는 이 프로젝트만의 문제가 아니라 Unicode 기술위원회도 명시한 한계다([L2/17-078 "Hangul Sort Order"](https://www.unicode.org/L2/L2017/17078-hangul-sort-order.pdf)) — JDK의 `Collator.getInstance(Locale.KOREAN)`으로도 기본으로는 해결되지 않으며, 완전한 해결은 한국 국가표준 KS X 1026-1 기반 커스텀 비교 로직이 필요하다. 레거시도 이 한계를 그대로 안고 프로덕션에서 운영 중이었고 관련 버그 수정 이력이 없어, 이번 범위(최대 5마리)에서도 그대로 받아들이기로 했다(KD3-432) |
| 소유권·상태 검증 | 단건은 `petId`가 없거나 soft delete된 pet이면 404(`PET-404-1`), 본인 소유가 아니면 403(`PET-403-1`). 목록은 본인 소유 활성 pet만 조회 대상이라 별도 인가 판정이 필요 없다 |
| 응답 | 생성·수정 API와 동일한 `PetResponse` 계약(전체 필드 + `breedNameKo`/`breedAlias`)을 재사용한다 |

상세 구현과 검증 상태는 [`KD3-432`](../work/KD3-432-pet-profile-query-api.md)를 참고한다.

## pet 대표견 설정 API

| 항목 | 현재 결정 |
|---|---|
| 엔드포인트 | `PUT /api/v1/pets/{petId}/representative` — 같은 pet에 반복 요청해도 결과가 같은 멱등 연산이라 POST가 아닌 PUT을 쓴다. 레거시 `POST /api/v0/pet/representative/{petId}`는 인벤토리에서 원래 `KEEP`으로 판정돼 있었으나, `POST /api/v1/pets`·`PATCH /api/v1/pets/{petId}`와 동일한 RESTful URL 패턴으로 재설계하기로 확정하며 `REDESIGN`으로 정정했다(KD3-433) |
| 대표견 전환 처리 | 위 "pet 프로필과 불변식"의 대표견 단일성 규칙(기존 대표견 해제 → 신규 대표견 지정 순서)을 `SavePetPort.setRepresentativeWithinLock`이 트랜잭션 안에서 수행한다. 이미 대표견인 pet에 재요청하면 DB 쓰기 없이 그대로 반환한다(멱등) |
| 동시성 처리 | `LockUserPort.lockById`로 `users` 행을 먼저 잠가 동일 사용자의 동시 요청을 직렬화한다 — "pet 프로필과 불변식"의 최대 마릿수 등록 잠금과 같은 패턴이며, 같은 `petJpaRepository.findAllActiveByUserIdForUpdate` 잠금 쿼리를 재사용한다(Testcontainers 기반 동시 요청 5건 테스트로 검증) |
| 소유권·상태 검증 | `petId`가 없거나 soft delete된 pet이면 404(`PET-404-1`), 본인 소유가 아니면 403(`PET-403-1`) — pet 생성·수정 API와 동일한 에러 코드를 재사용한다 |
| 응답 | pet 생성·수정 API와 동일하게 `PetResponse`(pet 전체 필드 + `breedNameKo`/`breedAlias`)를 반환한다. 레거시는 `Response<Void>`였으나, 이 프로젝트의 다른 pet 엔드포인트 관례를 따른다 |

상세 구현과 검증 상태는 [`KD3-433`](../work/KD3-433-pet-representative-api.md)을 참고한다.

## pet 삭제 API

| 항목 | 현재 결정 |
|---|---|
| 엔드포인트 | `DELETE /api/v1/pets/{petId}`, 응답 `204 No Content`. 레거시 `POST /api/v0/pet/remove/{petId}`는 인벤토리에서 원래 `미착수`로 남아 있었으나, 다른 pet 엔드포인트와 동일한 RESTful URL 패턴으로 재설계해 구현했다(KD3-434) |
| 삭제 대상이 대표견일 때 | **레거시엔 없는 v2 신규 동작**: 레거시 `PetService.removePet`은 자동 승격 로직이 없어 대표견을 지우면 그냥 대표견 없음 상태로 남았다. v2는 남은 활성 pet 중 정렬 1순위(대표견 우선 → 이름순과 같은 비교자, 대표견 자신이 삭제 대상이라 실질적으로 이름순 1번)를 자동으로 새 대표견으로 승격한다. 남은 pet이 없으면 대표견 없음 상태로 둔다 |
| 락 분기 | 대표견이 아닌 pet은 `Pet.delete()` + `SavePetPort.save`(`@Version` 낙관적 락)만으로 가볍게 처리한다. 대표견인 pet만 `SavePetPort.deleteAndPromoteWithinLock`으로 "pet 대표견 설정 API"와 같은 `users` 행 비관적 락 패턴을 타 삭제+승격을 한 트랜잭션으로 묶는다 |
| UNIQUE 제약과의 상호작용 | `pets.representative_user_id` UNIQUE는 `deleted_at`과 무관하게 걸린다 — 대표견을 삭제할 때 그 pet의 `representative_user_id`를 null로 같이 지우지 않으면 이후 누구도 새 대표견으로 승격될 수 없다(`Pet.delete()`가 `isRepresentative`도 `false`로 지워 자동 처리). 삭제 저장과 새 대표견 승격 저장이 같은 UNIQUE 컬럼을 건드리므로, "pet 대표견 설정 API"의 flush-순서 버그와 같은 이유로 그 사이에 명시적 flush가 필요하다 |
| 소유권·상태 검증 | `petId`가 없거나 이미 soft delete된 pet이면 404(`PET-404-1`), 본인 소유가 아니면 403(`PET-403-1`) — 다른 pet 엔드포인트와 동일한 에러 코드를 재사용한다 |

상세 구현과 검증 상태는 [`KD3-434`](../work/KD3-434-pet-delete-api.md)을 참고한다.

## 참조

- 시드 원본: `daeng_v1_back/scripts/migrations/KD3-370-create-breed.sql`(UTF-8, 385건). `docs/work/똑독_견종목록_2026-08-11.csv`는 CP949로 특수문자가 손상된 출처 확인용 사본이며 시드 생성 기준이 아니다
- breeds 스키마: `src/main/resources/db/migration/V10__create_breeds.sql`
- breed 코드: `domain/breed/`
- 레거시 pet 원본: `daeng_v1_back`의 `pet/model/Pet.java`, `pet/service/PetService.java`
- pets 스키마: `src/main/resources/db/migration/V11__create_pets.sql`
- pet 코드: `domain/pet/`
