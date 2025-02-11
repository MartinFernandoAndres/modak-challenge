package com

import com.exception.RateLimitExceededException
import com.service.Gateway
import com.service.NotificationService
import java.time.Instant
import com.config.AppConfig
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
@EnableConfigurationProperties(AppConfig::class)
class NotificationApplication

fun main(args: Array<String>) {
    val context = runApplication<NotificationApplication>(*args)

    val gateway = object : Gateway {
        override fun send(userId: String, message: String) {
            println("[${Instant.now()}] Sending to $userId: $message")
        }
    }

    val config = context.getBean(AppConfig::class.java)
    val service = NotificationService(gateway, config.toRateLimitRules())

    repeat(4) { i ->
        try {
            service.send("news", "user1", "News $i")
        } catch (e: RateLimitExceededException) {
            println(e.message)
        }
    }
}