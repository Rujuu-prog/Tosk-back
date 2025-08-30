package com.tosk.app.common;

public interface MailService {
  void send(String from, String to, String subject, String textBody, String htmlBody);
}
