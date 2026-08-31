> 생성: 2026-08-03 14:00 · 최종 수정: 2026-09-01 01:10

# KD3-258 — User 엔티티 + 소셜 로그인 회원가입 + 인증/인가 기반 구축

| 항목 | 값 |
|---|---|
| Jira | `KD3-258` |
| 브랜치 | `feat/KD3-258-user-social-auth` |
| 상위 에픽 | `KD3-194` |

## 현재 제어점

- 활성 workflow: `003-migration`, `005-new-feature`
- 현재 공통 단계: `5` (PR #4 리뷰 반영 중)
- 다음 결정 또는 전환 조건: 머지 가능 상태. 미결 질문 없음 — KEEP parity는 프론트 확인으로, Flyway 정합성 자동 검증은 보류로 정리됐다

## 작업 목표

`auth` 도메인을 정석형 헥사고날 구조로 신규 구축한다. 완료 시 클라이언트가 (1) Apple/Google/Kakao ID Token으로 소셜 인증을 검증하고, (2) 신규 유저를 회원가입시키고, (3) 로그인해서 액세스/리프레시 토큰을 받고, (4) 토큰을 재발급/로그아웃할 수 있게 된다. 이 도메인이 완료되면 이후 모든 도메인이 전제하는 인증 기반(`SecurityFilterChain` + 토큰 검증)이 마련된다.

## 작업 범위

### 신규 패키지: `domain/auth/`

```
domain/auth/
  domain/                         User, SocialUser, UserAddress 순수 모델 + VO(UserId, UserCode, Email, Provider 등)
  application/
    port/input/                   VerifyOidcUseCase, RegisterUserUseCase, LoginUseCase, RefreshTokenUseCase, LogoutUseCase
    port/output/                  LoadUserPort, SaveUserPort, LoadSocialUserPort, SaveSocialUserPort, TokenPort, LoadRefreshTokenPort/SaveRefreshTokenPort
    service/                      각 UseCase 구현체
  adapter/
    inbound/web/                  OidcVerificationController, AuthController, UserController (유스케이스별 분리)
    outbound/persistence/         UserJpaEntity, SocialUserJpaEntity, UserAddressJpaEntity + Repository + Mapper + PersistenceAdapter
    outbound/cache/                RedisRefreshToken(@RedisHash) + RedisRefreshTokenAdapter
    outbound/oidc/                 AppleTokenVerifier, GoogleTokenVerifier, KakaoTokenVerifier (JWKS 기반 서명 검증)
```

### 엔드포인트 (7개)

| 메서드/경로 | 유스케이스 | 인증 요구 |
|---|---|---|
| `POST /api/v1/auth/oidc-verifications` | VerifyOidcUseCase | 공개 |
| `POST /api/v1/auth/login` | LoginUseCase | 공개 (OIDC 임시 토큰으로 자체 검증) |
| `POST /api/v1/auth/refresh` | RefreshTokenUseCase | 공개 (리프레시 토큰으로 자체 검증) |
| `POST /api/v1/auth/logout` | LogoutUseCase | 공개 (리프레시 토큰으로 자체 검증) |
| `POST /api/v1/users` | RegisterUserUseCase | 공개 (OIDC 임시 토큰으로 자체 검증) |
| `POST /api/v0/user/agreements` | AgreeToTermsUseCase | 인증 필요 (기본 deny) |
| `GET /api/v0/user/agreements/status` | GetAgreementStatusUseCase | 인증 필요 (기본 deny) |

약관 동의 2개만 `v0` 경로인 이유는 아래 §방향 논의 9를 참고한다.

### DB 스키마 (신규 Flyway 마이그레이션)

`knockdog_server` 레포의 `docs/superpowers/specs/2026-08-02-db-schema-redesign-design.md`(별도 레포, 이 저장소에서는 링크 불가)를 기준으로 하되, 이번 브레인스토밍에서 아래와 같이 조정한다.

```sql
CREATE TABLE users (
  id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_code               VARCHAR(8)   NOT NULL UNIQUE,
  nickname                VARCHAR(100),                   -- NULL 허용 (§방향 논의 8)
  profile_image           VARCHAR(500),
  info_receive_email      VARCHAR(255),
  gender                  VARCHAR(20),
  phone_number            VARCHAR(20),
  emergency_phone_number  VARCHAR(20),
  created_at              DATETIME(6) NOT NULL,
  updated_at              DATETIME(6) NOT NULL,
  deleted_at              DATETIME(6)
);

CREATE TABLE social_users (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT,                        -- FK 제약 없음, 값만 저장
  provider     VARCHAR(20)  NOT NULL,          -- KAKAO, GOOGLE, APPLE
  provider_id  VARCHAR(255) NOT NULL,
  email        VARCHAR(255) NOT NULL,
  name         VARCHAR(255),
  picture      VARCHAR(500),
  status       VARCHAR(20)  NOT NULL,          -- LINKED, UNLINKED, PENDING
  linked_at    DATETIME(6),
  created_at   DATETIME(6) NOT NULL,
  updated_at   DATETIME(6) NOT NULL,
  deleted_at   DATETIME(6),
  UNIQUE (provider, provider_id)
  -- email UNIQUE 제약 없음 (아래 "방향 논의" 참고)
);

CREATE TABLE user_addresses (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,               -- FK 제약 없음
  type          VARCHAR(20) NOT NULL,           -- HOME, OTHER (§방향 논의 8)
  alias         VARCHAR(20),
  address       VARCHAR(200) NOT NULL,
  road_address  VARCHAR(200),
  lat           DOUBLE NOT NULL,
  lng           DOUBLE NOT NULL,
  created_at    DATETIME(6) NOT NULL,
  updated_at    DATETIME(6) NOT NULL,
  deleted_at    DATETIME(6)
);

-- V2 (A-3.5, §방향 논의 9). append-only 이력이라 BaseEntity 공통 컬럼을 두지 않는다.
CREATE TABLE user_agreements (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL,               -- FK 제약 없음
  term_type  VARCHAR(30) NOT NULL,               -- TERMS_OF_SERVICE, PRIVACY_POLICY, AGE_OVER_14
  agreed_at  DATETIME(6) NOT NULL,
  UNIQUE (user_id, term_type)
);
```

`users`/`social_users` 모두 `status`(ACTIVE/WITHDRAWN) 컬럼을 두지 않는다 — `users`는 `deleted_at`으로 탈퇴 여부를 표현(§ 방향 논의 참고). `user_notification_settings`, `withdraw_reasons`는 이번 범위에서 제외(아래 참고).

### 공통 인프라 신규 추가

- `global/persistence/BaseEntity.kt` — `@MappedSuperclass`, `createdAt`/`updatedAt`(`@CreatedDate`/`@LastModifiedDate`)/`deletedAt`. `KnockdogApplication`에 `@EnableJpaAuditing` 추가.
- `build.gradle.kts`: `spring-boot-starter-security`, `spring-boot-starter-data-redis`, `io.jsonwebtoken:jjwt-api`/`jjwt-impl`/`jjwt-jackson` 추가.
- `SecurityFilterChain`(기본 deny, ADR 0007) + 커스텀 `OncePerRequestFilter`(ID Token 아님 — 자체 발급 액세스 토큰 검증, ADR 0006) + 단일 `ROLE_USER`.
- `db/migration/V1__create_auth_tables.sql` (Flyway) + `FLYWAY_ENABLED=true`, `JPA_DDL_AUTO=validate`로 전환 (§ 방향 논의 참고).
- `HexagonalArchitectureTest.kt` 규칙 4 대상 패키지에 `domain.auth.domain` 추가.

## 작업 제외 범위

- **A-5 회원 탈퇴, A-6 이메일 인증, A-7 개발용 로그인**: 이번 브레인스토밍에서 범위로 정하지 않음. 후속 티켓.
- **`gender`/`phone_number`/`emergency_phone_number` 입력**: 컬럼은 스키마에 두지만, 회원가입 요청(`RegisterUserUseCase`)에는 포함하지 않는다 — 레거시 회원가입 화면도 이 값들을 받지 않았고(닉네임/프로필이미지/주소/`info_receive_email`만 받음), 마이페이지에서 별도 입력받는 것으로 보이는데 마이페이지 자체가 이번 범위 밖이다. 값은 전부 `null`로 생성된다.
- **알림 수신 설정**: 스키마/코드 모두에서 제외한다. 제외 근거가 KD3-402 재대조로 바뀌었다 — 레거시에 `user_notification_setting`과 `notification_preference`가 **공존**하고(KD3-287이 앞의 테이블을 건드리지 않고 뒤를 새로 만듦) 어느 쪽이 진실인지 미정이다. notification 도메인 티켓에서 통합 대상을 먼저 확정해야 여기서 만들 것이 정해진다.
- **소셜 계정 재연동(A-4, `ReconnectSocialUserUseCase`)**: `VerifyOidcUseCase`가 `PENDING`/`EMAIL_ALREADY_EXISTS` 응답은 내려주지만, 실제 병합(재연동) 액션은 이번 범위에 없다. 클라이언트는 이 응답을 받아도 아직 연동을 완료할 방법이 없다 — 후속 티켓에서 A-4를 붙여야 실제로 동작한다.
- **owner 도메인 인증 적용**: 이번에 `SecurityFilterChain` 기본 deny를 도입하면 기존 `OwnerController`(`/owners`, `/owners/{id}`)도 기본적으로 인증을 요구하게 된다. 이 엔드포인트들을 permit 목록에 올릴지, 인증을 요구하게 둘지는 owner 도메인 담당 범위이므로 이번 티켓에서 결정하지 않는다 — 일단 permit 목록에 넣지 않고, 완료 확인 시 실제 영향(401 여부)만 확인한다.

## 방향 논의 및 결정 사항

1. **DB 스키마 소스**: 초기엔 이 저장소의 `docs/domains/auth.md`(레거시 스키마 기준, `ShortIdGenerator` 8자 PK 등)를 볼지 `docs/specs/2026-07-30-auth-daycare-schema-draft.md`(신규 초안, 미해결 질문 다수)를 볼지 논의했으나, `knockdog_server` 레포에 더 최신이고 확정된 `2026-08-02-db-schema-redesign-design.md`가 있어 이걸 기준으로 확정했다. 스탭(원장/선생님)도 전부 소셜로그인으로 통일하는 결정이 이미 내려져 있어 비밀번호 컬럼이 필요 없다.

2. **동일 이메일, 다른 provider 충돌 처리**: 웹 서비스 일반적으로 (a) 완전 별개 계정 취급, (b) 검증된 이메일 기준 자동 병합, (c) 충돌 감지 후 사용자 확인을 거쳐 수동 연동, (d) 병합 기능 없이 차단 중 하나를 쓴다. 자동 병합(b)은 provider가 이메일 검증을 보장하지 않으면(Kakao 등) 계정 탈취 위험이 있어 배제. 레거시 코드(`VerifyOidcService.java`) 확인 결과 (c) 방식이었다 — `social_user` row를 새로 만들되 `PENDING` 상태로 표시하고 `EMAIL_ALREADY_EXISTS` 코드를 응답, 실제 연동은 별도 재연동 플로우(A-4, 이번 범위 밖)가 담당. 이 로직을 그대로 포팅하기로 했다.
   - 이로 인해 `social_users.email`에는 **UNIQUE 제약을 걸지 않는다** — `knockdog_server`의 스키마 재정의 문서는 UNIQUE를 걸어뒀는데, 이는 레거시의 "동일 이메일 다중 provider row 허용" 동작과 충돌한다는 걸 이번에 발견했다. ADR 0006(로직 변경 없이 이관) 취지에 따라 로직을 우선하고 스키마 쪽 제약을 뺀다.

3. **BaseEntity 컨벤션 신설**: `created_at`/`updated_at`/`deleted_at`을 모든 엔티티가 `BaseEntity`로 공통 상속하고, 별도 `status` 컬럼은 BaseEntity로 표현 불가능한 상태에만 둔다는 컨벤션을 이번에 처음 정했다(기존 `OwnerJpaEntity`엔 아직 없음 — 이번 작업으로 신설, 기존 owner 엔티티 소급 적용은 범위 밖).
   - `users`: 기존 `UserStatus`(ACTIVE/WITHDRAWN)를 없애고 `deleted_at IS NOT NULL`로 탈퇴 여부를 표현한다. 도메인은 `user.isWithdrawn()`/`user.withdraw()`로 감싼다.
   - `social_users`: `status`(LINKED/UNLINKED/PENDING)는 soft-delete로 표현 불가능한 3단계 상태라 그대로 유지한다.

4. **FK 제약 미적용 + 연관관계 매핑**: 실무 관행에 따라 DB에는 FK 제약(`REFERENCES`)을 걸지 않되, JPA 엔티티에서는 `@ManyToOne(fetch = LAZY)` + `@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))`로 연관관계 매핑 자체는 유지한다 — Hibernate의 지연 로딩은 컬럼 값만으로 동작하므로 실제 DB 제약 유무와 무관하게 작동한다. 이 프로젝트는 스키마를 Hibernate 자동생성이 아니라 Flyway로 관리하므로, 실질적인 결정은 마이그레이션 SQL에 `REFERENCES`를 안 쓰는 것이고 `NO_CONSTRAINT` 애너테이션은 향후 `ddl-auto: validate` 전환 시를 위한 보험이다.

5. **Flyway 전환**: `docs/conventions/infra.md` §3에 예고된 "마이그레이션 파일이 쌓이기 시작하면 전환" 시점이 이번 작업이다. `FLYWAY_ENABLED=true`, `JPA_DDL_AUTO=validate`로 전환하고, `db/migration/V1__create_auth_tables.sql`을 이번 작업의 첫 마이그레이션 파일로 작성한다.

6. **3종 토큰 체계 유지 (ADR 0006)**: 액세스 토큰(JWT, 헤더) / 리프레시 토큰(JWT, Redis 저장 + HttpOnly·Secure·SameSite=None 쿠키, TTL 30일) / OIDC 임시 토큰(JWT, 쿠키, TTL 20분, `provider`/`email`/`type` 클레임 + subject=`providerId`)을 레거시 그대로 유지한다. 액세스/리프레시 토큰 subject는 `UserCode`(8자 외부 노출 코드) — 내부 `id`(Long)는 API로 노출하지 않는다.

7. **`info_rcv_email` → `info_receive_email` 이름 변경**: 컬럼/필드명 축약이 불명확하다는 지적에 따라 풀어씀. 기능(마이페이지에서 조회/수정하는 부가 이메일)은 유지하되 마이페이지 자체는 범위 밖이라 이번엔 값만 받아서 저장한다.

8. **레거시 KD3-372 계약 변경 반영 (KD3-402 재대조)**: 인벤토리를 레거시 `dev@2479b02c`(2026-08-30) 기준으로 재대조하면서, 이 문서가 기준으로 삼은 `2026-08-02-db-schema-redesign-design.md` 이후에 레거시가 계약을 바꾼 걸 발견했다. 둘 다 신규 서버가 **제품이 이미 버린 계약을 되살리는** 형태라 레거시를 따라간다.
   - `AddressType`에서 `WORK` 제거 — 레거시가 KD3-372(2026-08-20)에서 `WORK`를 `OTHER`로 통합하고 기존 데이터도 마이그레이션했다. 컬럼이 `VARCHAR`라 DDL 변경은 불필요하고 enum과 요청 타입만 바뀐다.
   - `users.nickname`을 NULL 허용으로 — 같은 KD3-372가 레거시 `user.nickname`을 NULL 허용으로 바꿨고 `RegisterRequest.nickname`에도 `@NotBlank`가 없다. `NOT NULL`로 두면 닉네임 없는 가입이 실패한다.
   - `V1__create_auth_tables.sql`은 새 마이그레이션을 추가하지 않고 직접 수정했다. 이 파일은 KD3-258에서 처음 도입돼 아직 어떤 환경에도 적용된 적이 없고, 존재한 적 없는 테이블을 `ALTER`하는 V2를 남기는 편이 스키마 이력을 더 읽기 어렵게 만들기 때문이다. **로컬에서 V1을 이미 적용해 둔 사람은 로컬 DB를 초기화해야 한다.**

9. **약관 동의(A-3.5)를 `v0` 경로로 구현**: KD3-402 재대조에서 `POST /api/v0/user/agreements`와 `GET /api/v0/user/agreements/status`가 `KEEP`(프론트 호출 확인)으로 판정돼 범위에 들어왔다. 다른 auth API처럼 `v1`으로 옮기지 않는다 — `KEEP`은 path/method와 요청·응답 필드를 유지해야 하고([`api-migration.md`](../rules/api-migration.md) §2), 프론트가 이미 이 경로를 호출 중이라 경로를 바꾸면 breaking change다. `AgreementTermType` enum 이름도 프론트가 요청 본문에 그대로 싣기 때문에 레거시와 동일하게 둔다.
   - 중복 동의 처리는 레거시와 구현이 다르다. 레거시는 `insertIgnoringDuplicateKey`로 중복을 흘려보내는데, 여기서는 이미 동의한 약관을 뺀 차집합만 저장한다 — `(user_id, term_type)` unique를 건드리지 않고 최초 동의 시각을 보존한다. 외부에서 관찰되는 동작(재제출해도 200, 이력 1건 유지)은 같다.
   - `UserAgreementJpaEntity`는 `BaseEntity`를 상속하지 않는다. 동의는 갱신·soft delete 대상이 아니라 append-only 이력이라 `updated_at`/`deleted_at`이 의미가 없다.

## 완료 확인 기준

- ArchUnit 전체 규칙 통과 (`domain.auth.domain` 규칙 4 등록 포함)
- ktlint 통과
- 도메인 단위 테스트: `User`(주소 HOME 불변식, `withdraw`/`isWithdrawn`), `SocialUser`(`link`/`unlink`, 상태 전이)
- `VerifyOidcUseCase` 단위 테스트: LINKED/UNLINKED/PENDING(다른 provider 동일 이메일) 3분기 모두 커버
- `LoginUseCase` 단위 테스트: 정상 로그인, 미연동 소셜계정, 탈퇴 후 7일 이내/이후 분기
- provider별 OIDC 검증기 단위 테스트 (JWKS mock)
- Flyway 마이그레이션이 로컬 MySQL에 정상 적용됨 (`./gradlew flywayMigrate` 또는 앱 기동 시 자동 적용)
- `KnockdogApplicationTests`(컨텍스트 로딩) 통과 — Redis 연결 없이도 컨텍스트가 뜨는지 확인(레이지 커넥션 확인)
- `SecurityFilterChain` 도입 후 기존 `OwnerController` 엔드포인트 동작 확인 — 막히는 게 의도된 변화임을 인지하고 기록만 남김(별도 조치는 owner 도메인 범위). **실측 결과 401이 아니라 403이다** — [`auth.md`](../domains/auth.md) §3 참고
- 약관 동의 단위 테스트: 필수 3종 동의 저장, 하나라도 빠지면 `REQUIRED_AGREEMENT_NOT_COMPLETED`, 중복 동의 미저장, 미존재 회원 `NOT_FOUND_USER`, 상태 조회 true/false

**2026-08-31 검증 결과** (epic 머지 + §방향 논의 8·9 반영 후):

| 항목 | 결과 |
|---|---|
| `./gradlew build` (ktlint·ArchUnit 포함) | BUILD SUCCESSFUL |
| 단위 테스트 | 51개 통과, 실패 0 |
| `node scripts/docs-check.mjs` | 통과 |

**로컬 기동 검증 (빈 DB `knockdog_verify`, MySQL 8.0 + Redis 컨테이너):**

| 항목 | 결과 |
|---|---|
| Flyway V1·V2·V3 적용 | `Successfully applied 3 migrations ... now at version v3` |
| 앱 기동 (`JPA_DDL_AUTO=validate`) | `Started KnockdogApplicationKt` |
| `users.nickname` nullable | `IS_NULLABLE=YES` (§방향 논의 8 반영 확인) |
| `user_agreements` 생성 | 4개 컬럼 + `(user_id, term_type)` unique |
| 약관 동의 2개 인증 요구 | 토큰 없이 호출 시 403 |
| permit 목록 통과 | `/api/v1/auth/login`, `/api/v1/users`가 403이 아님 |
| 필수 쿠키 누락 | 400 `INVALID_INPUT_VALUE` (아래 §발견 3 수정 후) |

### 해소된 질문 — KEEP parity 공통 응답 래퍼 차이 (2026-08-31 프론트 확인)

약관 동의 2개는 `KEEP` 판정이라 [`api-migration.md`](../rules/api-migration.md) §2가 "응답 필드명·타입·null 처리 유지"를 요구한다. 레거시 저장소의 `common/response/Response.java`와 대조한 결과, `data` 안쪽 DTO는 같고 **그걸 감싸는 공통 응답 래퍼가 다르다.**

| | 레거시 v0 | 신규 |
|---|---|---|
| 필드 순서·구성 | `data, status, code, message, responseTime` | `status, code, message, data` |
| 성공 시 `code` | `"SUCCESS"` | `null` |
| `responseTime` | 있음 (`LocalDateTime`) | **없음** |
| null 필드 | `@JsonInclude(NON_NULL)`으로 생략 | 그대로 내려감 |

즉 `GET /api/v0/user/agreements/status`의 응답이 레거시는 `{"data":{"hasAgreedRequiredTerms":true},"status":200,"code":"SUCCESS","message":"정상 처리되었습니다.","responseTime":"..."}`인데 신규는 `{"status":200,"code":null,"message":"정상 처리되었습니다.","data":{...}}`가 된다. `data` 안쪽(`hasAgreedRequiredTerms`)은 일치한다.

프론트 저장소(`daeng_v2_front` `develop@0abacd7`)를 확인한 결과 **차이를 허용해도 안전하다**.

- 프론트의 `ApiResponse` 타입(`shared/api/model/response.ts`)은 `{status, code, message, data}`뿐이다. `responseTime`은 저장소 전체에서 참조가 0건이다.
- 약관 동의 소비처(`entities/user/api/useUserAgreementQuery.ts`, `features/required-terms-consent/`, `views/guardian-invite/privacy-consent/`)는 `data.hasAgreedRequiredTerms`만 읽는다.
- `useUserAgreementQuery.ts:40`에 `code: 'SUCCESS'`가 있지만 이건 서버 응답을 읽는 게 아니라 mutation 성공 후 **react-query 캐시에 넣는 로컬 객체**다.

따라서 (c)를 택한다 — 공통 `Response<T>`를 그대로 두고 차이를 허용한다. 단 이건 **이 두 endpoint에 한정된 판단**이다. 다른 `KEEP` API를 이관할 때는 그 API의 프론트 소비처를 각각 확인해야 한다. `POST /api/v0/address/search` 계열처럼 `code !== 'SUCCESS'`로 분기하는 곳이 실제로 있다(`features/address-picker/api/searchAddress.ts`).

### 보류한 것 — Flyway↔엔티티 정합성 자동 검증 (2026-09-01)

테스트 환경이 H2 + `ddl-auto: create-drop` + `flyway.enabled: false`라 **`V1`·`V2` SQL이 테스트에서 한 번도 실행되지 않는다.** 반면 로컬·운영은 `validate`라 마이그레이션과 엔티티가 어긋나면 기동 자체가 실패한다. 이번에 그 간극이 실제로 두 번 드러났고(아래 §발견 1·2), 둘 다 손으로 앱을 띄워서야 잡았다.

Testcontainers로 테스트를 실제 MySQL + Flyway 위에서 돌리면 `./gradlew build`가 이런 걸 자동으로 잡는다. **이번 티켓에서는 넣지 않기로 했다** — 마이그레이션이 2개뿐이고 로컬 기동으로 직접 확인했으며, Docker 의존이 생겨 CI·로컬 테스트가 함께 무거워진다.

다만 도메인이 늘어 마이그레이션이 쌓이면 "테스트는 통과하는데 배포하면 안 뜬다"가 반복된다. **다음 도메인 이관 착수 시 함께 도입을 검토한다.**

### 성공 응답 `code`를 `"SUCCESS"`로 (2026-09-01)

KD3-257은 `code`를 "성공 시 생략 가능"으로 뒀는데, 레거시는 성공에도 `code: "SUCCESS"`를 내리고 프론트에 그 값으로 분기하는 곳이 있다(`searchAddress.ts`의 `code !== 'SUCCESS'`). `v0` 계약 유지 대상이라 채우는 쪽으로 바꿨다.

`code`는 성공/실패 2종이 아니다. 실패는 `ErrorCode` 18개(공통 4 + auth 14)이고, **성공에도 결과를 구분하는 코드가 온다** — 레거시 `Response.success(data, code, message)`가 이메일 인증 결과 등에 쓴다. 그래서 `success()`에 `code` 파라미터를 열어뒀다(기본값 `"SUCCESS"`).

남은 차이: 레거시는 `@JsonInclude(NON_NULL)`로 null 필드 키를 생략하는데 신규는 `"data": null`을 내려보낸다. 프론트 `ApiResponse` 타입은 `data: T`(non-optional)라 타입상으로는 신규 쪽이 오히려 맞고, 현재 이 차이로 깨지는 곳은 확인되지 않았다.

### `v1` 유지 근거 재검토 (2026-09-01)

[`api-migration.md`](../rules/api-migration.md) §2가 "경로가 같으면 `v1` 트윈을 만들지 않는다"를 규칙으로 정했다. 이 티켓의 `v1` 5개를 그 기준으로 다시 봤다.

| v1 | v0 대비 경로 | v0 대비 응답 | 판단 |
|---|---|---|---|
| `POST /api/v1/auth/oidc-verifications` | 재설계됨 | | `v1` 유지 |
| `POST /api/v1/users` | 재설계됨 (`user/register`) | | `v1` 유지 |
| `POST /api/v1/auth/login` | **동일** | `addresses[]`에서 `id`·`addressDetail` 누락 | **확인 필요 — 아래** |
| `POST /api/v1/auth/refresh` | **동일** | `code` 수정 후 **완전히 동일** | **`v1` 근거 없음** |
| `POST /api/v1/auth/logout` | **동일** | `code` 수정 후 **완전히 동일** | **`v1` 근거 없음** |

`login`의 응답 차이는 의도한 재설계가 아니라 **누락으로 보인다.** 레거시는 `UserAddress` **JPA 엔티티를 그대로 직렬화**해서 `id`·`addressDetail`이 나가는데, 신규 `UserAddressResponse`는 6개 필드만 담는다. 프론트는 둘 다 쓴다 —

- `MypageProfileLocationPage.tsx:69`가 `String(address.id)`로 주소 id를 뽑아 수정·삭제 요청(`operation: UPDATE|DELETE`)에 넣는다. `id`가 없으면 `"undefined"`가 되어 **주소 수정·삭제가 깨진다.**
- `LocationField.tsx:115`가 `address.detail || address.addressDetail`을 표시한다. 없으면 상세주소가 화면에서 사라진다.

프론트가 아직 `v0`를 호출하므로 지금 깨지는 것은 없다. `v1`으로 전환하는 시점에 터진다.

**결정 필요**: (a) `UserAddressResponse`에 `id`·`addressDetail`을 추가해 `v0`와 맞추고 `login`도 `v1` 근거를 잃게 할지, (b) 의도적 축소로 두고 프론트 전환 시 함께 고칠지. `refresh`/`logout`은 (a)와 무관하게 `v1` 근거가 없어 `v0` 단독으로 내릴지 결정해야 한다.

### 로컬 기동에서 발견한 문제

1. **기존 로컬 DB는 Flyway checksum 불일치로 기동 실패한다.** §방향 논의 8에서 V1을 직접 수정했기 때문이다. 실제로 재현했다 — `Migration checksum mismatch for migration version 1 / Applied to database: 1207543218 / Resolved locally: -1900621767`. 로컬 DB를 초기화해야 한다(운영/스테이징은 아직 이 스키마가 적용된 적이 없어 영향 없음).

2. **빈 DB에서는 앱이 아예 뜨지 못했다** — `Schema-validation: missing table [bookmark]`. `owner`/`bookmark` 예제 슬라이스 테이블이 `ddl-auto: update`로만 만들어져 있었고 마이그레이션이 없었다. 기존 로컬 DB에는 테이블이 이미 있어 드러나지 않던 문제로, KD3-258이 `validate`로 전환하면서 **새로 받는 사람은 앱을 띄울 수 없는 상태**였다. `V3__create_example_slice_tables.sql`로 해결했다.

3. **인증 실패가 403 + Spring 기본 본문으로 나갔다.** 프론트 `tokenRefreshInterceptor`는 `status === 401`일 때만 토큰 갱신에 진입하고 본문 `code`로 갱신/로그아웃을 분기하는데, 신규 서버는 Spring Security 기본값대로 403을 냈다. **액세스 토큰이 만료되면 자동 갱신이 아예 동작하지 않아 모든 인증 요청이 조용히 실패하는 상태**였다. `AuthenticationFailureResponder`로 401 + `Response` 규격 + 정확한 `code`를 내도록 고쳤다(로컬 실측: 토큰 없음 `UNAUTHORIZED_REQUEST`, 만료 `EXPIRED_TOKEN`, 무효 `INVALID_TOKEN` 모두 401).

4. **필수 쿠키 누락이 500으로 나갔다.** `@CookieValue`가 던지는 `MissingRequestCookieException`을 `GlobalExceptionHandler`가 처리하지 않아 `Exception` 핸들러로 떨어졌다. 쿠키로 토큰을 받는 3개 API(`/api/v1/auth/login`, `/refresh`, `POST /api/v1/users`)가 전부 해당된다. 클라이언트 실수를 서버 오류로 보고하던 것이라 400 `INVALID_INPUT_VALUE`로 고치고 회귀 테스트를 추가했다.

아직 확인하지 못한 것: 실제 OIDC 토큰이 필요한 정상 경로(로그인→약관 동의 200)는 provider 인증이 필요해 검증하지 못했다.

## 작업 후 확인 목록

| 문서 | 결과 | 근거 |
|---|---|---|
| [`docs/domains/auth.md`](../domains/auth.md) | 갱신 | 신규 스키마/엔드포인트/유스케이스 분해로 갱신. 2026-08-31에 약관 동의 2개와 `AddressType` 변경 추가 반영 |
| [`docs/conventions/jpa-entity.md`](../conventions/jpa-entity.md) | 갱신 | BaseEntity 공통 컬럼, status 컬럼 사용 기준, FK 미적용 + `NO_CONSTRAINT` 정책 기록 |
| [`docs/conventions/error-handling.md`](../conventions/error-handling.md) | 갱신 | 도메인 전용 `ErrorCode` 첫 도입 사례로 `AuthErrorCode` 예시 추가 |
| `docs/conventions/infra.md` | 해당 없음 | epic의 docs 재편(KD3-242)에서 삭제됐고 내용이 [`docs/inventory/operations.md`](../inventory/operations.md)로 재배치됐다. Flyway 전환·Redis 도입 사실은 아래 인벤토리 항목으로 넘긴다 |
| `docs/specs/2026-07-30-auth-daycare-schema-draft.md` | 해당 없음 | `docs/specs/` 폴더가 KD3-242 재편에서 사라졌다. 이 문서를 참조하던 서술은 §방향 논의 1에 근거가 남아 있어 별도 조치가 필요 없다 |
| [`docs/inventory/api.md`](../inventory/api.md), [`docs/inventory/database.md`](../inventory/database.md), [`docs/inventory/integrations.md`](../inventory/integrations.md), [`docs/inventory/operations.md`](../inventory/operations.md) | 갱신 | KD3-402가 epic에 머지된 뒤(2026-08-31) 반영했다. 아래 목록 참고 |
| [`docs/rules/notion-api-spec-sync.md`](../rules/notion-api-spec-sync.md) | 갱신 | 명세서 `이름` 속성 규칙을 정정하고 "Cookie" 섹션(§3)을 신설했다. auth 5개 페이지를 만들면서 쿠키 값을 HTTP Header 표에 잘못 적은 걸 발견해 팀 규칙 자체를 고친 것이다 |
| `docs/rules/notion-api-page-template.json` | 갱신 | 위 규칙 변경에 맞춰 Cookie 섹션 블록을 템플릿에 추가 |
| [`docs/rules/api-migration.md`](../rules/api-migration.md) | 갱신 | §4가 요구하던 golden/parity 테스트에 실행 수단이 없었다. 자동 테스트가 없다는 사실을 명시하고 로컬 대조 절차를 가리키게 했다 |
| [`docs/rules/documentation.md`](../rules/documentation.md) | 갱신 | 인벤토리 `이관 진척` 열을 사실상 금지하던 "확인 상태 열 일괄 추가 금지" 규칙을 갱신. `완료`를 근거 있을 때만 적도록 제약은 유지 |
| [`docs/workflows/003-migration.md`](../workflows/003-migration.md) | 갱신 | 4단계에 "`KEEP` API 로컬 응답 대조" 절차 신설 |
| [`docs/architecture/hexagonal.md`](../architecture/hexagonal.md) | 갱신 | 실용형을 선택지에서 제거, 기준 예제를 `owner` → `auth`로 교체 |
| `README.md` | 갱신 | 예제 슬라이스 절 교체, 로컬 실행에 Redis·`JWT_SECRET_KEY`·Flyway 안내 추가 |
| `.github/workflows/build.yml` | 신규 | PR에서 `./gradlew build` 실행 |
| Notion API 명세서(외부) | 미처리 | v1 5개는 생성했고 약관 동의 2개가 미반영이다 ([`notion-api-spec-sync.md`](../rules/notion-api-spec-sync.md) 절차) |

### 인벤토리 반영 내역 (2026-08-31, KD3-402 머지 후)

KD3-402가 epic에 들어간 뒤 아래를 반영했다.

- `operations.md` `스키마 관리` — 신규 서버는 KD3-258부터 Flyway가 단일 출처다. `FLYWAY_ENABLED=true` + `JPA_DDL_AUTO=validate`가 기본값이고 `db/migration/V1`, `V2`가 존재한다. "migration 파일이 없고 `FLYWAY_ENABLED=false`"는 더 이상 사실이 아니다.
- `operations.md` `로컬 실행` — `docker-compose.local.yaml`이 MySQL(기본 3308) + Redis(기본 6380)를 제공한다.
- `integrations.md` Redis — 신규 서버가 리프레시 토큰 저장 용도로 Redis를 도입했다(`@RedisHash`, TTL 30일, 레거시와 별개 인스턴스). "신규 서버 미도입"은 더 이상 사실이 아니다.
- `api.md` — 약관 동의 2행에 v0 구현 완료를 표시하고, auth `v0+v1` 5행(`verify/oidc`, `login`, `logout`, `refresh`, `user/register`)에 v1 구현 완료를 표시했다.
- `database.md` `user_agreement` 행 — 신규 서버에서 `user_agreements`로 확정. `(user_id, term_type)` unique, append-only.
