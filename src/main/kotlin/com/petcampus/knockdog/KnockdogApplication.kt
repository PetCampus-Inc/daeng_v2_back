package com.petcampus.knockdog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KnockdogApplication

fun main(args: Array<String>) {
    runApplication<KnockdogApplication>(*args)
}
