package com.petcampus.knockdog.domain.memo.application.service

import com.petcampus.knockdog.domain.memo.application.MemoErrorCode
import com.petcampus.knockdog.domain.memo.application.port.input.FreeMemoView
import com.petcampus.knockdog.domain.memo.application.port.input.GetFreeMemoUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.GetMemoedKindergartensUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.MemoedKindergartenView
import com.petcampus.knockdog.domain.memo.application.port.input.SaveFreeMemoCommand
import com.petcampus.knockdog.domain.memo.application.port.input.SaveFreeMemoUseCase
import com.petcampus.knockdog.domain.memo.application.port.output.LoadFreeMemoPort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoPhotoStoragePort
import com.petcampus.knockdog.domain.memo.application.port.output.SaveFreeMemoPort
import com.petcampus.knockdog.domain.memo.domain.FreeMemo
import com.petcampus.knockdog.domain.memo.domain.MemoPhoto
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FreeMemoService(
    private val loadFreeMemoPort: LoadFreeMemoPort,
    private val saveFreeMemoPort: SaveFreeMemoPort,
    private val memoPhotoStoragePort: MemoPhotoStoragePort,
) : GetFreeMemoUseCase,
    SaveFreeMemoUseCase,
    GetMemoedKindergartensUseCase {
    @Transactional(readOnly = true)
    override fun get(
        userCode: String,
        targetId: String,
    ): FreeMemoView {
        val memo = loadFreeMemoPort.findByUserCodeAndTargetId(userCode, targetId)
        return FreeMemoView(
            content = memo?.content,
            photos =
                memo?.photos.orEmpty().map {
                    FreeMemoView.PhotoView(key = it.objectKey, url = memoPhotoStoragePort.viewUrlFor(it.objectKey))
                },
        )
    }

    @Transactional
    override fun save(command: SaveFreeMemoCommand): FreeMemoView {
        if (command.content != null && command.content.length > FreeMemo.CONTENT_MAX_LENGTH) {
            throw BusinessException(MemoErrorCode.CONTENT_TOO_LONG)
        }
        if (command.photoKeys.size > FreeMemo.PHOTO_MAX_COUNT) {
            throw BusinessException(MemoErrorCode.TOO_MANY_PHOTOS)
        }

        val photos = resolvePhotos(command.userCode, command.photoKeys)
        val base =
            loadFreeMemoPort.findByUserCodeAndTargetId(command.userCode, command.targetId)
                ?: FreeMemo.create(command.userCode, command.targetId, null)
        saveFreeMemoPort.save(base.withContent(command.content).withPhotos(photos))

        return get(command.userCode, command.targetId)
    }

    @Transactional(readOnly = true)
    override fun list(userCode: String): List<MemoedKindergartenView> =
        loadFreeMemoPort
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
