> 생성: 2026-09-07 12:22 · 최종 수정: 2026-09-07 13:15

# KD3-478 — S3 인프라 기본 설정 및 범용 이미지 업로드 기능 이관

| 항목 | 값 |
|---|---|
| Jira | `KD3-478` (하위 작업) |
| 브랜치 | `feat/KD3-478-s3-infra-image-upload` |
| 상위 에픽 | `KD3-403`(Epic, [리팩토링] daeng_v1_back → daeng_v2_back). 중간 작업 `KD3-477`(Task, s3 마이그레이션) — git 브랜치는 `epic/KD3-477-s3-migration` |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `5`(독립 리뷰·PR·문서 동기화) — 구현·검증 완료, `./gradlew build` green(106 테스트), 문서 동기화 완료. 독립 리뷰(컨텍스트 미공유) 완료 — "no material findings", 작업 문서 대비 누락·범위 초과·계획 불일치 없음. [PR #19](https://github.com/PetCampus-Inc/daeng_v2_back/pull/19) (`feat/KD3-478-s3-infra-image-upload` → `epic/KD3-477-s3-migration`) 생성.
- 다음 결정 또는 전환 조건: 머지 전 남은 사람 몫: ① `S3ObjectStorageAdapter`의 copy/delete/exists 로컬 S3 스모크 대조 ② Notion API 명세 등록. ③ 프론트(`daeng_v2_front`) v1 전환은 별도 작업. ①②가 끝나면 머지 후 Jira `완료`로 전환.
- `epic/KD3-477-s3-migration`은 dev로 합치지 않는다 — KD3-477(s3 마이그레이션)의 도메인별 후속이 남아 있으면 그 위에서 계속 진행. 후속이 없다고 확정되면 epic → dev 일반 merge.

## 작업 목표

레거시(`daeng_v1_back` = `knockdog_server`)의 S3 연동 기본 설정과 범용 이미지 업로드/다운로드/이동 기능을 신규 서버로 이관한다. 이관 후 신규 서버는:

- AWS SDK v2 기반 S3 클라이언트를 `global/config`에서 제공하고,
- `media` 도메인이 presigned URL 발급(업로드/다운로드)과 임시→영구 오브젝트 확정(commit)을 `/api/v1/media/**` 로 제공한다.

memo·album·kindergarten-change·thumbnail 등 **도메인별 S3 소비**는 이 티켓 범위가 아니며, 각 도메인의 epic 하위 후속 작업에서 이 `media` 도메인의 포트/어댑터 또는 자체 outbound 포트로 처리한다.

## 작업 범위

### 이관 대상 — 원본·대상

| 레거시 (`daeng_v1_back`) | 신규 서버 | 판정 |
|---|---|---|
| `config/S3Config.java` (`AmazonS3` 빈, AWS SDK v1, `DefaultAWSCredentialsProviderChain`) | `global/config/S3ClientConfig.kt` — AWS SDK v2 `S3Client` + `S3Presigner` 빈, `DefaultCredentialsProvider`, region from config | `REDESIGN` (SDK v1 → v2) |
| `config/S3Props.java` (`aws.s3` prefix) | `global/config/S3Properties.kt` (`@ConfigurationProperties("aws.s3")`) — region, bucket, presign TTL | `REDESIGN` |
| `GET /api/v0/s3/image/pre-signed-url/upload?path=` (프론트: `src/shared/lib/media/api/getUploadImage.ts`) | `POST /api/v1/media/upload-urls` | `REDESIGN` |
| `GET /api/v0/s3/image/pre-signed-url?key=` (프론트: `getPreviewImage.ts`) | `POST /api/v1/media/download-urls` | `REDESIGN` |
| `POST /api/v0/s3/image/move` `{key, path}` (프론트: `moveImage.ts`) | `POST /api/v1/media/commits` | `REDESIGN` |

- `docs/inventory/api.md` L295~297의 3개 행이 이 대상. 전부 `REDESIGN`(0004 보안 이슈: 인증 없이 발급 / 이동 권한 검증 없음).
- `S3ImageUploadService`의 부가 메서드(`head`, `deleteImage`, 배치 발급, 확장자 보존 변형)는 필요한 최소만 신규 유스케이스에 반영한다. `getUploadPreSignedUrls`(배치, 앨범 50장) 같은 도메인 특화 형태는 이관하지 않는다.

### S3 인프라 기본 설정

- AWS SDK v2 의존성 추가 (`software.amazon.awssdk:s3`, `software.amazon.awssdk:s3-transfer-manager` 불필요 — presigner는 `s3` + `aws-crt` 없이 가능. 정확한 아티팩트는 구현 시 확정)
- `S3Client`, `S3Presigner` 빈. 자격증명은 `DefaultCredentialsProvider`(env var `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` 또는 인스턴스/태스크 역할 체인). **자격증명 값은 이 문서에 적지 않는다.**
- `application.yaml`에 `aws.s3` 블록 추가: `region`(`${AWS_S3_REGION:ap-northeast-2}`), `bucket`(`${AWS_S3_BUCKET:kindergarten-image-bucket}`), presign TTL(업로드/다운로드 분리 여부는 구현 시 확정 — 레거시는 단일 `pre-signed-expiration-time`).

### `media` 도메인 (정석형 헥사고날 — ADR 0003)

```
domain/media/
  domain/                         Media 관련 순수 모델/VO (ObjectKey 등)
  application/
    MediaErrorCode.kt             ErrorCode 구현 enum
    port/input/
      IssueUploadUrlUseCase.kt
      IssueDownloadUrlUseCase.kt
      CommitObjectUseCase.kt
    port/output/
      ObjectStoragePort.kt        presign(put/get), copy, delete, head — S3 비의존 인터페이스
    service/
      IssueUploadUrlService.kt
      IssueDownloadUrlService.kt
      CommitObjectService.kt
  adapter/
    inbound/web/
      MediaUploadUrlController.kt
      MediaDownloadUrlController.kt
      MediaCommitController.kt
    outbound/storage/
      S3ObjectStorageAdapter.kt   ObjectStoragePort 구현, S3Client/S3Presigner 주입
```

- `HexagonalArchitectureTest.kt` 규칙 4는 `domain.*.domain..` 와일드카드라 별도 등록 불필요(구현 중 확인 — hexagonal.md §3의 "현재 auth만 등록됨" 문구가 stale이라 같이 정정).
- 컨트롤러는 유스케이스별 분리(hexagonal.md §1).

### API 계약 (v1 신규 — ADR 0012)

| 엔드포인트 | 요청 | 응답 | 인가 |
|---|---|---|---|
| `POST /api/v1/media/upload-urls` | `{contentType}` (허용 목록 검증) | `{url, key, expiresIn}` | 인증 필요. 서버가 `key = tmp/{userCode}/{uuid}.{ext}` 생성 — 클라이언트는 prefix/path를 지정하지 못한다 |
| `POST /api/v1/media/download-urls` | `{key}` | `{url, expiresIn}` | 인증만 요구. key 소유권 검증은 하지 않는다(소비 도메인 책임) |
| `POST /api/v1/media/commits` | `{key, targetPath}` | `{key, url}` | 인증 필요. `key`가 호출자 `tmp/{userCode}/` 아래인지, `targetPath`가 허용 prefix인지 서버가 검증. copy + delete로 영구 위치 이동 |

- 응답 필드명은 레거시(`preSignedUrl`, `key`)를 그대로 쓰지 않고 `url`/`key`로 정리한다. `daeng_v2_front` 레포가 로컬에 없어 프론트 소비 코드 대조는 못 했다 — 프론트 전환은 별도 작업이며(ADR 0012), 필드명 최종 확정 시 프론트와 맞춘다. 아래 "미결 질문" 참고.
- 공통 응답 래퍼는 `global/response/Response.kt`(`Response.success(data)`), 성공 `code`는 `"SUCCESS"`.
- 오류는 `BusinessException(MediaErrorCode.*)`. 후보: `MEDIA_OBJECT_NOT_FOUND`(404), `MEDIA_UNSUPPORTED_CONTENT_TYPE`(400), `MEDIA_FORBIDDEN_KEY`(403).

## 작업 제외 범위

- memo / album / kindergarten-change / school-thumbnail 도메인의 S3 연동 이관 (레거시 `S3Uploader`, `S3UrlSigner`, 각 도메인 서비스) — 각 도메인 epic 하위 후속.
- 레거시 `S3Uploader`(서버 직접 multipart 업로드) 방식 이관 — 신규는 presigned URL 기반 클라이언트 직접 업로드로 통일. 서버 직접 업로드가 필요한 도메인은 그 도메인 작업에서 판단.
- 앨범 `photos/upload-urls` → `photos/commit` 2단계 커밋(도메인 특화) — album 작업 소관.
- 전용 S3 버킷 신설 및 기존 버킷 콘텐츠 이관(`aws s3 sync`) — operations.md에 후속 옵션으로 기록. 이번엔 `kindergarten-image-bucket` 재사용.
- 신규 서버 배포 파이프라인 / secret 주입(Parameter Store·Secrets Manager) — operations.md `secret 관리` `REDESIGN` 항목에 종속. 이 티켓은 코드 + 로컬 검증 + 운영 요구사항 문서화까지.
- 프론트(`daeng_v2_front`) `shared/lib/media/api/*` 의 v1 전환.
- 레거시 `v0` S3 엔드포인트 제거 (ADR 0012: 컷오버까지 레거시가 계속 서비스).

## 방향 논의 및 결정 사항

### 확정 사항

| # | 결정 | 근거 |
|---|---|---|
| 1 | 새 도메인 `media` 신설 (정석형 슬라이스). SDK 클라이언트 빈은 `global/config` | ADR 0003(정석형 통일). 이름은 `s3`가 아니라 `media` — 아웃바운드 포트가 벤더를 숨기는데 패키지명에 벤더를 박으면 추상화와 충돌. 프론트가 이미 `shared/lib/media`로 부름 |
| 2 | 3개 API 전부 `/api/v1/**` 신규 | ADR 0012(신규 서버는 `v0` 미제공). 레거시는 GET으로 URL 발급(비RESTful) + 경로에 동사 — api-migration.md §2 재명명 대상 |
| 3 | 임시 key는 서버 생성 `tmp/{userCode}/{uuid}.{ext}`, 클라이언트 prefix 지정 불가 | 0004 보안 스멜("누구나 자신 명의로 업로드 URL 발급") 제거. 업로드 대상이 호출자 네임스페이스로 강제됨 |
| 4 | 다운로드 presign은 인증만 요구, key 소유권 검증 안 함 | ADR 0007 기본 deny로 "인증 없음"은 자동 해소. 소유권은 key가 특정 도메인 리소스에 연결됐는지의 문제라 소비 도메인 책임. 범용 API는 짧은 TTL만 보장. 유치원 공용 이미지 조회가 안 깨짐 |
| 5 | 범용 commit 유지 (`POST /api/v1/media/commits`), source가 호출자 `tmp/` 아래인지 검증 | 레거시 `moveImage.ts` 흐름 보존. 레거시 `move`의 "임의 path 이동" 권한 공백은 서버 검증으로 메움 |
| 6 | 버킷은 레거시 `kindergarten-image-bucket` 재사용, `${AWS_S3_BUCKET:...}` env 주입 | 새 AWS 리소스·IAM 정책 변경 0. 기존 IAM 키가 이미 접근 권한 보유. 환경 격리 부재는 레거시도 동일한 기존 갭 — KD3-478 범위 아님. 전용 버킷 전환은 env 1줄 + IAM 수정(코드 무변경) |
| 7 | AWS SDK v2 (`software.amazon.awssdk`) | v1은 2024년 유지보수 종료 공지. 신규 구축이므로 지금 전환. `S3Presigner`도 v2가 깔끔 |
| 8 | 로컬 검증 수단은 구현 착수 시 결정 | 설계는 `ObjectStoragePort` 추상화로 테스트 가능하게 유지 |

### 자격증명·버킷에 대한 확인 결과 (참고)

- AWS access key는 IAM 주체(사용자/역할)에 귀속 — 버킷 신설이 새 키를 요구하지 않는다.
- 레거시: IAM 사용자 장기 access key(`AWS_ACCESS_KEY_ID_NEW`/`_SECRET_..._NEW`, GitHub secrets)를 배포 시 EC2 호스트 env로 주입. `DefaultAWSCredentialsProviderChain`이 픽업.
- 신규 서버는 현재 AWS 설정이 전무하고 배포 파이프라인도 없음(operations.md). 운영 자격증명 배선은 이 티켓에서 완결 불가 — 코드/로컬까지만.
- 버킷 간 콘텐츠 이관은 `aws s3 sync`(증분·재실행 가능, key 보존)로 가능. key layout을 그대로 두면 DB의 `s3_key` 컬럼 무수정. prefix 재설계 시 리매핑+DB 갱신 별도 필요. 자체 runbook을 갖는 별도 태스크.

### 미결 질문

- v1 응답 필드명(`url` vs `preSignedUrl`, `expiresIn` 포함 여부) — 프론트 전환 작업에서 `daeng_v2_front` 소비 코드와 맞춰 최종 확정. 현재는 `{url, key, expiresIn}` 잠정.
- presign TTL: 업로드/다운로드를 분리할지, 레거시처럼 단일 값(`local/dev` 1시간, `prod` 20분)으로 갈지 — 구현 시 확정.
- `contentType` 허용 목록의 정확한 범위(image/*만? webp 포함?) — 구현 시 레거시 실사용 + 프론트 업로드 타입 확인해 확정.
- commit의 `targetPath` 허용 prefix 규칙 — 도메인별 소비가 붙기 전까지는 검증 기준이 느슨할 수밖에 없음. 최소 규칙(예: `tmp/` 금지, 절대경로·`..` 금지)만 이번에 두고 도메인 확정은 후속.
- 로컬 검증 수단 (LocalStack 컨테이너 vs 실 S3 + 개발자 자격증명).

### 사용자 승인 기록

- 2026-09-07 — 브랜치 구조(`epic/KD3-477-s3-migration` → `feat/KD3-478-s3-infra-image-upload`), type 접두사 `feat` 승인.
- 2026-09-07 — 작업 범위(S3 인프라 + 범용 이미지 API만, 도메인별 소비 제외), AWS SDK v2, 도메인명 `media` 승인.
- 2026-09-07 — 방향 논의 확정 사항 1~8 전체 승인 ("yes 이대로 작성해줘").

## 완료 확인 기준

- [x] `POST /api/v1/media/upload-urls`: 인증 없으면 401, 인증 시 `tmp/{userCode}/` prefix key와 presigned URL 반환, 허용 안 되는 `contentType`은 400(`MEDIA_UNSUPPORTED_CONTENT_TYPE`) — `MediaEndpointsTest`, `IssueUploadUrlServiceTest`.
- [x] `POST /api/v1/media/download-urls`: 인증 필요, 임의 key에 대해 presigned URL 반환 — `IssueDownloadUrlServiceTest`. **존재하지 않는 key: 사전 head 검증 없이 발급, 404는 GET 시점에 S3가 낸다**(레거시 동일, HEAD 호출 절약).
- [x] `POST /api/v1/media/commits`: 호출자 `tmp/` 밖 key는 403(`MEDIA_FORBIDDEN_KEY`), 정상 시 copy 후 원본 delete, `{key, url}` 반환, 원본 부재 시 404(`MEDIA_OBJECT_NOT_FOUND`), `targetPath` 임시영역/`..` 금지 400(`MEDIA_INVALID_TARGET_PATH`) — `CommitObjectServiceTest`, `MediaEndpointsTest`.
- [x] `HexagonalArchitectureTest` 통과 — 규칙 4 와일드카드가 `media.domain` 자동 포함.
- [x] 단위 테스트: 각 서비스 + `ObjectStoragePort` fake. presigned URL 생성은 실제 `S3Presigner`로 오프라인 검증(`S3ObjectStorageAdapterTest` — 버킷·key·TTL·서명 포함 확인).
- [x] `./gradlew build` green — ktlint(main/test/script) + ArchUnit + 전체 106 테스트.
- [ ] **로컬 S3 스모크 대조 (사람 몫)**: `S3ObjectStorageAdapter`의 `copy`/`delete`/`exists`는 실제 S3 왕복이라 자동 테스트에서 제외됨. 로컬 자격증명 + 개발용 버킷으로 upload presign → PUT → commit(copy+delete) → download presign → GET 한 사이클을 대조하고 결과를 여기 남긴다. (003-migration §4 "로컬 대조" 방식)
- [ ] **Notion API 명세 등록 (사람 몫)**: v1 media 3개 엔드포인트.

### 계약 parity (003-migration §4)

- 3개 API 전부 `REDESIGN` — 레거시 응답과 1:1 대조 대상 아님. 대조 제외 근거: 경로·메서드·응답 필드명·인가 정책을 의도적으로 바꿈(위 확정 사항 2~5). `KEEP` 항목 없음.
- 다만 **프론트가 현재 레거시 `v0` S3 API로 올린 key/URL을 다른 도메인 요청 본문에 넣고 있다**(예: `RegisterUserRequest.profileImage`). v1 전환 전까지 이 값들의 형식(전체 URL vs key)이 신규 API 산출물과 어긋나지 않는지, 프론트 전환 작업에서 확인 대상으로 넘긴다.

## 작업 후 확인 목록

| 문서 | 판정 | 결과 |
|---|---|---|
| `docs/inventory/integrations.md` | 갱신 | S3 행: 진척 `미착수`→`진행중`, 사용 위치에 신규 서버(`media` 도메인·`global/config`) 추가, 범용 부분 v1 계약·인가·버킷·TTL 반영, 도메인별 소비·"outbound 포트 분리"·앨범 2단계는 각 도메인 후속으로 명시 |
| `docs/inventory/api.md` | 갱신 | s3/image 3개 행: 진척 `진행중`, `대상 버전` `v1`, 도메인 `s3-image`→`media`, 근거에 KD3-478 링크·재설계 요지, 후속에 프론트 v1 전환 |
| `docs/inventory/operations.md` | 갱신 | `S3(운영 제공)` 행 신설 — 레거시 자격증명 방식, 신규 서버 필요 env(`AWS_S3_REGION`/`AWS_S3_BUCKET`/자격증명)·IAM 권한, 전용 버킷 신설 시 절차(키 불필요·IAM ARN 추가·`aws s3 sync`), 배포 파이프라인 종속. 자격증명 값 미기재 |
| `docs/domains/media.md` | 신설 | 새 도메인 — 경계·불변식(key 네임스페이스, content-type, commit 소유권, 다운로드 인가), v1 엔드포인트 매핑, 구조, 도메인별 소비와의 관계 |
| `docs/architecture/hexagonal.md` | 갱신 | §3의 "규칙 4는 현재 auth만 등록됨 / 새 도메인 추가 시 등록 필요" 문구가 stale — 실제 테스트는 `domain.*.domain..` 와일드카드라 전 도메인 자동 포함. 표·설명 정정 (repo-wide 참고 문서라 fast dev PR 대상일 수 있음 — 아래 PR 노트) |
| Notion API 명세 | 미완(사람 몫) | v1 media 3개 엔드포인트 등록 (`docs/rules/notion-api-spec-sync.md`) |
| `docs/conventions/*` | 해당 없음 | 새 판단 기준 없음. content-type 허용 목록·key 규칙은 `media` 도메인 한정이라 `domains/media.md`에 둠 |
| `docs/adr/` | 해당 없음 | 되돌리기 어렵거나 여러 도메인에 걸친 신규 결정 없음 — SDK v2 선택, `media` 명명 등은 이 문서에 기록 |
| `build.gradle.kts` | 갱신 | AWS SDK v2 BOM `2.30.0` + `s3` + `url-connection-client`. 문서 아님, PR 포함 |
| `docs/service.md` §6 용어집 | 확인, 변경 없음 | `media`는 사용자 대면 개념이 아니라 인프라성 도메인이라 용어집 추가 안 함 |
