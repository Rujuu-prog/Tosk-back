package com.tosk.app.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.login")
public class LoginRateLimiter {
  private int maxAttempts = 5;
  private long windowSeconds = 300; // 5min

  private static class Counter {
    int count;
    long windowStart;
  }

  private final Map<String, Counter> counters = new ConcurrentHashMap<>();
  private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

  public LoginRateLimiter(io.micrometer.core.instrument.MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void checkAndIncrement(String key) {
    long now = Instant.now().getEpochSecond();
    Counter c =
        counters.computeIfAbsent(
            key,
            k -> {
              Counter x = new Counter();
              x.count = 0;
              x.windowStart = now;
              return x;
            });
    synchronized (c) {
      if (now - c.windowStart >= windowSeconds) {
        c.windowStart = now;
        c.count = 0;
      }
      c.count++;
      if (c.count > maxAttempts) {
        meterRegistry.counter("tosk.auth.login.rate_limited_total").increment();
        throw new com.tosk.app.common.TooManyRequestsException("too many login attempts");
      }
    }
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public void setWindowSeconds(long windowSeconds) {
    this.windowSeconds = windowSeconds;
  }
}
