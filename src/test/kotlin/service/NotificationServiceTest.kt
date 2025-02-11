package service

import com.exception.RateLimitExceededException
import com.model.RateLimitRule
import com.service.Gateway
import com.service.NotificationService
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class NotificationServiceTest {
    private lateinit var gateway: Gateway
    private lateinit var testClock: TestClock
    private lateinit var service: NotificationService

    @BeforeEach
    fun setup() {
        gateway = mockk(relaxed = true)
        testClock = TestClock(Instant.parse("2023-01-01T00:00:00Z"))
    }

    @Test
    fun `should allow sending when under rate limit`() {
        val rules = mapOf("news" to RateLimitRule(1, Duration.ofDays(1)))
        service = NotificationService(gateway, rules, testClock)

        service.send("news", "user1", "message1")

        verify(exactly = 1) { gateway.send("user1", "message1") }
    }

    @Test
    fun `should throw when exceeding rate limit`() {
        val rules = mapOf("news" to RateLimitRule(1, Duration.ofDays(1)))
        service = NotificationService(gateway, rules, testClock)

        service.send("news", "user1", "message1")

        val exception = assertThrows<RateLimitExceededException> {
            service.send("news", "user1", "message2")
        }

        assertTrue(exception.message!!.contains("Limit: 1 per PT24H"))
        verify(exactly = 1) { gateway.send(any(), any()) }
    }

    @Test
    fun `should reset counter after time window`() {
        val rules = mapOf("status" to RateLimitRule(2, Duration.ofMinutes(1)))
        service = NotificationService(gateway, rules, testClock)

        repeat(2) {
            service.send("status", "user1", "message$it")
        }

        // move 61 minutes
        testClock.instant += Duration.ofMinutes(61)

        service.send("status", "user1", "message3")

        verify(exactly = 3) { gateway.send("user1", any()) }
    }

    @Test
    fun `should handle multiple users independently`() {
        val rules = mapOf("news" to RateLimitRule(1, Duration.ofDays(1)))
        service = NotificationService(gateway, rules, testClock)

        service.send("news", "user1", "message1")
        service.send("news", "user2", "message1")

        verify(exactly = 1) { gateway.send("user1", any()) }
        verify(exactly = 1) { gateway.send("user2", any()) }
    }

    @Test
    fun `should handle multiple types independently`() {
        val rules = mapOf(
            "news" to RateLimitRule(1, Duration.ofDays(1)),
            "status" to RateLimitRule(2, Duration.ofMinutes(1))
        )
        service = NotificationService(gateway, rules, testClock)

        service.send("news", "user1", "news message")

        repeat(2) {
            service.send("status", "user1", "status message $it")
        }
        verify(exactly = 1) {
            gateway.send("user1", "news message")
        }
        verify(exactly = 1) {
            gateway.send("user1", "status message 0")
        }
        verify(exactly = 1) {
            gateway.send("user1", "status message 1")
        }
    }

    @Test
    fun `should allow unlimited sends for types without rules`() {
        service = NotificationService(gateway, emptyMap(), testClock)

        repeat(10) {
            service.send("unknown_type", "user1", "message$it")
        }

        verify(exactly = 10) { gateway.send("user1", any()) }
    }

    @Test
    fun `should evict old entries from queue`() {
        val rules = mapOf("marketing" to RateLimitRule(3, Duration.ofHours(1)))
        service = NotificationService(gateway, rules, testClock)

        // full limit
        repeat(3) { service.send("marketing", "user1", "message$it") }

        // move 61 minutes
        testClock.instant += Duration.ofMinutes(61)

        // send 3 more
        repeat(3) { service.send("marketing", "user1", "message${it + 3}") }

        verify(exactly = 6) { gateway.send("user1", any()) }
    }

    @Test
    fun `integration test - full flow with multiple users and types`() {
        val realGateway = object : Gateway {
            val sentMessages = mutableListOf<String>()
            override fun send(userId: String, message: String) {
                sentMessages.add("$userId|$message")
            }
        }
        val rules = mapOf(
            "alert" to RateLimitRule(3, Duration.ofHours(1)),
            "update" to RateLimitRule(5, Duration.ofMinutes(10))
        )
        val fixedClock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC)
        val service = NotificationService(realGateway, rules, fixedClock)

        // User 1 - Alerts
        repeat(3) {
            service.send("alert", "user1", "alert $it")
        }
        // User 1 - Updates
        repeat(5) {
            service.send("update", "user1", "update $it")
        }
        // User 2 - Mix
        service.send("alert", "user2", "alert 0")
        service.send("update", "user2", "update 0")

        assertEquals(10, realGateway.sentMessages.size)

        // check límites
        assertThrows<RateLimitExceededException> {
            service.send("alert", "user1", "extra alert")
        }
        assertThrows<RateLimitExceededException> {
            service.send("update", "user1", "extra update")
        }
        // check user2 allowed
        service.send("alert", "user2", "alert 1")
        service.send("update", "user2", "update 1")
    }
}

class TestClock(var instant: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = instant
}