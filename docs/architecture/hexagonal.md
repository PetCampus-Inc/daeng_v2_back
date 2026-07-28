> 생성: 2026-07-28 16:30 · 최종 수정: 2026-07-28 16:54

# 헥사고날 아키텍처 구조

이 서버의 모든 도메인(`domain/<도메인>/`)은 헥사고날(포트-어댑터) 구조를 따른다. 경계는 설명이 아니라 [`HexagonalArchitectureTest.kt`](../../src/test/kotlin/com/petcampus/knockdog/HexagonalArchitectureTest.kt)의 ArchUnit 규칙으로 강제되며, 규칙을 어기면 빌드가 실패한다. 왜 이 구조를 택했는지(정석형 통일 이유 등)는 [`0003`](../adr/0003-헥사고날-정석형-통일.md)을 참고한다.

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

## 2. 정석형 vs 실용형

같은 헥사고날 구조 안에서도 `domain/` 패키지를 두 가지 방식으로 쓸 수 있다. 어느 쪽을 쓸지는 도메인 복잡도에 따라 판단하되, **한 도메인 안에서는 섞지 않는다.**

| | 정석형 (예: `owner`) | 실용형 (예: `bookmark`) |
|---|---|---|
| `domain/` | 순수 모델 + VO, JPA 어노테이션 없음 | JPA 엔티티(`@Entity`)를 그대로 도메인 모델로 사용 |
| `adapter/outbound/persistence/` | JPA 엔티티 + Repository + Mapper + PersistenceAdapter | Repository + PersistenceAdapter (Mapper 없음, 엔티티가 `domain/`에 있으므로) |
| `application/port/output/` | 유스케이스별 포트 분리(`LoadOwnerPort`, `SaveOwnerPort`) | 단일 포트로 묶음(`BookmarkPort`) |
| ArchUnit 순수성 강제 | O — `domain.owner.domain` 패키지는 Spring/JPA 의존 금지 (규칙 4) | X — 규칙 4는 `owner.domain`에만 적용, `bookmark.domain`은 대상 아님 |

**새 도메인을 만들 때 기본값은 정석형이다.** 실용형은 도메인 모델과 영속성 모델이 사실상 1:1이고 별도 불변식이 거의 없는 단순 CRUD 도메인에 한해 예외적으로 허용한다 (`bookmark`가 그 예). 판단이 애매하면 정석형으로 시작한다 — 나중에 실용형으로 단순화하는 것보다, 실용형으로 시작한 걸 정석형으로 쪼개는 비용이 훨씬 크다.

파일 단위 템플릿(복붙 가능한 실제 코드)은 `docs/architecture/slice-template.md`에 별도로 정리한다(아직 작성 전).

## 3. ArchUnit 규칙 (4원칙)

[`HexagonalArchitectureTest.kt`](../../src/test/kotlin/com/petcampus/knockdog/HexagonalArchitectureTest.kt)에 정의되어 있고, 전 도메인에 공통 적용된다(4번만 `owner`에 한정).

| # | 규칙 | 대상 |
|---|---|---|
| 1 | `application` → `adapter` 의존 금지 | 전 도메인 |
| 2 | `application` → `jakarta.persistence` 의존 금지 | 전 도메인 |
| 3 | `domain` → `application`/`adapter` 의존 금지 | 전 도메인 |
| 4 | 순수 도메인(`domain.owner.domain`) → `org.springframework.*`/`jakarta.persistence.*` 의존 금지 | 정석형 도메인만 (현재 `owner`) |

새 도메인을 정석형으로 만들 때는 규칙 4의 대상 패키지 목록(`resideInAnyPackage(...)`)에 그 도메인의 `domain` 패키지를 추가해야 실제로 강제된다 — 추가하지 않으면 정석형으로 작성해도 위반이 빌드를 막아주지 않는다.

## 4. 참고

- 설계 근거: [`0003`](../adr/0003-헥사고날-정석형-통일.md)(헥사고날 정석형 통일), [`0004`](../adr/0004-api-v0-유지-v1-신규.md) 구현 메모(작업 단위 분해)
- 코드 예시: `src/main/kotlin/com/petcampus/knockdog/domain/owner/`(정석형), `.../domain/bookmark/`(실용형)
- 경계 테스트: `src/test/kotlin/com/petcampus/knockdog/HexagonalArchitectureTest.kt`
