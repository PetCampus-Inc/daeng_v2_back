> 생성: 2026-09-02 22:02 · 최종 수정: 2026-09-02 23:01

# pet 도메인

## 견종 기준 데이터

| 항목 | 현재 결정 |
|---|---|
| 소유 | pet 도메인이 `breeds` 기준 데이터를 소유한다 |
| 식별자 | `breeds.id`를 사용하며, 이후 pet은 `breed_id`만 저장한다 |
| 데이터 원본 | `daeng_v1_back/scripts/migrations/KD3-370-create-breed.sql`의 UTF-8 385건. CP949 CSV는 특수문자가 손상된 출처 확인용 사본으로만 보관한다 |
| 노출 순서 | 전체 목록은 `display_order` 오름차순. 1번 믹스견, 마지막 기타 |
| 검색 | 한글명 또는 별칭 부분 일치. 시작 일치, 포함 일치, 한글명 가나다순으로 정렬 |
| 공개 API | `GET /api/v1/breeds?query=`. v0는 신규 서버에 구현하지 않는다 |

상세 구현과 검증 상태는 [`KD3-418`](../work/KD3-418-breed-catalog-v1-api.md)을 참고한다.
