package com.petcampus.knockdog.domain.media.adapter.outbound.storage

import com.petcampus.knockdog.domain.media.domain.MediaContentType
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import com.petcampus.knockdog.global.config.S3Properties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.time.Duration
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class S3ObjectStorageAdapterTest {
    private val presigner: S3Presigner =
        S3Presigner
            .builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-access", "test-secret")))
            .build()

    private val properties =
        S3Properties(
            region = "ap-northeast-2",
            bucket = "knockdog-media-test",
            presign = S3Properties.Presign(uploadTtl = Duration.ofMinutes(10), downloadTtl = Duration.ofMinutes(2)),
        )

    private val s3Client: S3Client =
        S3Client
            .builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-access", "test-secret")))
            .build()

    private val adapter = S3ObjectStorageAdapter(s3Client = s3Client, s3Presigner = presigner, properties = properties)

    @AfterEach
    fun tearDown() {
        presigner.close()
        s3Client.close()
    }

    @Test
    fun `업로드 presigned URL은 설정된 버킷과 key, 업로드 TTL을 담는다`() {
        val result = adapter.createUploadUrl(ObjectKey("tmp/A1B2C3D4/photo.webp"), MediaContentType.WEBP)

        assertContains(result.url, "knockdog-media-test")
        assertContains(result.url, "tmp/A1B2C3D4/photo.webp")
        assertContains(result.url, "X-Amz-Expires=600")
        assertContains(result.url, "X-Amz-Signature=")
        assertEquals(600, result.expiresIn)
    }

    @Test
    fun `다운로드 presigned URL은 다운로드 TTL을 담는다`() {
        val result = adapter.createDownloadUrl(ObjectKey("kindergarten/1/thumbnail.webp"))

        assertContains(result.url, "knockdog-media-test")
        assertContains(result.url, "kindergarten/1/thumbnail.webp")
        assertContains(result.url, "X-Amz-Expires=120")
        assertEquals(120, result.expiresIn)
    }

    @Test
    fun `headObject가 NoSuchKeyException이면 exists는 false다`() {
        val mockedS3 = Mockito.mock(S3Client::class.java)
        Mockito
            .doThrow(
                NoSuchKeyException
                    .builder()
                    .statusCode(404)
                    .message("Not Found")
                    .build(),
            ).`when`(mockedS3)
            .headObject(Mockito.any(HeadObjectRequest::class.java))
        val adapterWithMock = S3ObjectStorageAdapter(mockedS3, presigner, properties)

        assertFalse(adapterWithMock.exists(ObjectKey("tmp/A1B2C3D4/photo.webp")))
    }

    @Test
    fun `headObject가 statusCode 404인 S3Exception이면 exists는 false다`() {
        val mockedS3 = Mockito.mock(S3Client::class.java)
        Mockito
            .doThrow(
                S3Exception
                    .builder()
                    .statusCode(404)
                    .message("Not Found")
                    .build(),
            ).`when`(mockedS3)
            .headObject(Mockito.any(HeadObjectRequest::class.java))
        val adapterWithMock = S3ObjectStorageAdapter(mockedS3, presigner, properties)

        assertFalse(adapterWithMock.exists(ObjectKey("tmp/A1B2C3D4/photo.webp")))
    }

    @Test
    fun `headObject가 403이면 exists는 예외를 다시 던진다`() {
        val mockedS3 = Mockito.mock(S3Client::class.java)
        Mockito
            .doThrow(
                S3Exception
                    .builder()
                    .statusCode(403)
                    .message("Forbidden")
                    .build(),
            ).`when`(mockedS3)
            .headObject(Mockito.any(HeadObjectRequest::class.java))
        val adapterWithMock = S3ObjectStorageAdapter(mockedS3, presigner, properties)

        assertFailsWith<S3Exception> { adapterWithMock.exists(ObjectKey("tmp/A1B2C3D4/photo.webp")) }
    }

    @Test
    fun `headObject가 성공하면 exists는 true다`() {
        val mockedS3 = Mockito.mock(S3Client::class.java)
        val adapterWithMock = S3ObjectStorageAdapter(mockedS3, presigner, properties)

        assertTrue(adapterWithMock.exists(ObjectKey("kindergarten/1/thumbnail.webp")))
    }
}
