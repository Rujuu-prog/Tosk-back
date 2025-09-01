package com.tosk.app.common;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class ResendMailServiceRetryTest {

  static class FlakyRestTemplate extends RestTemplate {
    private static final int FIRST_CALL = 1;
    int calls = 0;

    @Override
    public <T> ResponseEntity<T> exchange(
        String url,
        HttpMethod method,
        HttpEntity<?> requestEntity,
        Class<T> responseType,
        Object... uriVariables)
        throws RestClientException {
      calls++;
      if (calls == FIRST_CALL) {
        // first: non-2xx
        return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      // second: 2xx
      return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.OK);
    }
  }

  @Test
  void retriesOnceThenSucceeds() {
    AppProperties props = new AppProperties();
    FlakyRestTemplate rt = new FlakyRestTemplate();
    ResendMailService svc = new ResendMailService(props, rt, "dummy-api-key");
    assertThatCode(() -> svc.send("from@e", "to@e", "s", "text", null)).doesNotThrowAnyException();
  }
}
