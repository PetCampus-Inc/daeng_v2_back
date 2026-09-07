package com.petcampus.knockdog.domain.media.application.service

import com.petcampus.knockdog.domain.media.application.port.input.DownloadUrl
import com.petcampus.knockdog.domain.media.application.port.input.IssueDownloadUrlCommand
import com.petcampus.knockdog.domain.media.application.port.input.IssueDownloadUrlUseCase
import com.petcampus.knockdog.domain.media.application.port.output.ObjectStoragePort
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import org.springframework.stereotype.Service

@Service
class IssueDownloadUrlService(
    private val objectStoragePort: ObjectStoragePort,
) : IssueDownloadUrlUseCase {
    override fun issue(command: IssueDownloadUrlCommand): DownloadUrl {
        val presigned = objectStoragePort.createDownloadUrl(ObjectKey(command.key))

        return DownloadUrl(url = presigned.url, expiresIn = presigned.expiresIn)
    }
}
