package com.tosk.app.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile("test")
public class InMemoryMailService implements MailService {
  public static record Mail(String to, String subject, String body) {}

  private final List<Mail> box = Collections.synchronizedList(new ArrayList<>());

  @Override
  public void send(String from, String to, String subject, String textBody, String htmlBody) {
    box.add(new Mail(to, subject, textBody));
  }

  public List<Mail> getMails() {
    return new ArrayList<>(box);
  }

  public void clear() {
    box.clear();
  }
}
