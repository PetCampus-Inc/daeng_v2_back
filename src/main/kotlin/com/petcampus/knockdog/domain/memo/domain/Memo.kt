package com.petcampus.knockdog.domain.memo.domain

class Memo private constructor(
    val id: MemoId?,
    val userCode: String,
    val targetId: String,
    val content: String?,
) {
    fun withContent(content: String?): Memo {
        validateContent(content)
        return Memo(id, userCode, targetId, content)
    }

    companion object {
        const val CONTENT_MAX_LENGTH = 2000

        fun create(
            userCode: String,
            targetId: String,
            content: String?,
        ): Memo {
            validateContent(content)
            return Memo(null, userCode, targetId, content)
        }

        fun reconstitute(
            id: MemoId,
            userCode: String,
            targetId: String,
            content: String?,
        ): Memo = Memo(id, userCode, targetId, content)

        private fun validateContent(content: String?) {
            require(content == null || content.length <= CONTENT_MAX_LENGTH) {
                "메모는 ${CONTENT_MAX_LENGTH}자 이내입니다."
            }
        }
    }
}
