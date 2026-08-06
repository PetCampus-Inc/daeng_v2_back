> 생성: 2026-08-02 13:45 · 최종 수정: 2026-08-04 17:37

# 외부 연동 인벤토리

이 문서는 Redis, S3, OIDC, 크롤링, 외부 API 등 신규 서버가 의존하거나 이관해야 할 외부 연동을 관리한다.

## 1. 판정 기준

| 판정 | 의미 |
|---|---|
| `KEEP` | 기존 연동 방식을 유지하거나 동일 계약으로 이관할 의존성 |
| `REDESIGN` | 기능은 필요하지만 구현 방식, 책임 경계, 인증/권한 계약을 바꿀 의존성 |
| `DROP` | 신규 서버에서 제거할 의존성 |
| `DEFER` | 사용 여부나 신규 방향 확인이 필요한 의존성 |

## 2. 작성 규칙

- 연동별로 사용 위치, 인증/권한 방식, 장애 시 사용자 영향과 fallback 여부를 가능한 한 분리해 기록한다.
- 인스턴스 사양, 네트워크, 배포, 모니터링, 백업, 컷오버, rollback은 `docs/operations/inventory.md`에 둔다.
- secret, token, key 값은 문서에 직접 적지 않는다.

## 3. 인벤토리

| 의존성 | 용도 | 사용 위치 | 판정 | 신규 방향 | 위험 | 후속 확인 |
|---|---|---|---|---|---|---|
| Redis | 리프레시 토큰, 이메일 인증, 유치원 데이터 저장소 후보 | 레거시 기준 확인 필요 | `DEFER` | auth/kindergarten 이관 시 재검토 | 키 충돌/전체 삭제 시 인증 상태 영향 | 키 목록, TTL, key prefix, 장애 시 fallback 확인 |
| S3 | 이미지 업로드/저장 | 레거시 기준 확인 필요 | `DEFER` | presigned URL 발급 정책 재검토 | 인증 없는 발급 위험 | 호출 API와 권한 정책 확인 |
| OIDC | Apple/Google/Kakao ID Token 검증 | auth | `DEFER` | 기존 직접 검증 방식 유지 후보 | provider별 key 검증 실패 처리 | provider별 설정/에러 계약 확인 |
| 도로명주소 API | 주소 검색 | address | `KEEP` | `GET /api/v0/address/search`에서 outbound client로 호출 | 장애 시 주소 검색 결과가 빈 결과로 반환됨 | key 발급/쿼터, 장애 알림 필요 여부 확인 |
| Kakao Local API | 주소 좌표 변환, 역 지오코딩 | address | `KEEP` | `GET /api/v0/address/geo`, `/reverse-geo`에서 outbound client로 호출 | 장애 시 좌표 변환/역 지오코딩 실패 | key 발급/쿼터, 장애 시 사용자 안내 방식 확인 |
| 크롤링 | 블로그 리뷰 조회 | kindergarten | `DEFER` | 신규 서버 직접 이관 여부 확인 필요 | Selenium/WebDriver 운영 비용 | 별도 서비스 분리 여부 확인 |
