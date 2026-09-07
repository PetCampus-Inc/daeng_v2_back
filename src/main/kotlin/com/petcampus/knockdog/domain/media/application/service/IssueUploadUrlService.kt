package com.petcampus.knockdog.domain.media.application.service

import com.petcampus.knockdog.domain.media.application.MediaErrorCode
import com.petcampus.knockdog.domain.media.application.port.input.IssueUploadUrlCommand
import com.petcampus.knockdog.domain.media.application.port.input.IssueUploadUrlUseCase
import com.petcampus.knockdog.domain.media.application.port.input.UploadUrl
import com.petcampus.knockdog.domain.media.application.port.output.ObjectStoragePort
import com.petcampus.knockdog.domain.media.domain.MediaContentType
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service

@Service
class IssueUploadUrlService(
    private val objectStoragePort: ObjectStoragePort,
) : IssueUploadUrlUseCase {
    override fun issue(command: IssueUploadUrlCommand): UploadUrl {
        val contentType =
            MediaContentType.forMimeType(command.contentType)
                ?: throw BusinessException(MediaErrorCode.UNSUPPORTED_CONTENT_TYPE, "지원하지 않는 형식: ${command.contentType}")

        val key = ObjectKey.temporary(command.userCode, contentType)
        val presigned = objectStoragePort.createUploadUrl(key, contentType)

        return UploadUrl(url = presigned.url, key = key.value, expiresIn = presigned.expiresIn)
    }
}
