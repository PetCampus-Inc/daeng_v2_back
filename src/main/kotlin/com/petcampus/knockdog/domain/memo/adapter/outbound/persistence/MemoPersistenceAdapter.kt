package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.petcampus.knockdog.domain.memo.application.port.output.LoadMemoPort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoSummary
import com.petcampus.knockdog.domain.memo.application.port.output.SaveMemoPort
import com.petcampus.knockdog.domain.memo.domain.Memo
import com.petcampus.knockdog.domain.memo.domain.MemoId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class MemoPersistenceAdapter(
    private val memoJpaRepository: MemoJpaRepository,
) : LoadMemoPort,
    SaveMemoPort {
    @Transactional(readOnly = true)
    override fun findByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): Memo? = memoJpaRepository.findByUserCodeAndTargetId(userCode, targetId)?.toDomain()

    @Transactional(readOnly = true)
    override fun findSummariesByUserCode(userCode: String): List<MemoSummary> =
        memoJpaRepository
            .findAllByUserCodeOrderByUpdatedAtDesc(userCode)
            .map { MemoSummary(targetId = it.targetId, content = it.content, memoDate = it.updatedAt.toLocalDate()) }

    @Transactional
    override fun save(memo: Memo): Memo {
        val entity =
            memo.id?.let { memoId ->
                memoJpaRepository.findById(memoId.value).orElseThrow().apply {
                    content = memo.content
                    updatedAt = LocalDateTime.now()
                }
            } ?: MemoJpaEntity(userCode = memo.userCode, targetId = memo.targetId, content = memo.content)
        return memoJpaRepository.save(entity).toDomain()
    }
}

private fun MemoJpaEntity.toDomain(): Memo =
    Memo.reconstitute(
        id = MemoId(requireNotNull(id)),
        userCode = userCode,
        targetId = targetId,
        content = content,
    )
