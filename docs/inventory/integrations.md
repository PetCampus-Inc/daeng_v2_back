> 생성: 2026-08-02 13:45 · 최종 수정: 2026-08-31 12:10

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
- 인스턴스 사양, 네트워크, 배포, 모니터링, 백업, 컷오버, rollback은 [`docs/inventory/operations.md`](operations.md)에 둔다.
- secret, token, key 값은 문서에 직접 적지 않는다.
- 레거시는 계속 변경되므로, 재추출할 때마다 3절에 기준 커밋 SHA와 날짜를 갱신한다.

## 3. 추출 범위

`PetCampus-Inc/daeng_v1_back` `dev@2479b02c` (2026-08-30)의 `src/main/resources/application*.yml`, `docker-compose.yml`, `src/main/java/**/config`·`**/client`를 기준으로 추출했다.

## 4. 인벤토리

| 의존성 | 용도 | 사용 위치 | 판정 | 신규 방향 | 위험 | 후속 확인 |
|---|---|---|---|---|---|---|
| Redis | 리프레시 토큰·이메일 인증 코드, 유치원 목록/자동완성/가격 캐시, OIDC public key 캐시 | auth, kindergardeninfo(`KindergartenQueryService`, `AutocompleteService`, `LawCodeRedisLoader`, `SchoolProfileRedisSyncService`), `OidcPublicKeyClient` (신규 서버 미도입) | `REDESIGN` | 캐시(유치원/자동완성)와 상태 저장(토큰/인증코드)의 책임을 분리한 뒤 이관 | 캐시와 세션성 데이터가 같은 인스턴스에 있어 전체 삭제 시 인증까지 끊김 | 확인된 key prefix: `kindergarten:list`, `autocomplete:{name,prefix,jamo,region,filter}`, `*:pricing`. 각 TTL과 유실 시 fallback, 캐시/상태 인스턴스 분리 여부 |
| S3 | 이미지·앨범·증빙 파일 저장 | `S3Config`/`S3Uploader`/`S3UrlSigner`, `S3ImageUploadService`, album/memo/kindergarten-change | `REDESIGN` | presigned URL 발급을 도메인별 outbound 포트로 분리. 앨범은 `photos/upload-urls` → `photos/commit` 2단계 커밋 패턴 | 발급 시 소유권 검증이 없으면 타 유치원 object 접근 가능 | 버킷 3개(`knockdog-kindergarten-change-reports`, `knockdog-memo-attachments`, 기본)의 소유 도메인, presigned 만료 시간, DB 삭제 시 object 정리 주체 |
| OIDC | Apple/Google/Kakao ID Token 검증 | auth | `DEFER` | 기존 직접 검증 방식 유지 후보 | provider별 key 검증 실패 처리 | provider별 설정/에러 계약 확인 |
| 도로명주소 API | 주소 검색 | address | `KEEP` | `GET /api/v0/address/search`에서 outbound client로 호출 | 장애 시 주소 검색 결과가 빈 결과로 반환됨 | key 발급/쿼터, 장애 알림 필요 여부 확인 |
| Kakao Local API | 주소 좌표 변환, 역 지오코딩 | address | `KEEP` | `GET /api/v0/address/geo`, `/reverse-geo`에서 outbound client로 호출 | 장애 시 좌표 변환/역 지오코딩 실패 | key 발급/쿼터, 장애 시 사용자 안내 방식 확인 |
| Firebase Cloud Messaging | 푸시 발송 | `FirebaseConfig`/`FirebaseUtil`, `NotificationOutboxWorker`, `FcmService` | `REDESIGN` | outbox worker가 발송을 폴링(10초 간격, 최대 5회 재시도)하는 구조. 신규 서버에서 outbox 유지 여부부터 결정 | 서비스 계정 키가 EC2 호스트 파일(`serviceAccountKey.json`) 마운트에 의존 | 서비스 계정 키 주입 방식, 재시도/DLQ 정책, 발송 실패 관측 방법 |
| 국세청 사업자등록 상태조회 (odcloud) | 사업자등록번호 진위·휴폐업 확인 | business-registration, owner-verification | `KEEP` | `POST /api/v0/admin/business-registration/verify` 등에서 outbound client로 호출. QA3-188에서 폐업 유치원 재인증 차단의 판단 근거로 승격 | 장애 시 원장 인증 흐름 전체가 막힘 | 쿼터, 장애 시 fallback(임시 통과 여부), 응답 보존 기간 |
| TMAP API | 유치원 비교의 이동 시간 계산 | `TmapApiClient`, `ComparisonService` | `DEFER` | 비교 기능 유지 여부에 종속 | 장애 시 비교 화면의 이동 시간 누락 | 키 쿼터, 캐시 TTL(`cache.ttl.transit-days: 7`)의 신규 서버 유지 여부 |
| 네이버 대중교통 경로 API | 대중교통 경로 조회 | `NaverMapApiClient` | `DEFER` | TMAP과 역할이 겹치므로 하나로 정리 후 이관 | 비공식 endpoint(`pt.map.naver.com`) 의존 | TMAP과의 중복 제거, 실제 사용 화면 확인 |
| SMTP (Gmail) | 이메일 인증 코드 발송 | `EmailVerifyService` (`POST /api/v0/auth/email/send`, `/verify`) | `REDESIGN` | 개인 Gmail 계정 + 앱 비밀번호 방식. 신규 서버는 전송 전용 서비스로 교체 검토 | 계정 정지 시 이메일 인증 전면 중단, 발송 쿼터 제한 | 전송 서비스 선택, 발송 실패 처리, 인증 코드 TTL |
| Discord Webhook | 에러 로그 알림, healthcheck 결과 알림 | `logback-spring.xml`, `.github/workflows/healthcheck.yml` | `REDESIGN` | 관측 도구를 정한 뒤 알림 채널을 재배치. 운영 구성은 [`operations.md`](operations.md) 참고 | 로깅 경로에 외부 HTTP 호출이 있어 장애 시 지연 유발 가능 | 알림 대상 레벨, 실패 시 로깅 자체가 막히지 않는지 확인 |
| 네이버 플레이스 GraphQL | 블로그 리뷰 조회 (`GET /api/v0/kindergarten/{placeId}/blog-reviews`) | `NaverGraphQLClient` (kindergardeninfo) | `DEFER` | 0005에 따라 마이그레이션 보류. 신규 서버 직접 이관 여부 미정 | 비공식 endpoint라 스키마 변경 시 예고 없이 깨짐, 차단 위험 | 별도 서비스 분리 여부, 응답 캐시 정책. 0005 판단이 유지되는지 재확인 |
