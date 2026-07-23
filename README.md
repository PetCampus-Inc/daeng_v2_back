# daeng_v3_back (신규 Kotlin Server)

똑독(daeng) 서버를 **Kotlin + 하이브리드 헥사고날 아키텍처**로 재구축하는 신규 리포지토리입니다.
레거시(Java + 3-layered)의 v1 혼재·미사용 API를 걷어내는 것이 목표입니다. (Jira: KD3-194)

> ⚠️ 현재는 **초기 세팅 + 참조용 예제 슬라이스** 단계입니다. 실제 도메인/테이블은 팀 회의 후 확정합니다.

## 기술 스택

- Kotlin 2.3.21 / **JDK 21 (LTS)** / Spring Boot 3.5.16
- Gradle (Kotlin DSL) · Spring Data JPA · Flyway · MySQL
- 코드 스타일: **ktlint** / 아키텍처 검증: **ArchUnit**

## 빌드 / 실행

빌드는 **JDK 21**로 합니다. (SDKMAN 예시)

```bash
# 이 프로젝트 셸에서 JDK 21 사용
sdk use java 21.0.10-tem      # 또는: export JAVA_HOME=~/.sdkman/candidates/java/21.0.10-tem

./gradlew build               # 컴파일 + ktlint 검사 + 테스트
./gradlew ktlintFormat        # 코드 자동 정렬
./gradlew ktlintCheck         # 스타일 검사만
```

### 로컬 실행

환경변수는 `.env.example`을 복사해 `.env.local`에 채웁니다. (`.env.local`은 커밋되지 않습니다)

```bash
cp .env.example .env.local

# 1) 로컬 MySQL 기동 — compose는 기본적으로 .env 를 읽으므로 --env-file 이 필요합니다
docker compose --env-file .env.local -f docker-compose.local.yaml up -d

# 2) 앱 실행 — Spring Boot는 .env 파일을 읽지 않으므로 셸에 export 해야 합니다
set -a && source .env.local && set +a
./gradlew bootRun --args='--spring.profiles.active=local'
```

DB 포트·계정·컨테이너명 등은 모두 환경변수로 조정할 수 있고, 값을 비우면 기본값이 쓰입니다.
IntelliJ에서 실행한다면 EnvFile 플러그인으로 `.env.local`을 지정하는 방법도 있습니다.

## 아키텍처 요약

`domain`(비즈니스) / `global`(공통) 2축. 각 도메인 내부는 헥사고날:

```
domain/<도메인>/
  domain/          순수 도메인 모델 (정석형) 또는 엔티티=도메인 (실용형)
  application/
    port/input/    유스케이스 인터페이스 (입력 포트)
    port/output/   나가는 인터페이스 (출력 포트)
    service/       유스케이스 구현
  adapter/
    inbound/web/         Controller + DTO
    outbound/persistence/ JPA Entity·Repository·PersistenceAdapter (+ 정석형은 Mapper)
```

**핵심 규칙 (ArchUnit이 강제):**
1. 의존 방향은 항상 안쪽으로: `adapter → application → domain`
2. `application`은 포트에만 의존 (JPA/adapter 직접 참조 금지)
3. 순수 도메인(정석형)은 Spring/JPA를 모른다

### 예제 슬라이스 (참조용, 추후 교체)

- **`owner` (정석형)**: 순수 도메인 `Owner` + JPA 엔티티 분리 + 매퍼. `POST /owners`, `GET /owners/{id}`
- **`bookmark` (실용형)**: JPA 엔티티=도메인, 매퍼 없음. `POST /bookmarks`, `GET /bookmarks?ownerId=`
