> 생성: 2026-08-30 20:26 · 최종 수정: 2026-08-30 20:26

# 현재 운영·실행 구성

이 문서는 현재 확인된 실행 환경 구성을 기록한다. 스키마 변경 원칙은 [`../database/policy.md`](../database/policy.md)에, 외부 의존성의 애플리케이션 사용 의미는 [`../integrations/inventory.md`](../integrations/inventory.md)에 둔다.

## 구성 요소

| 구성 요소 | 현재 상태 | 운영상 확인·전환 사항 |
|---|---|---|
| MySQL | 애플리케이션의 주 저장소 | 신규 운영 인스턴스 구성, 백업·복구, 접근 제어는 `inventory.md`에서 확인한다. |
| Docker Compose | `docker-compose.local.yaml`은 로컬 MySQL 컨테이너만 제공 | 애플리케이션은 호스트에서 실행한다. |
| 애플리케이션 컨테이너·배포 파이프라인 | 저장소에 Dockerfile·이미지 빌드·배포 파이프라인 없음 | 도입 시 제공 방식, 배포, 관찰성, rollback을 `inventory.md`에 함께 확정한다. |

## 경계

- Flyway 의존성·migration 현황과 `ddl-auto` 전환은 [`../database/policy.md`](../database/policy.md)의 현재 상태를 따른다.
- Redis의 인증·유치원 데이터 용도, TTL, fallback은 [`../integrations/inventory.md`](../integrations/inventory.md)에서 관리한다. Redis 인스턴스·네트워크·모니터링·전환은 `inventory.md`에서 관리한다.
- 로컬 실행 명령과 환경변수 설정은 온보딩 절차이므로 루트 [`README.md`](../../README.md)에 둔다.
