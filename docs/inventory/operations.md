> 생성: 2026-08-02 13:45 · 최종 수정: 2026-08-30 23:24

# 운영 인벤토리

이 문서는 현재 확인된 운영·실행 구성과 신규 서버 전환에 필요한 운영 환경, 배포, 관찰, rollback, 보안 설정 항목을 관리한다.

## 1. 판정 기준

| 판정 | 의미 |
|---|---|
| `KEEP` | 기존 운영 방식을 유지할 항목 |
| `REDESIGN` | 신규 서버 전환에 맞춰 재설계할 항목 |
| `DROP` | 신규 운영 환경에서 제거할 항목 |
| `DEFER` | 현재 구성을 더 확인해야 하는 항목 |

## 2. 작성 규칙

- EC2, RDS/MySQL, Redis, 배포 스크립트, 환경변수, secret, 로그, 알람, 백업을 분리해 기록한다.
- Redis, S3, OIDC처럼 외부 연동 인벤토리와 이름이 겹치는 항목은 운영 제공 방식만 기록한다. 애플리케이션 사용처, 인증/권한 계약, key/TTL/fallback은 [`docs/inventory/integrations.md`](integrations.md)에 둔다.
- 운영 credential, secret 값은 문서에 직접 적지 않는다.
- 신규 AWS 리소스는 수동 생성 여부와 IaC 관리 필요성을 함께 기록한다.
- 컷오버/rollback 가능 여부가 불확실한 항목은 `DEFER`로 둔다.

## 3. 인벤토리

| 항목 | 현재 구성 | 신규 필요 여부 | 판정 | 위험 | 후속 확인 |
|---|---|---|---|---|---|
| 로컬 실행 | `docker-compose.local.yaml`은 로컬 MySQL 컨테이너만 제공하고 애플리케이션은 호스트에서 실행 | 유지 | `KEEP` | 컨테이너 기반 배포 구성으로 오인할 수 있음 | 실제 실행 명령과 환경변수는 루트 `README.md`를 따른다 |
| 스키마 관리 | `flyway-core`·`flyway-mysql` 의존성은 있으나 migration 파일이 없고, local 기본값은 `FLYWAY_ENABLED=false`, `JPA_DDL_AUTO=update` | 필요 | `REDESIGN` | 스키마 변경 이력이 남지 않고 환경 간 차이가 생길 수 있음 | 신규 테이블 도입 작업에서 Flyway migration을 추가하고 `FLYWAY_ENABLED=true`·`JPA_DDL_AUTO=validate` 전환을 별도 결정 |
| 애플리케이션 배포 | 저장소에 Dockerfile, 이미지 빌드, 배포 파이프라인 없음 | 필요 | `DEFER` | 제공 방식, 배포, 관찰성, rollback 경로가 없음 | 배포 방식과 책임 범위를 확정 |
| EC2 | 레거시 서버 배포 환경 확인 필요 | 필요 | `DEFER` | 서버 전환/rollback 경로 불명확 | 인스턴스, 배포 방식, 보안 그룹 확인 |
| MySQL 스키마 전략 | 레거시 DB와 신규 DB 분리 예정 | 필요 | `REDESIGN` | 데이터 이관/검증/rollback 필요 | 신규 스키마 확정 범위와 검증 기준 확인 |
| MySQL 운영 구성 | 확인 필요 | 필요 | `DEFER` | 백업/복구/접근 제어 기준 불명확 | RDS 여부, 백업, snapshot, 접근 제어 정책 확인 |
| Redis | 레거시 사용 중, 신규 서버 미도입 | 필요 후보 | `DEFER` | 세션성 데이터 삭제 위험 | 인스턴스 구성, 네트워크 접근, 모니터링, 백업, 전환/rollback 확인 |
| 로그/메트릭 | 확인 필요 | 필요 | `DEFER` | 전환 중 장애 감지 지연 | CloudWatch/Actuator/알람 기준 확인 |
| secret 관리 | 확인 필요 | 필요 | `DEFER` | 환경변수/키 노출 위험 | Parameter Store 또는 Secrets Manager, 외부 API key 주입 방식 확인 |
