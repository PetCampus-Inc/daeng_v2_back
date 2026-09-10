package com.petcampus.knockdog.domain.memo.domain

class Memo private constructor(
    val id: MemoId?,
    val userCode: String,
    val targetId: String,
    val content: String?,
    val photos: List<MemoPhoto>,
) {
    fun withContent(content: String?): Memo {
        validateContent(content)
        return Memo(id, userCode, targetId, content, photos)
    }

    fun withPhotos(photos: List<MemoPhoto>): Memo {
        require(photos.size <= PHOTO_MAX_COUNT) { "사진은 최대 ${PHOTO_MAX_COUNT}장입니다." }
        return Memo(id, userCode, targetId, content, photos.sortedBy { it.sortOrder })
    }

    companion object {
        const val CONTENT_MAX_LENGTH = 2000
        const val PHOTO_MAX_COUNT = 5

        fun create(
            userCode: String,
            targetId: String,
            content: String?,
        ): Memo {
            validateContent(content)
            return Memo(null, userCode, targetId, content, emptyList())
        }

        fun reconstitute(
            id: MemoId,
            userCode: String,
            targetId: String,
            content: String?,
            photos: List<MemoPhoto>,
        ): Memo = Memo(id, userCode, targetId, content, photos)

        private fun validateContent(content: String?) {
            require(content == null || content.length <= CONTENT_MAX_LENGTH) {
                "메모는 ${CONTENT_MAX_LENGTH}자 이내입니다."
            }
        }
    }
}
