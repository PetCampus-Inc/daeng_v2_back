package com.petcampus.knockdog.domain.media.application.service

import com.petcampus.knockdog.domain.media.application.MediaErrorCode
import com.petcampus.knockdog.domain.media.application.port.input.CommitObjectCommand
import com.petcampus.knockdog.domain.media.application.port.input.CommitObjectUseCase
import com.petcampus.knockdog.domain.media.application.port.input.CommittedObject
import com.petcampus.knockdog.domain.media.application.port.output.ObjectStoragePort
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import com.petcampus.knockdog.global.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommitObjectService(
    private val objectStoragePort: ObjectStoragePort,
) : CommitObjectUseCase {
    override fun commit(command: CommitObjectCommand): CommittedObject {
        val source = ObjectKey(command.key)
        if (!source.isInTemporaryAreaOf(command.userCode)) {
            throw BusinessException(MediaErrorCode.FORBIDDEN_KEY, command.key)
        }

        val purpose =
            source.temporaryPurpose()
                ?: throw BusinessException(MediaErrorCode.UNSUPPORTED_PURPOSE, command.key)
        val destination = purpose.permanentKey(command.userCode, source.filename)

        if (!objectStoragePort.exists(source)) {
            throw BusinessException(MediaErrorCode.OBJECT_NOT_FOUND, command.key)
        }

        objectStoragePort.copy(source, destination)
        runCatching { objectStoragePort.delete(source) }
            .onFailure { log.warn("commit 후 임시 오브젝트 삭제 실패, lifecycle 정리 대상: {}", source.value, it) }

        val download = objectStoragePort.createDownloadUrl(destination)
        return CommittedObject(key = destination.value, url = download.url)
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommitObjectService::class.java)
    }
}
