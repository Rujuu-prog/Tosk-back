package com.tosk.app.common;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "resend")
public class ResendMailService implements MailService {
  private final RestTemplate restTemplate;
  private final AppProperties props;
  private final String apiKeyOverride;

  public ResendMailService(AppProperties props) {
    this.props = props;
    this.restTemplate = new RestTemplate();
    this.apiKeyOverride = null;
  }

  // Visible for tests
  ResendMailService(AppProperties props, RestTemplate restTemplate, String apiKeyOverride) {
    this.props = props;
    this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
    this.apiKeyOverride = apiKeyOverride;
  }

  @Override
  public void send(String from, String to, String subject, String textBody, String htmlBody) {
    String apiKey = apiKeyOverride != null ? apiKeyOverride : System.getenv("RESEND_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("RESEND_API_KEY is not set");
    }

    String url = "https://api.resend.com/emails";
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    h.setBearerAuth(apiKey);
    int attempts = 0;
    int maxAttempts = 3;
    long baseDelayMs = 500;
    while (true) {
      attempts++;
      Map<String, Object> payload = new HashMap<>();
      payload.put("from", from);
      payload.put("to", to);
      payload.put("subject", subject);
      if (textBody != null) {
        payload.put("text", textBody);
      }
      if (htmlBody != null) {
        payload.put("html", htmlBody);
      }
      HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, h);
      try {
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, req, String.class);
        if (res.getStatusCode().is2xxSuccessful()) {
          return;
        }
        if (attempts >= maxAttempts) {
          throw new IllegalStateException("Resend failed: " + res.getStatusCode());
        }
      } catch (RestClientException e) {
        if (attempts >= maxAttempts) {
          throw new IllegalStateException("Resend API error", e);
        }
      }
      try {
        long jitter = (long) (Math.random() * 200);
        long delay = baseDelayMs * (1L << (attempts - 1)) + jitter; // 500, 1000, 2000 (+jitter)
        Thread.sleep(delay);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
