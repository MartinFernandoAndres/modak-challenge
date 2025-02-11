package com.service

import com.exception.RateLimitExceededException
import com.model.RateLimitRule
import com.model.UserTypeKey
import java.time.Clock
import java.time.Instant

class NotificationService (
    private val gateway: Gateway,
    private val rateLimitRules: Map<String, RateLimitRule>,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    private val userTypeTimestamps = mutableMapOf<UserTypeKey, ArrayDeque<Instant>>()

    fun send(type: String, userId: String, message: String) {
        val rule = rateLimitRules[type] ?: run {
            gateway.send(userId, message)
            return
        }

        val now = clock.instant()
        val cutoff = now.minus(rule.window)
        val key = UserTypeKey(userId, type)

        val timestamps = userTypeTimestamps.getOrPut(key) { ArrayDeque() }

        synchronized(timestamps) {
            while (timestamps.firstOrNull()?.isBefore(cutoff) == true) {
                timestamps.removeFirst()
            }

            if (timestamps.size >= rule.maxCount) {
                throw RateLimitExceededException(
                    "Rate limit exceeded for user $userId and type $type. " +
                            "Limit: ${rule.maxCount} per ${rule.window}"
                )
            }

            timestamps.add(now)
        }

        gateway.send(userId, message)
    }
}