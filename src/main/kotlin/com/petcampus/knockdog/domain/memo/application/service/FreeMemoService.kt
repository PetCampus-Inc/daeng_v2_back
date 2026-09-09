package com.petcampus.knockdog.domain.memo.application.service

import com.petcampus.knockdog.domain.memo.application.MemoErrorCode
import com.petcampus.knockdog.domain.memo.application.port.input.FreeMemoView
import com.petcampus.knockdog.domain.memo.application.port.input.GetFreeMemoUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.GetMemoedKindergartensUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.MemoedKindergartenView
import com.petcampus.knockdog.domain.memo.application.port.input.SaveFreeMemoCommand
import com.petcampus.knockdog.domain.memo.application.port.input.SaveFreeMemoUseCase
import com.petcampus.knockdog.domain.memo.application.port.output.LoadFreeMemoPort
import com.petcampus.knockdog.domain.memo.application.port.output.SaveFreeMemoPort
import com.petcampus.knockdog.domain.memo.domain.FreeMemo
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FreeMemoService(
    private val loadFreeMemoPort: LoadFreeMemoPort,
    private val saveFreeMemoPort: SaveFreeMemoPort,
) : GetFreeMemoUseCase,
    SaveFreeMemoUseCase,
    GetMemoedKindergartensUseCase {
    @Transactional(readOnly = true)
    override fun get(
        userCode: String,
        targetId: String,
    ): FreeMemoView {
        val memo = loadFreeMemoPort.findByUserCodeAndTargetId(userCode, targetId)
        return FreeMemoView(content = memo?.content, photos = emptyList())
    }

    @Transactional
    override fun save(command: SaveFreeMemoCommand): FreeMemoView {
        if (command.content != null && command.content.length > FreeMemo.CONTENT_MAX_LENGTH) {
            throw BusinessException(MemoErrorCode.CONTENT_TOO_LONG)
        }

        val base =
            loadFreeMemoPort.findByUserCodeAndTargetId(command.userCode, command.targetId)
                ?: FreeMemo.create(command.userCode, command.targetId, null)
        saveFreeMemoPort.save(base.withContent(command.content))

        return FreeMemoView(content = command.content, photos = emptyList())
    }

    @Transactional(readOnly = true)
    override fun list(userCode: String): List<MemoedKindergartenView> =
        loadFreeMemoPort
            .findSummariesByUserCode(userCode)
            .map { MemoedKindergartenView(shopId = it.targetId, content = it.content, memoDate = it.memoDate) }
}
