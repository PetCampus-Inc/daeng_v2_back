package com.petcampus.knockdog.domain.memo.application.service

import com.petcampus.knockdog.domain.memo.application.MemoErrorCode
import com.petcampus.knockdog.domain.memo.application.port.input.GetMemoUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.GetMemoedKindergartensUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.MemoView
import com.petcampus.knockdog.domain.memo.application.port.input.MemoedKindergartenView
import com.petcampus.knockdog.domain.memo.application.port.input.SaveMemoCommand
import com.petcampus.knockdog.domain.memo.application.port.input.SaveMemoUseCase
import com.petcampus.knockdog.domain.memo.application.port.output.LoadMemoPhotoPort
import com.petcampus.knockdog.domain.memo.application.port.output.LoadMemoPort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoPhotoStoragePort
import com.petcampus.knockdog.domain.memo.application.port.output.SaveMemoPort
import com.petcampus.knockdog.domain.memo.domain.Memo
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemoService(
    private val loadMemoPort: LoadMemoPort,
    private val saveMemoPort: SaveMemoPort,
    private val loadMemoPhotoPort: LoadMemoPhotoPort,
    private val memoPhotoStoragePort: MemoPhotoStoragePort,
) : GetMemoUseCase,
    SaveMemoUseCase,
    GetMemoedKindergartensUseCase {
    @Transactional(readOnly = true)
    override fun get(
        userCode: String,
        targetId: String,
    ): MemoView {
        val content = loadMemoPort.findByUserCodeAndTargetId(userCode, targetId)?.content
        val photos =
            loadMemoPhotoPort.findAllByUserCodeAndTargetId(userCode, targetId).map {
                MemoView.PhotoView(
                    id = requireNotNull(it.id),
                    key = it.objectKey,
                    url = memoPhotoStoragePort.viewUrlFor(it.objectKey),
                )
            }
        return MemoView(content = content, photos = photos)
    }

    @Transactional
    override fun save(command: SaveMemoCommand): MemoView {
        if (command.content != null && command.content.length > Memo.CONTENT_MAX_LENGTH) {
            throw BusinessException(MemoErrorCode.CONTENT_TOO_LONG)
        }

        saveMemoPort.save(Memo.create(command.userCode, command.targetId, command.content))

        return get(command.userCode, command.targetId)
    }

    @Transactional(readOnly = true)
    override fun list(userCode: String): List<MemoedKindergartenView> =
        loadMemoPort
            .findSummariesByUserCode(userCode)
            .map { MemoedKindergartenView(shopId = it.targetId, content = it.content, memoDate = it.memoDate) }
}
