package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.petcampus.knockdog.domain.memo.application.port.output.LoadMemoPhotoPort
import com.petcampus.knockdog.domain.memo.application.port.output.SaveMemoPhotoPort
import com.petcampus.knockdog.domain.memo.domain.MemoPhoto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemoPhotoPersistenceAdapter(
    private val memoPhotoJpaRepository: MemoPhotoJpaRepository,
) : LoadMemoPhotoPort,
    SaveMemoPhotoPort {
    @Transactional(readOnly = true)
    override fun findAllByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): List<MemoPhoto> =
        memoPhotoJpaRepository
            .findAllByUserCodeAndTargetIdOrderBySortOrder(userCode, targetId)
            .map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findById(photoId: Long): MemoPhoto? = memoPhotoJpaRepository.findById(photoId).orElse(null)?.toDomain()

    @Transactional(readOnly = true)
    override fun countByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): Int = memoPhotoJpaRepository.countByUserCodeAndTargetId(userCode, targetId)

    @Transactional
    override fun save(photo: MemoPhoto): MemoPhoto =
        memoPhotoJpaRepository
            .save(
                MemoPhotoJpaEntity(
                    userCode = photo.userCode,
                    targetId = photo.targetId,
                    objectKey = photo.objectKey,
                    sortOrder = photo.sortOrder,
                ),
            ).toDomain()

    @Transactional
    override fun deleteById(photoId: Long) {
        memoPhotoJpaRepository.deleteById(photoId)
    }
}

private fun MemoPhotoJpaEntity.toDomain(): MemoPhoto =
    MemoPhoto.reconstitute(
        id = requireNotNull(id),
        userCode = userCode,
        targetId = targetId,
        objectKey = objectKey,
        sortOrder = sortOrder,
    )
