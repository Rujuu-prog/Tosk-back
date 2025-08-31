package com.tosk.app.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditLoggerTest {
  @Test
  void hashHandlesNullInput() {
    String h = AuditLogger.hash(null);
    assertThat(h).isEqualTo("na");
  }

  @Test
  void hashComputesForNonNull() {
    String h = AuditLogger.hash("hello");
    assertThat(h).isNotNull();
    assertThat(h.length()).isEqualTo(16);
  }
}
