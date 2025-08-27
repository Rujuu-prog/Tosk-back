package com.tosk.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  @Test
  void generatesRequestIdWhenMissing_setsHeader() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter("X-Request-Id");
    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, (request, response) -> {});
    assertThat(res.getHeader("X-Request-Id")).isNotBlank();
  }

  @Test
  void generatesRequestIdWhenMissing_proceedsFilterChain() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter("X-Request-Id");
    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();
    AtomicBoolean proceeded = new AtomicBoolean(false);
    filter.doFilter(req, res, (request, response) -> proceeded.set(true));
    assertThat(proceeded.get()).isTrue();
  }

  @Test
  void keepsProvidedRequestId() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter("X-Request-Id");
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.addHeader("X-Request-Id", "abc-123");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, new MockFilterChain());

    assertThat(res.getHeader("X-Request-Id")).isEqualTo("abc-123");
  }
}
