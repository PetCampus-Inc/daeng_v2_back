package com.petcampus.knockdog.domain.media.application.service

import com.petcampus.knockdog.domain.media.application.MediaErrorCode
import com.petcampus.knockdog.domain.media.application.port.input.CommitObjectCommand
import com.petcampus.knockdog.domain.media.application.port.input.CommitObjectUseCase
import com.petcampus.knockdog.domain.media.application.port.input.CommittedObject
import com.petcampus.knockdog.domain.media.application.port.output.ObjectStoragePort
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service

/**
 * 임시 업로드 오브젝트를 영구 경로로 확정(copy 후 원본 delete)한다.
 * 레거시 `POST /api/v0/s3/image/move`의 대체 — 레거시엔 없던 소유권 검증을 추가했다.
 */
@Service
class CommitObjectService(
    private val objectStoragePort: ObjectStoragePort,
) : CommitObjectUseCase {
    override fun commit(command: CommitObjectCommand): CommittedObject {
        val source = ObjectKey(command.key)
        if (!source.isInTemporaryAreaOf(command.userCode)) {
            throw BusinessException(MediaErrorCode.FORBIDDEN_KEY, "다른 사용자의 임시 오브젝트입니다: ${command.key}")
        }

        val targetPath = command.targetPath.trim().trimEnd('/')
        validateTargetPath(targetPath)
        val destination = ObjectKey("$targetPath/${source.filename}")

        if (!objectStoragePort.exists(source)) {
            throw BusinessException(MediaErrorCode.OBJECT_NOT_FOUND, command.key)
        }

        objectStoragePort.copy(source, destination)
        objectStoragePort.delete(source)

        val download = objectStoragePort.createDownloadUrl(destination)
        return CommittedObject(key = destination.value, url = download.url)
    }

    private fun validateTargetPath(path: String) {
        val invalid =
            path.isBlank() ||
                path.startsWith("/") ||
                path == "tmp" ||
                path.startsWith("tmp/") ||
                path.split("/").any { it.isBlank() || it == "." || it == ".." }
        if (invalid) {
            throw BusinessException(MediaErrorCode.INVALID_TARGET_PATH, "잘못된 저장 경로: $path")
        }
    }
}
