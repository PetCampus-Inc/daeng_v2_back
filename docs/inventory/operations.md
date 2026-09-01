> 생성: 2026-08-02 13:45 · 최종 수정: 2026-09-01 00:20

# 운영 인벤토리

이 문서는 현재 확인된 운영·실행 구성과 신규 서버 전환에 필요한 운영 환경, 배포, 관찰, rollback, 보안 설정 항목을 관리한다.

## 1. 판정 기준

| 판정 | 의미 |
|---|---|
| `KEEP` | 기존 운영 방식을 유지할 항목 |
| `REDESIGN` | 신규 서버 전환에 맞춰 재설계할 항목 |
| `DROP` | 신규 운영 환경에서 제거할 항목 |
| `DEFER` | 현재 구성을 더 확인해야 하는 항목 |

## 2. 이관 진척

`판정`이 "무엇을 할 것인가"라면 `이관 진척`은 "어디까지 했는가"다. 둘을 한 열에 섞지 않는다.

| 값 | 의미 |
|---|---|
| `완료` | 신규 서버에 반영되고 검증까지 끝난 항목 |
| `진행중` | 일부만 반영됐거나 검증이 남은 항목 |
| `미착수` | 아직 손대지 않은 항목. `DEFER`도 판정이 확정되면 대상이 될 수 있으므로 여기 둔다 |
| `해당없음` | `DROP` 판정이라 신규 운영 환경으로 가져가지 않는 항목 |

`완료`는 실제 코드와 검증 결과가 있을 때만 적는다. 어느 작업에서 처리했는지는 그 행에 `work/` 링크로 남긴다.

## 3. 작성 규칙

- EC2, RDS/MySQL, Redis, 배포 스크립트, 환경변수, secret, 로그, 알람, 백업을 분리해 기록한다.
- Redis, S3, OIDC처럼 외부 연동 인벤토리와 이름이 겹치는 항목은 운영 제공 방식만 기록한다. 애플리케이션 사용처, 인증/권한 계약, key/TTL/fallback은 [`docs/inventory/integrations.md`](integrations.md)에 둔다.
- 운영 credential, secret 값은 문서에 직접 적지 않는다.
- 신규 AWS 리소스는 수동 생성 여부와 IaC 관리 필요성을 함께 기록한다.
- 컷오버/rollback 가능 여부가 불확실한 항목은 `DEFER`로 둔다.
- 레거시는 계속 변경되므로, 재추출할 때마다 4절에 기준 커밋 SHA와 날짜를 갱신한다.

## 4. 추출 범위

신규 서버는 이 저장소, 레거시 운영 구성은 `PetCampus-Inc/daeng_v1_back` `dev@2479b02c` (2026-08-30)의 `.github/workflows/`, `docker-compose.yml`, `Dockerfile`, `appspec.yml`, `scripts/`를 기준으로 확인했다. 여기 적힌 레거시 구성은 저장소에 커밋된 내용이며, 운영 인스턴스의 실제 상태를 직접 확인한 것은 아니다.

## 5. 인벤토리

