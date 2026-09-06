> 생성: 2026-09-02 22:02 · 최종 수정: 2026-09-04 13:41

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

## 참조

- 시드 원본: `daeng_v1_back/scripts/migrations/KD3-370-create-breed.sql`(UTF-8, 385건). `docs/work/똑독_견종목록_2026-08-11.csv`는 CP949로 특수문자가 손상된 출처 확인용 사본이며 시드 생성 기준이 아니다
- 스키마: `src/main/resources/db/migration/V3__create_breeds.sql`
- 코드: `domain/breed/`
