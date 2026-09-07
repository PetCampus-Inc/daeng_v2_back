package com.petcampus.knockdog.domain.media.application.service

import com.petcampus.knockdog.domain.media.application.port.input.DownloadUrl
import com.petcampus.knockdog.domain.media.application.port.input.IssueDownloadUrlCommand
import com.petcampus.knockdog.domain.media.application.port.input.IssueDownloadUrlUseCase
import com.petcampus.knockdog.domain.media.application.port.output.ObjectStoragePort
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import org.springframework.stereotype.Service

/**
 * 다운로드 presigned URL은 인증만 요구한다 — key 소유권 검증은 그 key를 리소스에 연결한
 * 소비 도메인의 책임이다 (docs/work/KD3-478-s3-infra-image-upload.md 확정 사항 4).
 */
@Service
class IssueDownloadUrlService(
    private val objectStoragePort: ObjectStoragePort,
) : IssueDownloadUrlUseCase {
    override fun issue(command: IssueDownloadUrlCommand): DownloadUrl {
        val presigned = objectStoragePort.createDownloadUrl(ObjectKey(command.key))

        return DownloadUrl(url = presigned.url, expiresIn = presigned.expiresIn)
    }
}
