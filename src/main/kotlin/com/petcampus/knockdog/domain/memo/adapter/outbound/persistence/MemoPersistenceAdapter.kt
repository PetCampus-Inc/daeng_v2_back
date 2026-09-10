package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.petcampus.knockdog.domain.memo.application.port.output.LoadMemoPort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoSummary
import com.petcampus.knockdog.domain.memo.application.port.output.SaveMemoPort
import com.petcampus.knockdog.domain.memo.domain.Memo
import com.petcampus.knockdog.domain.memo.domain.MemoId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
            .findAllByUserCodeOrderByUpdatedAtDescIdDesc(userCode)
            .map { MemoSummary(targetId = it.targetId, content = it.content, memoDate = it.updatedAt.toLocalDate()) }

    @Transactional
    override fun save(memo: Memo): Memo {
        memoJpaRepository.upsert(memo.userCode, memo.targetId, memo.content)
        return requireNotNull(memoJpaRepository.findByUserCodeAndTargetId(memo.userCode, memo.targetId)).toDomain()
    }
}

private fun MemoJpaEntity.toDomain(): Memo =
    Memo.reconstitute(
        id = MemoId(requireNotNull(id)),
        userCode = userCode,
        targetId = targetId,
        content = content,
    )
