package com.tosk.app.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class ResendMailServiceExhaustedTest {

  static class AlwaysThrowRestTemplate extends RestTemplate {
    @Override
    public <T> ResponseEntity<T> exchange(
        String url,
        HttpMethod method,
        HttpEntity<?> requestEntity,
        Class<T> responseType,
        Object... uriVariables)
        throws RestClientException {
      throw new RestClientException("network down");
    }
  }

  @Test
  void throwsAfterMaxAttempts() {
    AppProperties props = new AppProperties();
    ResendMailService svc = new ResendMailService(props, new AlwaysThrowRestTemplate(), "key");
    assertThrows(IllegalStateException.class, () -> svc.send("f@e", "t@e", "s", "text", null));
  }
}
