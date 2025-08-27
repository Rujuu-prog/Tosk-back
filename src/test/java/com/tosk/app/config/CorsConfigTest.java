package com.tosk.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

class CorsConfigTest {

  @Test
  void allowsLocalhostOrigin_allowsOriginHeader() throws ServletException, IOException {
    CorsConfig cfg = new CorsConfig("");
    CorsFilter filter = cfg.corsFilterForDev();

    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod("OPTIONS");
    req.setRequestURI("/api");
    req.addHeader("Origin", "http://localhost:3000");
    req.addHeader("Access-Control-Request-Method", "GET");

    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, (r, s) -> {});

    assertThat(res.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:3000");
  }

  @Test
  void allowsLocalhostOrigin_setsVaryHeader() throws ServletException, IOException {
    CorsConfig cfg = new CorsConfig("");
    CorsFilter filter = cfg.corsFilterForDev();

    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod("OPTIONS");
    req.setRequestURI("/api");
    req.addHeader("Origin", "http://localhost:3000");
    req.addHeader("Access-Control-Request-Method", "GET");

    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, (r, s) -> {});

    assertThat(res.getHeader("Vary")).contains("Origin");
  }
}
