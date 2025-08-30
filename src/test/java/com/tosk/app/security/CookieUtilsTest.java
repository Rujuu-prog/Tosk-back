package com.tosk.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

class CookieUtilsTest {
  private static final String SAME_SITE_STRICT = "Strict";
  private static final String EXAMPLE_DOMAIN = "example.com";
  private static final String AUTH_PATH = "/auth";

  @Test
  void buildCookieWithDomainAndPath() {
    Cookie c =
        CookieUtils.buildCookie(
            "N", "V", 10, true, true, SAME_SITE_STRICT, EXAMPLE_DOMAIN, AUTH_PATH);
    assertThat(c.getName()).isEqualTo("N");
    assertThat(c.getValue()).isEqualTo("V");
    assertThat(c.getPath()).isEqualTo(AUTH_PATH);
    assertThat(c.getMaxAge()).isEqualTo(10);
    assertThat(c.isHttpOnly()).isTrue();
    assertThat(c.getSecure()).isTrue();
    assertThat(c.getDomain()).isEqualTo(EXAMPLE_DOMAIN);
  }

  @Test
  void buildCookieDefaultsPathAndNoDomain() {
    Cookie c = CookieUtils.buildCookie("N2", "V2", 0, true, false, SAME_SITE_STRICT, null);
    assertThat(c.getPath()).isEqualTo("/");
    assertThat(c.getDomain()).isNull();
    assertThat(c.getSecure()).isFalse();
  }

  @Test
  void buildCookieTreatsBlankPathAsRoot() {
    Cookie c =
        CookieUtils.buildCookie("N3", "V3", 1, true, true, SAME_SITE_STRICT, EXAMPLE_DOMAIN, "");
    assertThat(c.getPath()).isEqualTo("/");
  }
}
