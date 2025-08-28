package com.tosk.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

class CorsConfigTest {

  private static final String METHOD_OPTIONS = "OPTIONS";
  private static final String METHOD_GET = "GET";
  private static final String PATH_API = "/api";
  private static final String HEADER_ORIGIN = "Origin";
  private static final String HEADER_ACR_METHOD = "Access-Control-Request-Method";
  private static final String HEADER_AC_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

  @Test
  // ローカルホストOriginのプリフライトでAccess-Control-Allow-Originが返ることを検証する。
  void allowsLocalhostOrigin_allowsOriginHeader() throws ServletException, IOException {
    CorsConfig cfg = new CorsConfig("");
    CorsFilter filter = cfg.corsFilterForDev();

    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod(METHOD_OPTIONS);
    req.setRequestURI(PATH_API);
    req.addHeader(HEADER_ORIGIN, "http://localhost:3000");
    req.addHeader(HEADER_ACR_METHOD, METHOD_GET);

    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, (r, s) -> {});

    assertThat(res.getHeader(HEADER_AC_ALLOW_ORIGIN)).isEqualTo("http://localhost:3000");
  }

  @Test
  // ローカルホストOriginでVaryヘッダにOriginが含まれることを検証する。
  void allowsLocalhostOrigin_setsVaryHeader() throws ServletException, IOException {
    CorsConfig cfg = new CorsConfig("");
    CorsFilter filter = cfg.corsFilterForDev();

    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod(METHOD_OPTIONS);
    req.setRequestURI(PATH_API);
    req.addHeader(HEADER_ORIGIN, "http://localhost:3000");
    req.addHeader(HEADER_ACR_METHOD, METHOD_GET);

    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, (r, s) -> {});

    assertThat(res.getHeader("Vary")).contains("Origin");
  }

  @Test
  // プロパティ指定の許可OriginでAccess-Control-Allow-Originが返ることを検証する。
  void propertyDrivenOrigin_allowsOriginHeader() throws ServletException, IOException {
    CorsConfig cfg = new CorsConfig("http://foo.example:8080");
    CorsFilter filter = cfg.corsFilterForDev();

    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod(METHOD_OPTIONS);
    req.setRequestURI(PATH_API);
    req.addHeader(HEADER_ORIGIN, "http://foo.example:8080");
    req.addHeader(HEADER_ACR_METHOD, METHOD_GET);

    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, (r, s) -> {});

    assertThat(res.getHeader(HEADER_AC_ALLOW_ORIGIN)).isEqualTo("http://foo.example:8080");
  }

  @Test
  // 許可Originに対し認証情報許可ヘッダが付与されること（Allow-Credentials=true）を検証する。
  void propertyDrivenOrigin_allowsCredentials() throws ServletException, IOException {
    CorsConfig cfg = new CorsConfig("http://bar.example");
    CorsFilter filter = cfg.corsFilterForDev();

    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod(METHOD_OPTIONS);
    req.setRequestURI(PATH_API);
    req.addHeader(HEADER_ORIGIN, "http://bar.example");
    req.addHeader(HEADER_ACR_METHOD, METHOD_GET);

    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, (r, s) -> {});

    assertThat(res.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
  }

  @Test
  // 非許可OriginではAccess-Control-Allow-Originが付与されないことを検証する。
  void disallowedOrigin_doesNotSetAllowOrigin() throws ServletException, IOException {
    CorsConfig cfg = new CorsConfig("http://foo.example");
    CorsFilter filter = cfg.corsFilterForDev();

    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod(METHOD_OPTIONS);
    req.setRequestURI(PATH_API);
    req.addHeader(HEADER_ORIGIN, "http://evil.example");
    req.addHeader(HEADER_ACR_METHOD, METHOD_GET);

    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, (r, s) -> {});

    assertThat(res.getHeader(HEADER_AC_ALLOW_ORIGIN)).isNull();
  }

  @Test
  // 複数許可設定時にa.exampleが許可されることを検証する。
  void multipleOrigins_allowA() throws ServletException, IOException {
    CorsConfig cfg = new CorsConfig("http://a.example,http://b.example");
    CorsFilter filter = cfg.corsFilterForDev();

    MockHttpServletRequest reqA = new MockHttpServletRequest();
    reqA.setMethod(METHOD_OPTIONS);
    reqA.setRequestURI(PATH_API);
    reqA.addHeader(HEADER_ORIGIN, "http://a.example");
    reqA.addHeader(HEADER_ACR_METHOD, METHOD_GET);
    MockHttpServletResponse resA = new MockHttpServletResponse();
    filter.doFilter(reqA, resA, (r, s) -> {});
    assertThat(resA.getHeader(HEADER_AC_ALLOW_ORIGIN)).isEqualTo("http://a.example");
  }

  @Test
  // 複数許可設定時にb.exampleが許可されることを検証する。
  void multipleOrigins_allowB() throws ServletException, IOException {
    CorsConfig cfg = new CorsConfig("http://a.example,http://b.example");
    CorsFilter filter = cfg.corsFilterForDev();

    MockHttpServletRequest reqB = new MockHttpServletRequest();
    reqB.setMethod(METHOD_OPTIONS);
    reqB.setRequestURI(PATH_API);
    reqB.addHeader(HEADER_ORIGIN, "http://b.example");
    reqB.addHeader(HEADER_ACR_METHOD, METHOD_GET);
    MockHttpServletResponse resB = new MockHttpServletResponse();
    filter.doFilter(reqB, resB, (r, s) -> {});
    assertThat(resB.getHeader(HEADER_AC_ALLOW_ORIGIN)).isEqualTo("http://b.example");
  }
}
