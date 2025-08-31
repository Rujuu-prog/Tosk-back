package com.tosk.app.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ResendMailServiceTest {
  @Test
  void missingApiKeyThrows() {
    AppProperties props = new AppProperties();
    ResendMailService svc = new ResendMailService(props);
    assertThrows(
        IllegalStateException.class,
        () -> svc.send("from@example.com", "to@example.com", "sub", "text", null));
  }
}
