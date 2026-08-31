> 생성: 2026-07-28 16:30 · 최종 수정: 2026-08-31 23:10

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

[`HexagonalArchitectureTest.kt`](../../src/test/kotlin/com/petcampus/knockdog/HexagonalArchitectureTest.kt)에 정의되어 있고, 1~3번은 전 도메인에 공통 적용된다. 4번만 대상 패키지를 명시적으로 등록해야 한다.

| # | 규칙 | 대상 |
|---|---|---|
| 1 | `application` → `adapter` 의존 금지 | 전 도메인 |
| 2 | `application` → `jakarta.persistence` 의존 금지 | 전 도메인 |
| 3 | `domain` → `application`/`adapter` 의존 금지 | 전 도메인 |
| 4 | 순수 도메인(`domain.<도메인>.domain`) → `org.springframework.*`/`jakarta.persistence.*` 의존 금지 | 현재 `auth`만 등록됨 — 새 도메인 추가 시 그 도메인도 등록해야 함(아래 참고) |

새 도메인을 정석형으로 만들 때는 규칙 4의 대상 패키지 목록(`resideInAnyPackage(...)`)에 그 도메인의 `domain` 패키지를 추가해야 실제로 강제된다 — 추가하지 않으면 정석형으로 작성해도 위반이 빌드를 막아주지 않는다.

## 4. 참고

- 설계 근거: [`0003`](../adr/0003-헥사고날-정석형-통일.md)(헥사고날 정석형 통일), [`0004`](../adr/0004-api-v0-유지-v1-신규.md) 구현 메모(작업 단위 분해)
- 코드 예시: `src/main/kotlin/com/petcampus/knockdog/domain/auth/` (KD3-258). 초기 세팅의 예제 슬라이스(`owner`, `bookmark`)는 auth가 실제 구현으로 대체해 KD3-258에서 삭제했다
- 경계 테스트: `src/test/kotlin/com/petcampus/knockdog/HexagonalArchitectureTest.kt`
