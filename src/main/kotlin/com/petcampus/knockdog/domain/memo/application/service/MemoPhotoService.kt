package com.petcampus.knockdog.domain.memo.application.service

import com.petcampus.knockdog.domain.memo.application.MemoErrorCode
import com.petcampus.knockdog.domain.memo.application.port.input.AddMemoPhotoCommand
import com.petcampus.knockdog.domain.memo.application.port.input.AddMemoPhotoUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.DeleteMemoPhotoCommand
import com.petcampus.knockdog.domain.memo.application.port.input.DeleteMemoPhotoUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.MemoView
import com.petcampus.knockdog.domain.memo.application.port.output.LoadMemoPhotoPort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoPhotoStoragePort
import com.petcampus.knockdog.domain.memo.application.port.output.SaveMemoPhotoPort
import com.petcampus.knockdog.domain.memo.domain.MemoPhoto
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemoPhotoService(
    private val loadMemoPhotoPort: LoadMemoPhotoPort,
    private val saveMemoPhotoPort: SaveMemoPhotoPort,
    private val memoPhotoStoragePort: MemoPhotoStoragePort,
) : AddMemoPhotoUseCase,
    DeleteMemoPhotoUseCase {
    @Transactional
    override fun add(command: AddMemoPhotoCommand): MemoView.PhotoView {
        if (!command.photoKey.startsWith("tmp/${command.userCode}/")) {
            throw BusinessException(MemoErrorCode.INVALID_PHOTO_KEY)
        }
        if (loadMemoPhotoPort.countByUserCodeAndTargetId(command.userCode, command.targetId) >= MemoPhoto.MAX_COUNT_PER_TARGET) {
            throw BusinessException(MemoErrorCode.TOO_MANY_PHOTOS)
        }

        val existing = loadMemoPhotoPort.findAllByUserCodeAndTargetId(command.userCode, command.targetId)
        val nextSortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val objectKey = memoPhotoStoragePort.commitUploaded(command.userCode, command.photoKey).objectKey

        val saved =
            saveMemoPhotoPort.save(
                MemoPhoto.create(command.userCode, command.targetId, objectKey, nextSortOrder),
            )
        return MemoView.PhotoView(
            id = requireNotNull(saved.id),
            key = saved.objectKey,
            url = memoPhotoStoragePort.viewUrlFor(saved.objectKey),
        )
    }

    @Transactional
    override fun delete(command: DeleteMemoPhotoCommand) {
        val photo =
            loadMemoPhotoPort
                .findById(command.photoId)
                ?.takeIf { it.userCode == command.userCode && it.targetId == command.targetId }
                ?: throw BusinessException(MemoErrorCode.PHOTO_NOT_FOUND)

        saveMemoPhotoPort.deleteById(command.photoId)
        memoPhotoStoragePort.delete(photo.objectKey)
    }
}
