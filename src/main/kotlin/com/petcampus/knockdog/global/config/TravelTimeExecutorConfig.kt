package com.petcampus.knockdog.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Configuration
class TravelTimeExecutorConfig {
    @Bean(TRAVEL_TIME_PAIR_EXECUTOR)
    fun travelTimePairExecutor(): Executor = Executors.newFixedThreadPool(PAIR_POOL_SIZE)

    @Bean(TRAVEL_TIME_CALL_EXECUTOR)
    fun travelTimeCallExecutor(): Executor = Executors.newFixedThreadPool(CALL_POOL_SIZE)

    companion object {
        const val TRAVEL_TIME_PAIR_EXECUTOR = "travelTimePairExecutor"
        const val TRAVEL_TIME_CALL_EXECUTOR = "travelTimeCallExecutor"
        private const val PAIR_POOL_SIZE = 8
        private const val CALL_POOL_SIZE = 16
    }
}
