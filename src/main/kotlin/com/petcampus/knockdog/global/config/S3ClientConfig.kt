package com.petcampus.knockdog.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.time.Duration

@Configuration
class S3ClientConfig(
    private val properties: S3Properties,
) {
    @Bean
    fun s3Client(): S3Client =
        S3Client
            .builder()
            .region(Region.of(properties.region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .overrideConfiguration(
                ClientOverrideConfiguration
                    .builder()
                    .apiCallTimeout(API_CALL_TIMEOUT)
                    .apiCallAttemptTimeout(API_CALL_ATTEMPT_TIMEOUT)
                    .build(),
            ).build()

    @Bean
    fun s3Presigner(): S3Presigner =
        S3Presigner
            .builder()
            .region(Region.of(properties.region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()

    companion object {
        private val API_CALL_TIMEOUT = Duration.ofSeconds(10)
        private val API_CALL_ATTEMPT_TIMEOUT = Duration.ofSeconds(5)
    }
}
