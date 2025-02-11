package com

import com.exception.RateLimitExceededException
import com.model.RateLimitRule
import com.service.Gateway
import com.service.NotificationService
import java.time.Duration
import java.time.Instant

fun main() {
    val rules = mapOf(
        "status" to RateLimitRule(2, Duration.ofMinutes(1)),
        "news" to RateLimitRule(1, Duration.ofDays(1)),
        "marketing" to RateLimitRule(3, Duration.ofHours(1))
    )

    val gateway = object : Gateway {
        override fun send(userId: String, message: String) {
            println("[${Instant.now()}] Sending to $userId: $message")
        }
    }

    val service = NotificationService(gateway, rules)

    repeat(4) { i ->
        try {
            service.send("news", "user1", "News $i")
        } catch (e: RateLimitExceededException) {
            println(e.message)
        }
    }
}