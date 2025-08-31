package com.tosk.app.common;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpMailServiceSuccessTest {
  static class NoopSender implements JavaMailSender {
    @Override
    public MimeMessage createMimeMessage() {
      return new MimeMessage((Session) null);
    }

    @Override
    public MimeMessage createMimeMessage(java.io.InputStream contentStream) {
      return new MimeMessage((Session) null);
    }

    @Override
    public void send(MimeMessage mimeMessage) {}

    @Override
    public void send(MimeMessage... mimeMessages) {}

    @Override
    public void send(org.springframework.mail.SimpleMailMessage simpleMessage) {}

    @Override
    public void send(org.springframework.mail.SimpleMailMessage... simpleMessages) {}
  }

  @Test
  void sendTextOnly() {
    SmtpMailService svc = new SmtpMailService(new NoopSender());
    svc.send("from@example.com", "to@example.com", "s", "text body", null);
  }

  @Test
  void sendWithHtmlBody() {
    SmtpMailService svc = new SmtpMailService(new NoopSender());
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class,
        () -> svc.send("from@example.com", "to@example.com", "s", "text", "<p>html</p>"));
  }
}
