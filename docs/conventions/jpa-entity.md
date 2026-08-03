> 생성: 2026-08-03

# JPA 엔티티 컨벤션

`auth` 도메인(KD3-258) 구현 과정에서 처음 정한 컨벤션. 이후 모든 도메인의 JPA 엔티티가 따른다.

## 1. BaseEntity — created_at/updated_at/deleted_at 공통화

모든 JPA 엔티티는 [`global/persistence/BaseEntity.kt`](../../src/main/kotlin/com/petcampus/knockdog/global/persistence/BaseEntity.kt)(`@MappedSuperclass`)를 상속해 `created_at`/`updated_at`/`deleted_at`을 공통으로 갖는다. `created_at`/`updated_at`은 Spring Data JPA Auditing(`@CreatedDate`/`@LastModifiedDate`)이 자동으로 채운다 — 애플리케이션이 `@EnableJpaAuditing`(`KnockdogApplication`)을 켜둬야 동작한다.

## 2. status 컬럼은 BaseEntity로 표현 불가능한 상태에만

`deleted_at`(soft-delete)으로 표현 가능한 상태(예: 탈퇴 여부)는 별도 `status` 컬럼을 두지 않는다. `deleted_at IS NOT NULL` 자체가 상태를 의미하므로, 도메인 모델에서 `isWithdrawn`/`withdraw()`처럼 계산된 프로퍼티/메서드로 감싼다(예: `domain/auth/domain/User.kt`).

`deleted_at`으로 표현할 수 없는, soft-delete와 무관한 별도 상태(예: 소셜 계정의 LINKED/UNLINKED/PENDING 3단계)는 그대로 `status` 컬럼을 둔다(예: `domain/auth/domain/SocialUserStatus.kt`).

## 3. FK 제약(REFERENCES)은 걸지 않는다

DB에는 외래키 제약을 걸지 않는다 — 컬럼에는 ID 값만 저장한다. 대량 삭제/파티션/마이그레이션 시 FK가 걸림돌이 되는 걸 피하기 위함이고, 참조 무결성은 소프트 삭제(§2) 정책과 애플리케이션 코드가 책임진다.

연관관계 매핑 자체(객체 그래프 탐색, cascade 등)가 필요하면 `@ManyToOne`/`@JoinColumn`은 그대로 쓰되, `foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)`로 DB 제약 생성만 막는다:

```kotlin
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
val user: UserJpaEntity?
```

Hibernate의 지연 로딩은 컬럼에 저장된 값만으로 동작하므로, 실제 DB에 `REFERENCES` 제약이 있는지 여부와 무관하게 정상 작동한다. 이 프로젝트는 스키마를 Hibernate 자동 생성이 아니라 Flyway로 직접 관리하므로(§4), 실질적인 제약 여부는 마이그레이션 SQL에 `REFERENCES`를 쓰는지에 달려 있다 — `NO_CONSTRAINT`는 향후 `ddl-auto: validate` 전환 시를 위한 보험이다.

같은 애그리게잇 내부의 부모-자식 관계(예: `User`-`UserAddress`, cascade 저장이 필요한 경우)는 이 방식을 그대로 쓴다. 애그리게잇을 넘나드는 느슨한 참조(예: `SocialUser.userId`)는 전체 엔티티 로딩 없이 `EntityManager.getReference()`로 프록시 참조만 만들어 매퍼에 전달한다 — 이 경우 프록시 생성은 순수 매퍼가 아니라 PersistenceAdapter가 담당한다(예: `SocialUserPersistenceAdapter`).

## 4. 참고

- 예시 코드: `domain/auth/adapter/outbound/persistence/`(`UserJpaEntity`, `UserAddressJpaEntity`, `SocialUserJpaEntity`)
- Flyway 전환 배경: [`infra.md`](infra.md) §3
