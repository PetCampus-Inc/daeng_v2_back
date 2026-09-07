> 생성: 2026-09-07 13:10 · 최종 수정: 2026-09-07 13:10

# media 도메인

범용 오브젝트 스토리지(S3) 접근을 담당하는 도메인. 이미지·파일의 업로드/다운로드 presigned URL 발급과 임시→영구 확정(commit)만 제공하고, 그 object가 어떤 리소스에 속하는지는 알지 않는다.

- 설계 근거: [`0003`](../adr/0003-헥사고날-정석형-통일.md) 헥사고날 정석형, [`0007`](../adr/0007-인가-기본-deny-전환.md) 인가 기본 deny, [`0012`](../adr/0012-신규-서버-v0-미제공-원칙.md) `v0` 미제공
- 원본: `knockdog_server`(daeng_v1_back)의 `config/S3*`, `s3imageupload/` 패키지
- 착수 기록: [`docs/work/KD3-478-s3-infra-image-upload.md`](../work/KD3-478-s3-infra-image-upload.md)
- 이름이 `s3`가 아니라 `media`인 이유: 아웃바운드 포트(`ObjectStoragePort`)가 저장소 벤더를 숨기는데 도메인명에 벤더를 박으면 추상화와 충돌한다. 프론트도 이 기능을 `shared/lib/media/api/*`로 부른다.

## 1. 경계와 불변식

| 항목 | 규칙 |
|---|---|
| 임시 업로드 key | 서버가 생성한다 — `tmp/{userCode}/{uuid}.{ext}`. 클라이언트는 prefix/경로를 지정할 수 없다(레거시 `path` 파라미터 제거 — 0004 보안 이슈) |
| 업로드 content-type | `MediaContentType` 허용 목록(`image/jpeg`, `image/png`, `image/webp`)만. 그 외 400 |
| commit 소유권 | source key가 호출자의 `tmp/{userCode}/` 아래일 때만 허용. 아니면 403 |
| commit 대상 경로 | `tmp/` 하위·절대경로·`..` 금지. object는 `{targetPath}/{원본 filename}`으로 이동(copy 후 원본 delete) |
| 다운로드 presign | 인증만 요구한다. 임의 key에 대해 발급하며 **key 소유권은 검증하지 않는다** — 그 key를 리소스에 연결한 소비 도메인의 책임이다. 범용 API는 짧은 TTL만 보장 |
| 버킷 | 단일 버킷(`aws.s3.bucket`, env 주입). 레거시 `kindergarten-image-bucket` 재사용 중 — 전용 버킷 전환은 [`operations.md`](../inventory/operations.md) 참고 |
| presign TTL | `aws.s3.presign.upload-ttl`(기본 10m) / `download-ttl`(기본 5m) |

## 2. 엔드포인트 (전부 `v1` 신규 — 레거시 `GET /api/v0/s3/image/**` 재설계)

| v1 | 레거시 | 요청 | 응답(`data`) |
|---|---|---|---|
| `POST /api/v1/media/upload-urls` | `GET /s3/image/pre-signed-url/upload?path=` | `{contentType}` | `{url, key, expiresIn}` |
| `POST /api/v1/media/download-urls` | `GET /s3/image/pre-signed-url?key=` | `{key}` | `{url, expiresIn}` |
| `POST /api/v1/media/commits` | `POST /s3/image/move {key,path}` | `{key, targetPath}` | `{key, url}` |

- 응답 필드명을 레거시(`preSignedUrl`)에서 `url`로 정리했다. 프론트(`daeng_v2_front`)의 `shared/lib/media/api/*` 전환은 별도 작업이며, 그때 필드명을 최종 확정한다.
- 오류 코드: `MEDIA_UNSUPPORTED_CONTENT_TYPE`(400), `MEDIA_OBJECT_NOT_FOUND`(404), `MEDIA_FORBIDDEN_KEY`(403), `MEDIA_INVALID_TARGET_PATH`(400).

## 3. 구조

```
domain/media/
  domain/            MediaContentType, ObjectKey(VO — 경로 탈출·절대경로 차단, tmp 네임스페이스 판별)
  application/
    MediaErrorCode.kt
    port/input/       IssueUploadUrlUseCase, IssueDownloadUrlUseCase, CommitObjectUseCase
    port/output/      ObjectStoragePort (S3 비의존), PresignedUrl
    service/          각 UseCase 구현
  adapter/
    inbound/web/      MediaUploadUrlController, MediaDownloadUrlController, MediaCommitController
    outbound/storage/ S3ObjectStorageAdapter (AWS SDK v2 S3Client·S3Presigner)
```

- S3 SDK 빈(`S3Client`, `S3Presigner`)과 `S3Properties`는 `global/config/`에 둔다 — 여러 도메인이 공유할 인프라라서다.
- presigned URL 생성은 네트워크 호출이 아니라 서명 연산이므로 실제 S3 없이 단위 테스트한다. copy/delete/exists의 S3 왕복은 로컬 스모크 테스트로 대조한다.

## 4. 도메인별 소비와의 관계

memo(첨부), album(사진), kindergarten(가격표 이미지·썸네일), kindergarten-change(증빙)는 각자 S3를 쓴다. 이 도메인은 그 소비의 **공통 앞단**(업로드 발급·확정)만 제공한다. 각 도메인이 `media`의 `ObjectStoragePort`를 재사용할지, 자체 outbound 포트를 둘지는 그 도메인 작업에서 결정한다(integrations.md "presigned URL 발급을 도메인별 outbound 포트로 분리" 방향). 레거시 `S3Uploader`(서버 직접 multipart 업로드)는 이관하지 않았다 — 신규는 presigned URL 기반 클라이언트 직접 업로드로 통일.
