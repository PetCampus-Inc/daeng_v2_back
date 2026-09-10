package com.petcampus.knockdog.domain.memo.application.port.output

interface MemoPhotoStoragePort {
    fun commitUploaded(
        userCode: String,
        uploadedKey: String,
    ): CommittedPhoto

    fun viewUrlFor(objectKey: String): String

    fun delete(objectKey: String)
}

data class CommittedPhoto(
    val objectKey: String,
)
