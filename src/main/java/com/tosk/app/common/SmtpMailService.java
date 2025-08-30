package com.tosk.app.common;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "app.mail",
    name = "provider",
    havingValue = "smtp")
public class SmtpMailService implements MailService {
  private final JavaMailSender sender;

  public SmtpMailService(JavaMailSender sender) {
    this.sender = sender;
  }

  @Override
  public void send(String from, String to, String subject, String textBody, String htmlBody) {
    try {
      MimeMessage msg = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      if (htmlBody != null) {
        helper.setText(textBody == null ? "" : textBody, htmlBody);
      } else {
        helper.setText(textBody == null ? "" : textBody, false);
      }
      sender.send(msg);
    } catch (MessagingException e) {
      throw new IllegalStateException("メール送信に失敗しました", e);
    }
  }
}