| 항목 | 현재 구성 | 신규 필요 여부 | 판정 | 이관 진척 | 위험 | 후속 확인 |
|---|---|---|---|---|---|---|
| 로컬 실행 | `docker-compose.local.yaml`은 로컬 MySQL(호스트 포트 기본 3308)과 Redis(기본 6380) 컨테이너를 제공하고 애플리케이션은 호스트에서 실행 | 유지 | `KEEP` | `완료` | 컨테이너 기반 배포 구성으로 오인할 수 있음 | 실제 실행 명령과 환경변수는 루트 `README.md`를 따른다 |
| 스키마 관리 | 신규 서버: [`KD3-258`](../work/KD3-258-user-social-auth.md)부터 Flyway가 스키마 변경의 단일 출처다. local 기본값이 `FLYWAY_ENABLED=true`·`JPA_DDL_AUTO=validate`로 전환됐고 `db/migration/`에 `V1`(auth 테이블), `V2`(user_agreements)가 있다. 레거시: Flyway 없이 `scripts/migrations/*.sql` 22개를 배포 스크립트가 매 배포마다 `docker exec`로 전부 재실행(멱등하게 작성) | 필요 | `REDESIGN` | `완료` | 레거시는 스키마 변경 이력이 남지 않고, SQL 하나라도 비멱등하면 배포가 실패한다. 실행 이력도 어디에도 기록되지 않음 | 신규 서버는 전환 완료. 남은 것은 레거시 SQL 22개를 신규 스키마 재작성([`0010`](../adr/0010-신규-db-인스턴스-스키마-재작성.md))의 입력으로 정리하는 일이다 |
| 애플리케이션 배포 | 신규 서버: Dockerfile, 이미지 빌드, 배포 파이프라인 없음. 레거시: `dev` push → GitHub Actions가 gradle build → ECR `:latest` push → EC2에 SSH로 `docker-compose up -d` | 필요 | `REDESIGN` | `미착수` | 레거시는 `:latest` 단일 태그라 이전 이미지로 되돌릴 수 없고, `down` 후 `up`이라 배포 중 downtime이 있음 | 신규는 커밋 SHA 태그와 롤백 가능한 배포 방식을 전제로 설계. 무중단 컷오버는 [`0008`](../adr/0008-무중단-컷오버-전략.md)과 함께 확정 |
| EC2 | 레거시는 EC2 1대(`ec2-user`)에서 docker-compose로 app·MySQL·Redis를 함께 구동. `appspec.yml`·`scripts/deploy.sh`(CodeDeploy·nohup 방식)가 저장소에 남아 있으나 현재 파이프라인은 docker-compose 경로 | 필요 | `REDESIGN` | `미착수` | 앱과 데이터 저장소가 한 인스턴스에 묶여 있어 인스턴스 장애가 곧 데이터 장애. 배포 경로가 두 갈래로 보여 실제 경로를 오인할 수 있음 | 인스턴스 사양·보안 그룹 확인, CodeDeploy 잔재가 실제로 미사용인지 확인 후 제거, 신규는 앱과 저장소 분리 |
| MySQL 스키마 전략 | 레거시 DB와 신규 DB 분리 예정 | 필요 | `REDESIGN` | `미착수` | 데이터 이관/검증/rollback 필요 | 신규 스키마 확정 범위와 검증 기준 확인 |
| 컨테이너 이미지 저장소 | 레거시는 ECR(`ap-northeast-2`) 단일 리포지토리에 `:latest` 태그만 사용 | 필요 | `REDESIGN` | `미착수` | 배포된 이미지가 어느 커밋인지 추적 불가, 이전 버전 롤백 불가 | 신규는 커밋 SHA 태그 + 이미지 보존 정책 결정 |
| CI | 레거시는 `ci-cd.yml`이 `dev` push마다 `./gradlew clean build` 후 바로 배포. 별도 PR 검증 파이프라인은 `pr-review.yml` | 필요 | `REDESIGN` | `미착수` | 테스트 실패 외의 게이트가 없고, `dev` push가 곧 배포라 되돌릴 지점이 없음 | 신규는 브랜치 전략([`docs/rules/git.md`](../rules/git.md))에 맞춘 검증·배포 분리 |
| MySQL 운영 구성 | RDS가 아니라 EC2 위 `mysql:8.0` 컨테이너. `mysql_data` named volume, 3306 호스트 노출, 계정은 compose 파일에 평문 | 필요 | `REDESIGN` | `미착수` | 자동 백업·PITR·snapshot이 없고, 컨테이너/볼륨 유실이 곧 데이터 유실. 포트 노출 시 접근 제어가 보안 그룹에만 의존 | 신규는 관리형 DB 사용 여부를 먼저 결정. 레거시 데이터 이관 전 백업 존재 여부를 반드시 확인 |
| Redis | EC2 위 `redis:alpine` 컨테이너, `--appendonly yes`, `redis_data` volume, 6379 호스트 노출, 비밀번호는 compose 파일에 평문. 신규 서버 미도입 | 필요 후보 | `REDESIGN` | `미착수` | 캐시와 리프레시 토큰이 같은 인스턴스에 있어 유실 시 로그인까지 끊김. 키/TTL은 [`integrations.md`](integrations.md) 참고 | 캐시/상태 인스턴스 분리 여부, 관리형 사용 여부, 전환 시 토큰 무효화 범위 |
| 로그/메트릭 | 레거시는 `/actuator/health`만 노출(`show-details: never`), logback + Discord appender, GitHub Actions cron이 10분 간격으로 health를 ping 후 Discord 알림. CloudWatch 설정은 주석 처리 | 필요 | `REDESIGN` | `미착수` | 메트릭·요청 로그 수집처가 없어 health 200 외에는 장애를 알 수 없음. 감지 지연이 최대 10분 | 로그 수집처, 메트릭 노출 범위, 알람 조건을 신규 배포 방식과 함께 결정 |
| secret 관리 | 레거시는 일부만 GitHub Actions secrets로 주입하고, JWT 서명키·DB 계정·메일 계정·외부 API key는 `application.yml`과 `docker-compose.yml`에 평문으로 커밋돼 있음 | 필요 | `REDESIGN` | `미착수` | 저장소 접근 권한이 곧 운영 credential 접근 권한. 이력에 남아 있어 파일 수정만으로는 해소되지 않음 | 신규는 Parameter Store 또는 Secrets Manager로 주입. 레거시 노출 키는 문서 정리와 별개로 로테이션이 필요하며, 대상과 담당을 별도 티켓으로 관리 |
