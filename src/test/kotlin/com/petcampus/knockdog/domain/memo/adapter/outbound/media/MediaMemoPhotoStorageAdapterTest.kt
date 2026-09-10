package com.petcampus.knockdog.domain.memo.adapter.outbound.media

import com.petcampus.knockdog.domain.media.application.port.input.CommitObjectCommand
import com.petcampus.knockdog.domain.media.application.port.input.CommitObjectUseCase
import com.petcampus.knockdog.domain.media.application.port.input.CommittedObject
import com.petcampus.knockdog.domain.media.application.port.input.DownloadUrl
import com.petcampus.knockdog.domain.media.application.port.input.IssueDownloadUrlCommand
import com.petcampus.knockdog.domain.media.application.port.input.IssueDownloadUrlUseCase
import com.petcampus.knockdog.domain.media.application.port.output.ObjectStoragePort
import com.petcampus.knockdog.domain.media.application.port.output.PresignedUrl
import com.petcampus.knockdog.domain.media.domain.MediaContentType
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaMemoPhotoStorageAdapterTest {
    private val commit =
        object : CommitObjectUseCase {
            override fun commit(command: CommitObjectCommand): CommittedObject =
                CommittedObject(
                    key = "memo/${command.userCode}/x.webp",
                    url = "https://cdn/memo/${command.userCode}/x.webp",
                )
        }

    private val download =
        object : IssueDownloadUrlUseCase {
            override fun issue(command: IssueDownloadUrlCommand): DownloadUrl =
                DownloadUrl(url = "https://cdn/${command.key}?sig=1", expiresIn = 300)
        }

    private val deleted = mutableListOf<String>()

    private val storage =
        object : ObjectStoragePort {
            override fun createUploadUrl(
                key: ObjectKey,
                contentType: MediaContentType,
            ) = PresignedUrl("", 0)

            override fun createDownloadUrl(key: ObjectKey) = PresignedUrl("", 0)

            override fun exists(key: ObjectKey) = true

            override fun copy(
                source: ObjectKey,
                destination: ObjectKey,
            ) = Unit

            override fun delete(key: ObjectKey) {
                deleted.add(key.value)
            }
        }

    private val adapter = MediaMemoPhotoStorageAdapter(commit, download, storage)

    @Test
    fun `commitUploaded는 media commit 결과의 영구 key를 돌려준다`() {
        val result = adapter.commitUploaded("A1B2C3D4", "tmp/A1B2C3D4/MEMO_ATTACHMENT/u.webp")

        assertEquals("memo/A1B2C3D4/x.webp", result.objectKey)
    }

    @Test
    fun `viewUrlFor는 다운로드 URL을 준다`() {
        assertEquals("https://cdn/memo/A1B2C3D4/x.webp?sig=1", adapter.viewUrlFor("memo/A1B2C3D4/x.webp"))
    }

    @Test
    fun `delete는 ObjectStoragePort로 위임한다`() {
        adapter.delete("memo/A1B2C3D4/x.webp")

        assertTrue(deleted.contains("memo/A1B2C3D4/x.webp"))
    }
}
