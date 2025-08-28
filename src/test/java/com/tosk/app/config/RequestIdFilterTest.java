package com.tosk.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  private static final String HEADER_REQ_ID = "X-Request-Id";
  private static final String SAMPLE_ID = "abc-123";

  @Test
  // ヘッダ未指定時に新規リクエストIDが生成されレスポンスヘッダに設定されることを検証する。
  void generatesRequestIdWhenMissing_setsHeader() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter(HEADER_REQ_ID);
    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, (request, response) -> {});
    assertThat(res.getHeader(HEADER_REQ_ID)).isNotBlank();
  }

  @Test
  // ヘッダ未指定でもフィルタチェーンが継続実行されることを検証する。
  void generatesRequestIdWhenMissing_proceedsFilterChain() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter(HEADER_REQ_ID);
    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();
    AtomicBoolean proceeded = new AtomicBoolean(false);
    filter.doFilter(req, res, (request, response) -> proceeded.set(true));
    assertThat(proceeded.get()).isTrue();
  }

  @Test
  // 指定されたリクエストIDがそのままレスポンスヘッダに反映されることを検証する。
  void keepsProvidedRequestId() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter(HEADER_REQ_ID);
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.addHeader(HEADER_REQ_ID, SAMPLE_ID);
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, new MockFilterChain());

    assertThat(res.getHeader(HEADER_REQ_ID)).isEqualTo(SAMPLE_ID);
  }

  @Test
  // 空白のみのヘッダは未指定扱いとなり新規IDが設定されることを検証する。
  void blankHeaderGeneratesNewId_setsHeader() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter(HEADER_REQ_ID);
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.addHeader(HEADER_REQ_ID, "   ");
    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, new MockFilterChain());
    assertThat(res.getHeader(HEADER_REQ_ID)).isNotBlank();
  }

  @Test
  // 処理後にMDCのrequestIdが必ずクリアされることを検証する。
  void filterClearsMdcAfterProcessing() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter(HEADER_REQ_ID);
    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, new MockFilterChain());
    assertThat(org.slf4j.MDC.get(RequestIdFilter.MDC_KEY)).isNull();
  }

  @Test
  // カスタムヘッダ名が使用され値が設定されることを検証する。
  void customHeaderName_isUsed() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter("X-Custom-ReqId");
    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, new MockFilterChain());
    assertThat(res.getHeader("X-Custom-ReqId")).isNotBlank();
  }

  @Test
  // フィルタチェーン実行中はMDCにrequestIdが格納されていることを検証する。
  void mdcIsPopulatedDuringChain() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter(HEADER_REQ_ID);
    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();
    final AtomicReference<String> seen = new AtomicReference<>();
    filter.doFilter(
        req, res, (request, response) -> seen.set(org.slf4j.MDC.get(RequestIdFilter.MDC_KEY)));
    assertThat(seen.get()).isNotBlank();
  }

  @Test
  // 提供されたリクエストIDがMDCにも伝播することを検証する。
  void providedHeaderPropagatesToMdc() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter(HEADER_REQ_ID);
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.addHeader(HEADER_REQ_ID, SAMPLE_ID);
    MockHttpServletResponse res = new MockHttpServletResponse();
    final AtomicReference<String> seen = new AtomicReference<>();
    filter.doFilter(
        req, res, (request, response) -> seen.set(org.slf4j.MDC.get(RequestIdFilter.MDC_KEY)));
    assertThat(seen.get()).isEqualTo(SAMPLE_ID);
  }
}
