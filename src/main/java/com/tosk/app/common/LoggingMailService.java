package com.tosk.app.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(MailService.class)
public class LoggingMailService implements MailService {
  private static final Logger log = LoggerFactory.getLogger(LoggingMailService.class);

  @Override
  public void send(String from, String to, String subject, String textBody, String htmlBody) {
    log.info(
        "MAIL from={} to={} subject={} textLen={} htmlLen={}",
        from,
        to,
        subject,
        textBody == null ? 0 : textBody.length(),
        htmlBody == null ? 0 : htmlBody.length());
  }
}
