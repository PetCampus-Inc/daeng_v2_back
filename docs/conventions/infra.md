> 생성: 2026-07-28 17:30 · 최종 수정: 2026-07-28 17:30

# 로컬 실행 환경

로컬에서 이 서버를 띄우고 DB 스키마를 바꿀 때 실제로 쓰는 방식을 정리한다. "지켜야 할 규칙"이 아니라 지금 이렇게 되어 있다는 사실 기록이므로, 방식이 바뀌면 이 문서를 그 자리에서 덮어쓴다.

## 1. 환경변수 (`.env`)

`.env.example`이 템플릿이고, 실제 값은 `.env.local`(gitignore 대상, 커밋되지 않음)에 채운다.

```bash
cp .env.example .env.local
# .env.local 값을 채운 뒤
docker compose --env-file .env.local -f docker-compose.local.yaml up -d   # MySQL 기동
set -a && source .env.local && set +a && ./gradlew bootRun --args='--spring.profiles.active=local'  # 앱 실행
```

값을 비워두면 각 설정 파일(`docker-compose.local.yaml`, `application-local.yaml`)에 정의된 기본값이 쓰인다(예: `DB_PORT` 비우면 `3308`).

| 변수 | 용도 |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_TIMEZONE` | 앱과 docker compose가 공용으로 참조하는 DB 접속 정보 |
| `DB_ROOT_PASSWORD`, `DB_IMAGE`, `DB_CONTAINER_NAME`, `DB_VOLUME_NAME` | docker compose 전용(MySQL 컨테이너 구성) |
| `SERVER_PORT` | 앱 포트 |
| `JPA_DDL_AUTO` | Hibernate `ddl-auto` 값. 로컬 기본값은 `update` (§2 참고) |
| `JPA_SHOW_SQL` | SQL 로깅 여부 |
| `FLYWAY_ENABLED` | Flyway 마이그레이션 실행 여부. 로컬 기본값은 `false` (§2 참고) |

새 환경변수가 필요하면 `.env.example`에도 같이 추가한다 — `.env.example`에 없는 변수를 코드에서만 참조하면 다른 사람/AI가 로컬 세팅을 재현할 수 없다.

## 2. DB 스키마 — Flyway

`org.flywaydb:flyway-core` / `flyway-mysql` 의존성은 이미 추가되어 있지만, **로컬 기본 설정은 아직 Flyway가 아니라 Hibernate `ddl-auto: update`로 스키마를 맞추는 상태다** (`application-local.yaml`, `FLYWAY_ENABLED=false`가 기본값). `src/main/resources/db/migration/`에 마이그레이션 파일이 아직 없다.

[`0010-신규-db-인스턴스-스키마-재작성.md`](../adr/0010-신규-db-인스턴스-스키마-재작성.md) 결정에 따라 신규 DB 인스턴스에 스키마를 새로 쓰기로 했으므로, 앞으로 테이블을 만들 때는:

- `ddl-auto: update`로 임시로 스키마를 맞추지 말고, `src/main/resources/db/migration/V<N>__<설명>.sql` 형식의 Flyway 마이그레이션 파일로 스키마 변경을 남긴다.
- 마이그레이션 파일이 쌓이기 시작하면 `FLYWAY_ENABLED=true`로 전환하고 `JPA_DDL_AUTO`는 `validate`로 낮춰, 스키마 변경의 단일 출처가 마이그레이션 파일이 되도록 한다 (이 전환 자체가 확정되면 이 섹션을 갱신한다).

## 3. Docker

`docker-compose.local.yaml`은 **로컬 MySQL 컨테이너 하나만** 띄운다 — 앱 자체를 컨테이너로 실행하는 용도가 아니다. 앱은 `./gradlew bootRun`으로 호스트에서 직접 실행한다.

```bash
docker compose --env-file .env.local -f docker-compose.local.yaml up -d    # 기동
docker compose -f docker-compose.local.yaml down                          # 종료 (볼륨 유지)
```

DB 데이터는 named volume(`DB_VOLUME_NAME`, 기본 `knockdog_mysql_data`)에 남는다 — 컨테이너를 내렸다 올려도 데이터가 유지된다. 완전히 초기화하려면 `down -v`로 볼륨까지 지운다.

## 4. 참고

- 템플릿: `.env.example`
- compose 정의: `docker-compose.local.yaml`
- 로컬 프로필 설정: `src/main/resources/application-local.yaml`
- 관련 결정: [`0010`](../adr/0010-신규-db-인스턴스-스키마-재작성.md)
