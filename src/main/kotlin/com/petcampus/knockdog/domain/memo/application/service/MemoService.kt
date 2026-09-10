package com.petcampus.knockdog.domain.memo.application.service

import com.petcampus.knockdog.domain.memo.application.MemoErrorCode
import com.petcampus.knockdog.domain.memo.application.port.input.GetMemoUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.GetMemoedKindergartensUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.MemoView
import com.petcampus.knockdog.domain.memo.application.port.input.MemoedKindergartenView
import com.petcampus.knockdog.domain.memo.application.port.input.SaveMemoCommand
import com.petcampus.knockdog.domain.memo.application.port.input.SaveMemoUseCase
import com.petcampus.knockdog.domain.memo.application.port.output.LoadMemoPort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoPhotoStoragePort
import com.petcampus.knockdog.domain.memo.application.port.output.SaveMemoPort
import com.petcampus.knockdog.domain.memo.domain.Memo
import com.petcampus.knockdog.domain.memo.domain.MemoPhoto
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemoService(
    private val loadMemoPort: LoadMemoPort,
    private val saveMemoPort: SaveMemoPort,
    private val memoPhotoStoragePort: MemoPhotoStoragePort,
) : GetMemoUseCase,
    SaveMemoUseCase,
    GetMemoedKindergartensUseCase {
    @Transactional(readOnly = true)
    override fun get(
        userCode: String,
        targetId: String,
    ): MemoView {
        val memo = loadMemoPort.findByUserCodeAndTargetId(userCode, targetId)
        return MemoView(
            content = memo?.content,
            photos =
                memo?.photos.orEmpty().map {
                    MemoView.PhotoView(key = it.objectKey, url = memoPhotoStoragePort.viewUrlFor(it.objectKey))
                },
        )
    }

    @Transactional
    override fun save(command: SaveMemoCommand): MemoView {
        if (command.content != null && command.content.length > Memo.CONTENT_MAX_LENGTH) {
            throw BusinessException(MemoErrorCode.CONTENT_TOO_LONG)
        }
        if (command.photoKeys.size > Memo.PHOTO_MAX_COUNT) {
            throw BusinessException(MemoErrorCode.TOO_MANY_PHOTOS)
        }

        val photos = resolvePhotos(command.userCode, command.photoKeys)
        val base =
            loadMemoPort.findByUserCodeAndTargetId(command.userCode, command.targetId)
                ?: Memo.create(command.userCode, command.targetId, null)
        saveMemoPort.save(base.withContent(command.content).withPhotos(photos))

        return get(command.userCode, command.targetId)
    }

    @Transactional(readOnly = true)
    override fun list(userCode: String): List<MemoedKindergartenView> =
        loadMemoPort
            .findSummariesByUserCode(userCode)
            .map { MemoedKindergartenView(shopId = it.targetId, content = it.content, memoDate = it.memoDate) }

    private fun resolvePhotos(
        userCode: String,
        photoKeys: List<String>,
    ): List<MemoPhoto> {
        val temporaryPrefix = "tmp/$userCode/"
        val ownedPrefix = "memo/$userCode/"

        val hasForeignKey = photoKeys.any { !it.startsWith(temporaryPrefix) && !it.startsWith(ownedPrefix) }
        val hasDuplicateKey = photoKeys.toSet().size != photoKeys.size
        if (hasForeignKey || hasDuplicateKey) {
            throw BusinessException(MemoErrorCode.INVALID_PHOTO_KEY)
        }

        return photoKeys.mapIndexed { index, key ->
            val objectKey =
                if (key.startsWith(temporaryPrefix)) {
                    memoPhotoStoragePort.commitUploaded(userCode, key).objectKey
                } else {
                    key
                }
            MemoPhoto(objectKey = objectKey, sortOrder = index)
        }
    }
}
