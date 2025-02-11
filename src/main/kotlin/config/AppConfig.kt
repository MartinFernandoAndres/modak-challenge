package com.config

import com.model.RateLimitRule
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "rate-limits")
data class AppConfig(
    val rules: Map<String, RateLimitConfig> = emptyMap()
) {
    fun toRateLimitRules(): Map<String, RateLimitRule> {
        return rules.mapValues { (_, config) ->
            RateLimitRule(config.maxCount, config.window)
        }
    }
}

data class RateLimitConfig(
    val maxCount: Int,
    val window: Duration
)