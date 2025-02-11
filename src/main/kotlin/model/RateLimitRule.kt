package com.model

import java.time.Duration

data class RateLimitRule(
    val maxCount: Int,
    val window: Duration
)