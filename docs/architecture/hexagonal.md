> 생성: 2026-07-28 16:30 · 최종 수정: 2026-09-08 13:50

# 헥사고날 아키텍처 구조

이 서버의 모든 도메인(`domain/<도메인>/`)은 헥사고날(포트-어댑터) 구조를 따른다. 경계는 설명이 아니라 [`HexagonalArchitectureTest.kt`](../../src/test/kotlin/com/petcampus/knockdog/HexagonalArchitectureTest.kt)의 ArchUnit 규칙으로 강제되며, 규칙을 어기면 빌드가 실패한다. 왜 이 구조를 택했는지는 [`0003`](../adr/0003-헥사고날-정석형-통일.md)을 참고한다.

## 1. 패키지 구조

```
domain/<도메인>/
  domain/                          도메인 모델
  application/
    port/input/<X>UseCase.kt       유스케이스 인터페이스, 유스케이스 1개당 1파일
    port/output/<X>Port.kt         아웃바운드 포트
    service/<X>Service.kt          유스케이스 구현
  adapter/
    inbound/web/<X>Controller.kt   유스케이스별로 컨트롤러도 분리 (병렬 작업 충돌 방지)
    outbound/persistence/          JPA 엔티티 · Repository · PersistenceAdapter · (Mapper)
```

kindergarten처럼 DB가 아니라 **Redis가 주 저장소인 도메인**은 `adapter/outbound/persistence/` 대신 `adapter/outbound/cache/`로 명명하되, 포트 인터페이스는 동일한 패턴(`LoadXPort`/`SaveXPort`)을 따른다.

## 2. 정석형 통일

**모든 도메인은 정석형으로 만든다. 다른 선택지는 없다** ([`0003`](../adr/0003-헥사고날-정석형-통일.md)).

- `domain/` 패키지는 순수 모델 + VO로만 구성하고 JPA 어노테이션을 두지 않는다
- 영속성은 `adapter/outbound/persistence/`에 별도 JPA 엔티티 + Repository + Mapper + PersistenceAdapter로 분리한다
- 아웃바운드 포트는 유스케이스별로 나눈다 (`LoadUserPort`, `SaveUserPort`처럼)

새 도메인은 `domain/auth/`를 기준 예제로 삼는다.

파일 단위 템플릿(복붙 가능한 실제 코드)은 `docs/architecture/slice-template.md`에 별도로 정리한다(아직 작성 전).

## 3. ArchUnit 규칙 (4원칙)

[`HexagonalArchitectureTest.kt`](../../src/test/kotlin/com/petcampus/knockdog/HexagonalArchitectureTest.kt)에 정의되어 있고, 4원칙 모두 전 도메인에 공통 적용된다.

| # | 규칙 | 대상 |
|---|---|---|
| 1 | `application` → `adapter` 의존 금지 | 전 도메인 |
| 2 | `application` → `jakarta.persistence` 의존 금지 | 전 도메인 |
| 3 | `domain` → `application`/`adapter` 의존 금지 | 전 도메인 |
| 4 | 순수 도메인(`domain.<도메인>.domain`) → `org.springframework.*`/`jakarta.persistence.*` 의존 금지 | 전 도메인 — `resideInAnyPackage("com.petcampus.knockdog.domain.*.domain..")` 와일드카드로 모든 도메인에 자동 적용됨 |

새 도메인을 정석형으로 만들 때 별도로 등록할 것은 없다 — 규칙 4는 와일드카드라 `domain/` 패키지를 정석형 위치에 두기만 하면 자동으로 강제된다.

## 4. 후속 과제

- **`userCode` → `userId` 변환이 서비스마다 중복돼 있다(2026-09-08 발견, 미착수)**: `AccessTokenAuthenticationFilter`는 JWT를 검증해 `SecurityContextHolder`에 `userCode`(String) principal만 넣고, 실제 `User` 조회·존재 검증은 각 애플리케이션 서비스가 아래 형태로 직접 한다.
  ```kotlin
  private fun requireUserId(userCode: UserCode): Long {
      val user = loadUserPort.findByCode(userCode) ?: throw BusinessException(AuthErrorCode.NOT_FOUND_USER)
      return requireNotNull(user.id) { "저장되지 않은 User입니다." }.value
  }
  ```
  pet 도메인 서비스 5개(`CreatePetService`·`UpdatePetService`·`SetRepresentativeService`·`DeletePetService`·`GetPetsService`)와 auth의 `UserAgreementController`에 거의 동일한 코드가 중복돼 있다.
  **개선안**: 커스텀 애노테이션(예: `@CurrentUserId`) + `HandlerMethodArgumentResolver`를 `adapter/inbound/web`(또는 공통 web 설정)에 만들어, 컨트롤러가 `@CurrentUserId userId: Long`을 바로 받도록 한다. 리졸버가 `LoadUserPort`(application 포트)를 호출하는 건 헥사고날 의존 방향(adapter → application 허용)에 어긋나지 않고, 리졸버에서 던진 `BusinessException`도 `@RestControllerAdvice`(`GlobalExceptionHandler`)가 정상적으로 잡는다(Spring MVC의 인자 바인딩도 핸들러 호출 예외 처리 범위 안에 있음).
  **왜 아직 안 했는지**: pet 도메인 하나만의 문제가 아니라 auth 쪽 컨트롤러 관례까지 같이 바꾸는 리팩터링이라 특정 티켓에 끼워 넣기보다 별도 작업으로 다루기로 함(사용자 확인, 2026-09-08). 착수 전 확인할 점: `User` 조회 시점이 서비스의 `@Transactional` 시작 전(트랜잭션 밖)으로 이동한다 — 지금도 락 없는 일반 조회라 실질적 위험 증가는 없다고 판단했지만, 착수 시 재확인.

## 5. 참고

- 설계 근거: [`0003`](../adr/0003-헥사고날-정석형-통일.md)(헥사고날 정석형 통일), [`0004`](../adr/0004-api-v0-유지-v1-신규.md) 구현 메모(작업 단위 분해)
- 코드 예시: `src/main/kotlin/com/petcampus/knockdog/domain/auth/` (KD3-258). 초기 세팅의 예제 슬라이스(`owner`, `bookmark`)는 auth가 실제 구현으로 대체해 KD3-258에서 삭제했다
- 경계 테스트: `src/test/kotlin/com/petcampus/knockdog/HexagonalArchitectureTest.kt`
