package com.tosk.app.common;

import org.junit.jupiter.api.Test;

class LoggingMailServiceTest {
  @Test
  void logsWithNullBodies() {
    new LoggingMailService().send("from@e", "to@e", "sub", null, null);
  }

  @Test
  void logsWithBothBodies() {
    new LoggingMailService().send("from@e", "to@e", "sub", "text", "<p>html</p>");
  }
}
