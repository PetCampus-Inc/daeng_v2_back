package com.petcampus.knockdog.domain.memo.adapter.outbound.media

import com.petcampus.knockdog.domain.media.application.port.input.CommitObjectCommand
import com.petcampus.knockdog.domain.media.application.port.input.CommitObjectUseCase
import com.petcampus.knockdog.domain.media.application.port.input.IssueDownloadUrlCommand
import com.petcampus.knockdog.domain.media.application.port.input.IssueDownloadUrlUseCase
import com.petcampus.knockdog.domain.memo.application.port.output.CommittedPhoto
import com.petcampus.knockdog.domain.memo.application.port.output.MemoPhotoStoragePort
import org.springframework.stereotype.Component

@Component
class MediaMemoPhotoStorageAdapter(
    private val commitObjectUseCase: CommitObjectUseCase,
    private val issueDownloadUrlUseCase: IssueDownloadUrlUseCase,
) : MemoPhotoStoragePort {
    override fun commitUploaded(
        userCode: String,
        uploadedKey: String,
    ): CommittedPhoto {
        val committed = commitObjectUseCase.commit(CommitObjectCommand(userCode = userCode, key = uploadedKey))
        return CommittedPhoto(objectKey = committed.key)
    }

    override fun viewUrlFor(objectKey: String): String = issueDownloadUrlUseCase.issue(IssueDownloadUrlCommand(objectKey)).url
}
