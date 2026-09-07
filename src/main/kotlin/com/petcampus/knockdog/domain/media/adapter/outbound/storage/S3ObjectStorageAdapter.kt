package com.petcampus.knockdog.domain.media.adapter.outbound.storage

import com.petcampus.knockdog.domain.media.application.port.output.ObjectStoragePort
import com.petcampus.knockdog.domain.media.application.port.output.PresignedUrl
import com.petcampus.knockdog.domain.media.domain.MediaContentType
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import com.petcampus.knockdog.global.config.S3Properties
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest

@Component
class S3ObjectStorageAdapter(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val properties: S3Properties,
) : ObjectStoragePort {
    override fun createUploadUrl(
        key: ObjectKey,
        contentType: MediaContentType,
    ): PresignedUrl {
        val ttl = properties.presign.uploadTtl
        val presigned =
            s3Presigner.presignPutObject(
                PutObjectPresignRequest
                    .builder()
                    .signatureDuration(ttl)
                    .putObjectRequest(
                        PutObjectRequest
                            .builder()
                            .bucket(properties.bucket)
                            .key(key.value)
                            .contentType(contentType.mimeType)
                            .build(),
                    ).build(),
            )
        return PresignedUrl(url = presigned.url().toString(), expiresIn = ttl.seconds)
    }

    override fun createDownloadUrl(key: ObjectKey): PresignedUrl {
        val ttl = properties.presign.downloadTtl
        val presigned =
            s3Presigner.presignGetObject(
                GetObjectPresignRequest
                    .builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(
                        GetObjectRequest
                            .builder()
                            .bucket(properties.bucket)
                            .key(key.value)
                            .build(),
                    ).build(),
            )
        return PresignedUrl(url = presigned.url().toString(), expiresIn = ttl.seconds)
    }

    override fun exists(key: ObjectKey): Boolean =
        try {
            s3Client.headObject(
                HeadObjectRequest
                    .builder()
                    .bucket(properties.bucket)
                    .key(key.value)
                    .build(),
            )
            true
        } catch (e: NoSuchKeyException) {
            false
        }

    override fun copy(
        source: ObjectKey,
        destination: ObjectKey,
    ) {
        s3Client.copyObject(
            CopyObjectRequest
                .builder()
                .sourceBucket(properties.bucket)
                .sourceKey(source.value)
                .destinationBucket(properties.bucket)
                .destinationKey(destination.value)
                .build(),
        )
    }

    override fun delete(key: ObjectKey) {
        s3Client.deleteObject(
            DeleteObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(key.value)
                .build(),
        )
    }
}
