package com.petcampus.knockdog.domain.memo.application.port.output

import com.petcampus.knockdog.domain.memo.domain.MemoPhoto

interface SaveMemoPhotoPort {
    fun save(photo: MemoPhoto): MemoPhoto

    fun deleteById(photoId: Long)
}
