package com.petcampus.knockdog.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "aws.s3")
data class S3Properties(
    val region: String,
    val bucket: String,
    val presign: Presign,
) {
    data class Presign(
        val uploadTtl: Duration,
        val downloadTtl: Duration,
    )
}
