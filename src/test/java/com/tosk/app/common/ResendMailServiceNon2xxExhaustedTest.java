package com.tosk.app.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class ResendMailServiceNon2xxExhaustedTest {

  static class AlwaysNon2xxRestTemplate extends RestTemplate {
    @Override
    public <T> ResponseEntity<T> exchange(
        String url,
        HttpMethod method,
        HttpEntity<?> requestEntity,
        Class<T> responseType,
        Object... uriVariables) {
      return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.BAD_GATEWAY);
    }
  }

  @Test
  void failsAfterMaxAttemptsWithNon2xx() {
    ResendMailService svc =
        new ResendMailService(new AppProperties(), new AlwaysNon2xxRestTemplate(), "key");
    assertThrows(IllegalStateException.class, () -> svc.send("f@e", "t@e", "s", "text", null));
  }
}
