package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.petcampus.knockdog.domain.memo.application.port.output.LoadFreeMemoPort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoSummary
import com.petcampus.knockdog.domain.memo.application.port.output.SaveFreeMemoPort
import com.petcampus.knockdog.domain.memo.domain.FreeMemo
import com.petcampus.knockdog.domain.memo.domain.MemoId
import com.petcampus.knockdog.domain.memo.domain.MemoPhoto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemoPersistenceAdapter(
    private val memoJpaRepository: MemoJpaRepository,
    private val memoPhotoJpaRepository: MemoPhotoJpaRepository,
) : LoadFreeMemoPort,
    SaveFreeMemoPort {
    @Transactional(readOnly = true)
    override fun findByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): FreeMemo? = memoJpaRepository.findByUserCodeAndTargetId(userCode, targetId)?.let { assemble(it) }

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
        val savedRoot = memoJpaRepository.save(entity)
        val rootId = requireNotNull(savedRoot.id)

        memoPhotoJpaRepository.deleteAllByMemoId(rootId)
        memoPhotoJpaRepository.flush()
        memoPhotoJpaRepository.saveAll(
            memo.photos.mapIndexed { index, photo ->
                MemoPhotoJpaEntity(memoId = rootId, objectKey = photo.objectKey, sortOrder = index)
            },
        )

        return assemble(savedRoot)
    }

    private fun assemble(entity: MemoJpaEntity): FreeMemo {
        val id = requireNotNull(entity.id)
        val photos =
            memoPhotoJpaRepository
                .findAllByMemoIdOrderBySortOrder(id)
                .map { MemoPhoto(objectKey = it.objectKey, sortOrder = it.sortOrder) }
        return FreeMemo.reconstitute(
            id = MemoId(id),
            userCode = entity.userCode,
            targetId = entity.targetId,
            content = entity.content,
            photos = photos,
        )
    }
}
