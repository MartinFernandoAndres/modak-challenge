# Rate-Limited Notification Service

[![Kotlin](https://img.shields.io/badge/kotlin-1.6.10-blue.svg)](https://kotlinlang.org)
[![JUnit5](https://img.shields.io/badge/JUnit-5.8.2-green.svg)](https://junit.org/junit5/)

A robust notification service with configurable rate limiting per notification type and user.

## Features

- Type-specific rate limiting
- Thread-safe implementation
- Multiple time windows (minutes, hours, days)
- Independent user/type tracking
- Custom exception handling

## Getting Started

### Prerequisites

- JDK 17+
- Maven

### Installation

```bash
git clone https://github.com/MartinFernandoAndres/modak.git
cd modak
```

### Configuration
Define rate limits in application.conf:

```bash
rate-limits {
  status = { max-count = 2, window = "1m" }
  news = { max-count = 1, window = "24h" }
  marketing = { max-count = 3, window = "1h" }
}
```

### Usage
```bash
val gateway = object : Gateway { /* implementation */ }
val rules = mapOf(
    "status" to RateLimitRule(2, Duration.ofMinutes(1))
)

val service = NotificationServiceImpl(gateway, rules)

try {
    service.send("status", "user123", "Server update")
} catch (e: RateLimitExceededException) {
    println("Rate limit exceeded: ${e.message}")
}
```

### Testing
Run tests with:

```bash
mvn clean test
```

Test Types:

* Unit tests: Validate individual components

* Integration tests: Full system validation

* Edge case tests: Boundary conditions

### API Documentation
NotificationService

```bash
interface NotificationService {
    fun send(type: String, userId: String, message: String)
}
```

### Exceptions
```bash
RateLimitExceededException: Thrown when rate limit is exceeded
```

### Architecture

```bash
graph TD
    A[Client] --> B[NotificationService]
    B --> C{Rate Limit Check}
    C -->|Allowed| D[Gateway]
    C -->|Blocked| E[Throw Exception]
    D --> F[External Email Service]
```
