package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.petcampus.knockdog.domain.memo.application.port.output.LoadFreeMemoPort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoSummary
import com.petcampus.knockdog.domain.memo.application.port.output.SaveFreeMemoPort
import com.petcampus.knockdog.domain.memo.domain.FreeMemo
import com.petcampus.knockdog.domain.memo.domain.MemoId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemoPersistenceAdapter(
    private val memoJpaRepository: MemoJpaRepository,
) : LoadFreeMemoPort,
    SaveFreeMemoPort {
    @Transactional(readOnly = true)
    override fun findByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): FreeMemo? = memoJpaRepository.findByUserCodeAndTargetId(userCode, targetId)?.toDomain()

    @Transactional(readOnly = true)
    override fun findSummariesByUserCode(userCode: String): List<MemoSummary> =
        memoJpaRepository
            .findAllByUserCodeOrderByUpdatedAtDesc(userCode)
            .map { MemoSummary(targetId = it.targetId, content = it.content, memoDate = it.updatedAt.toLocalDate()) }

    @Transactional
    override fun save(memo: FreeMemo): FreeMemo {
        val entity =
            memo.id?.let { memoId ->
                memoJpaRepository.findById(memoId.value).orElseThrow().apply { content = memo.content }
            } ?: MemoJpaEntity(userCode = memo.userCode, targetId = memo.targetId, content = memo.content)
        return memoJpaRepository.save(entity).toDomain()
    }
}

private fun MemoJpaEntity.toDomain(): FreeMemo =
    FreeMemo.reconstitute(
        id = MemoId(requireNotNull(id)),
        userCode = userCode,
        targetId = targetId,
        content = content,
        photos = emptyList(),
    )
