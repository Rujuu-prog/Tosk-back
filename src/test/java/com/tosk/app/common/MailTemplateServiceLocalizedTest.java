package com.tosk.app.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MailTemplateServiceLocalizedTest {
  @Test
  void renderLocalizedFindsLocaleSpecificTemplate() {
    MailTemplateService svc = new MailTemplateService();
    String body =
        svc.renderLocalized(
            "mail/only_localized", "ja", "txt", Map.of("subject", "S", "link", "L"));
    assertThat(body).contains("only_localized ja");
  }
}
