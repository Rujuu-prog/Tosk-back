package com.tosk.app.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MailTemplateServiceTest {
  @Test
  void renderLocalizedFallsBackToBase() {
    MailTemplateService svc = new MailTemplateService();
    String body =
        svc.renderLocalized("mail/verification", "zz", "txt", Map.of("subject", "S", "link", "L"));
    assertThat(body).contains("Subject:").contains("L");
  }
}
