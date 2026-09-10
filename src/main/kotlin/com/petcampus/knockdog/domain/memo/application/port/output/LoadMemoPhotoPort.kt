package com.petcampus.knockdog.domain.memo.application.port.output

import com.petcampus.knockdog.domain.memo.domain.MemoPhoto

interface LoadMemoPhotoPort {
    fun findAllByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): List<MemoPhoto>

    fun findById(photoId: Long): MemoPhoto?

    fun countByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): Int
}
