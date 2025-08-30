package com.tosk.app.security;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class LoginRateLimiterUnitTest {
  @Test
  void windowResetPreventsThrottleWhenWindowSecondsZero() {
    LoginRateLimiter rl = new LoginRateLimiter(new SimpleMeterRegistry());
    rl.setMaxAttempts(1);
    rl.setWindowSeconds(0); // next call triggers window reset branch
    String key = "k" + System.nanoTime();
    assertThatCode(() -> rl.checkAndIncrement(key)).doesNotThrowAnyException();
    // second call would normally exceed, but windowSeconds=0 resets the counter
    assertThatCode(() -> rl.checkAndIncrement(key)).doesNotThrowAnyException();
  }
}
