package com.tosk.app.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MailTemplateServiceNullVarsTest {
  @Test
  void renderHandlesNullVars() {
    MailTemplateService svc = new MailTemplateService();
    String body = svc.render("templates/mail/verification.txt", null);
    assertThat(body).contains("Subject:");
  }
}
