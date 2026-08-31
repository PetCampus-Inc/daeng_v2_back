> 생성: 2026-08-03

# KD3-258 — User 엔티티 + 소셜 로그인 회원가입 + 인증/인가 기반 구축

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

### 엔드포인트 (v1, 5개)

| 메서드/경로 | 유스케이스 | 인증 요구 |
|---|---|---|
| `POST /api/v1/auth/oidc-verifications` | VerifyOidcUseCase | 공개 |
| `POST /api/v1/auth/login` | LoginUseCase | 공개 (OIDC 임시 토큰으로 자체 검증) |
| `POST /api/v1/auth/refresh` | RefreshTokenUseCase | 공개 (리프레시 토큰으로 자체 검증) |
| `POST /api/v1/auth/logout` | LogoutUseCase | 공개 (리프레시 토큰으로 자체 검증) |
| `POST /api/v1/users` | RegisterUserUseCase | 공개 (OIDC 임시 토큰으로 자체 검증) |

### DB 스키마 (신규 Flyway 마이그레이션)

`knockdog_server` 레포의 `docs/superpowers/specs/2026-08-02-db-schema-redesign-design.md`(별도 레포, 이 저장소에서는 링크 불가)를 기준으로 하되, 이번 브레인스토밍에서 아래와 같이 조정한다.

```sql
CREATE TABLE users (
  id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_code               VARCHAR(8)   NOT NULL UNIQUE,
  nickname                VARCHAR(100) NOT NULL,
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
  type          VARCHAR(20) NOT NULL,           -- HOME, WORK, OTHER
  alias         VARCHAR(20),
  address       VARCHAR(200) NOT NULL,
  road_address  VARCHAR(200),
  lat           DOUBLE NOT NULL,
  lng           DOUBLE NOT NULL,
  created_at    DATETIME(6) NOT NULL,
  updated_at    DATETIME(6) NOT NULL,
  deleted_at    DATETIME(6)
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
- **`user_notification_settings`**: 이번 스키마 문서엔 있지만 어느 화면/유스케이스가 채우는지 확인되지 않았고, 다른 테이블이 FK로 참조하지도 않아 존재가 필수가 아니다. 스키마/코드 모두에서 제외 — 마케팅 동의 관련 티켓이 생기면 그때 추가.
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

## 완료 확인 기준

- ArchUnit 전체 규칙 통과 (`domain.auth.domain` 규칙 4 등록 포함)
- ktlint 통과
- 도메인 단위 테스트: `User`(주소 HOME 불변식, `withdraw`/`isWithdrawn`), `SocialUser`(`link`/`unlink`, 상태 전이)
- `VerifyOidcUseCase` 단위 테스트: LINKED/UNLINKED/PENDING(다른 provider 동일 이메일) 3분기 모두 커버
- `LoginUseCase` 단위 테스트: 정상 로그인, 미연동 소셜계정, 탈퇴 후 7일 이내/이후 분기
- provider별 OIDC 검증기 단위 테스트 (JWKS mock)
- Flyway 마이그레이션이 로컬 MySQL에 정상 적용됨 (`./gradlew flywayMigrate` 또는 앱 기동 시 자동 적용)
- `KnockdogApplicationTests`(컨텍스트 로딩) 통과 — Redis 연결 없이도 컨텍스트가 뜨는지 확인(레이지 커넥션 확인)
- `SecurityFilterChain` 도입 후 기존 `OwnerController` 엔드포인트 동작 확인 — 401로 막히는 게 의도된 변화임을 인지하고 기록만 남김(별도 조치는 owner 도메인 범위)

## 작업 후 확인 목록

- `docs/domains/auth.md` — 레거시 스키마 기준 문서를 이번에 확정된 신규 스키마/엔드포인트/유스케이스 분해로 갱신
- `docs/conventions/jpa-entity.md` (신규) — BaseEntity 공통 컬럼, status 컬럼 사용 기준, FK 미적용 + `NO_CONSTRAINT` 연관관계 매핑 정책 기록
- `docs/conventions/infra.md` §3, §4 — Flyway 전환 완료, Redis 도입(접속 구성·레거시와 공유 여부) 반영
- `docs/specs/2026-07-30-auth-daycare-schema-draft.md` — 이번에 확정된 `knockdog_server`발 스키마로 대체/보류 처리되었음을 표시
- Notion API 명세서 — 신규 5개 엔드포인트 반영 ([`docs/rules/notion-api-spec-sync.md`](../rules/notion-api-spec-sync.md) 절차)
