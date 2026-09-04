> 생성: 2026-09-02 22:02 · 최종 수정: 2026-09-04 19:28

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
| `relationship` | 보호자와의 관계 8종 고정값 Kotlin enum: `MOTHER`(엄마)·`FATHER`(아빠)·`EONNI`(언니)·`NUNA`(누나)·`OPPA`(오빠)·`HYUNG`(형)·`GUARDIAN`(보호자)·`ETC`(기타). 손윗형제 4종(언니/누나/오빠/형)은 "손윗형제의 성별 × 화자(보호자)의 성별" 조합이라 영어로 정확히 대응되는 단어가 없어 로마자 표기를 그대로 쓴다(레거시는 `ELDER_SISTER`/`OLDER_SISTER`처럼 억지로 영어 대응시켜 의미가 왜곡돼 있었다). `breed`(FCI 참조 데이터, 385건, 자체 메타데이터 보유)와 달리 참조 테이블로 두지 않는다 — 값이 고정이고 늘리려면 코드 배포가 필요하기 때문. `ETC`일 때만 `relationshipText` 필수(도메인 검증) |
| `weight` | 컬럼 타입은 DOUBLE(반려동물 체중은 소수점 단위가 실제로 의미 있어 확장성을 열어둠). 다만 현재 기획(1~99 정수)에 맞춰 도메인 모델(`Pet.create`)이 범위와 "소수점 없음"을 함께 검증한다. 레거시는 API 요청 DTO에서만 검증했다 |
| `breedId` | NOT NULL. `breeds`에 믹스견(1번)·기타(385번)가 있어 견종을 특정할 수 없는 경우도 표현 가능해 견종 미상 상태를 별도로 두지 않는다 |
| 대표견 단일성 | `pets.representative_user_id`(nullable, UNIQUE — 대표견이면 `user_id`와 같은 값, 아니면 NULL)로 DB가 보장한다. 최초 등록하는 pet은 자동으로 대표견이 되는 레거시 규칙을 유지한다. **대표견을 교체할 때는 반드시 기존 대표견을 먼저 해제(`clearRepresentative`+저장)한 뒤 새 대표견을 지정(`markAsRepresentative`+저장)해야 한다** — 순서를 바꾸면 UNIQUE 제약 위반으로 실패한다 |
| 최대 마릿수 | 사용자당 5마리. `SELECT ... FOR UPDATE`로 활성 pet 행을 잠근 뒤 등록하는 애플리케이션 레벨 잠금으로 처리한다(기존 행이 있는 경우 실제 MySQL로 검증됨. 활성 pet 0건 상태의 동시 등록까지는 미검증 — [`KD3-430`](../work/KD3-430-pet-domain-foundation-schema.md) 검증 결과 참고) |
| 삭제 | soft delete(`deleted_at`). 삭제 유스케이스는 후속 티켓(KD3-434) |
| 견종 표시 이름 | pet 테이블에 중복 저장하지 않는다. 조회 API가 `breedId`로 breed 도메인의 조회 포트를 호출해 응답 시점에 조합한다 |

상세 구현과 검증 상태는 [`KD3-430`](../work/KD3-430-pet-domain-foundation-schema.md)을 참고한다.

## 참조

- 시드 원본: `daeng_v1_back/scripts/migrations/KD3-370-create-breed.sql`(UTF-8, 385건). `docs/work/똑독_견종목록_2026-08-11.csv`는 CP949로 특수문자가 손상된 출처 확인용 사본이며 시드 생성 기준이 아니다
- breeds 스키마: `src/main/resources/db/migration/V3__create_breeds.sql`
- breed 코드: `domain/breed/`
- 레거시 pet 원본: `daeng_v1_back`의 `pet/model/Pet.java`, `pet/service/PetService.java`
- pets 스키마: `src/main/resources/db/migration/V4__create_pets.sql`
- pet 코드: `domain/pet/`
