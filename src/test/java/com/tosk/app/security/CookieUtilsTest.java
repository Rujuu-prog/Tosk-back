package com.tosk.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

class CookieUtilsTest {
  @Test
  void buildCookieWithDomainAndPath() {
    Cookie c = CookieUtils.buildCookie("N", "V", 10, true, true, "Strict", "example.com", "/auth");
    assertThat(c.getName()).isEqualTo("N");
    assertThat(c.getValue()).isEqualTo("V");
    assertThat(c.getPath()).isEqualTo("/auth");
    assertThat(c.getMaxAge()).isEqualTo(10);
    assertThat(c.isHttpOnly()).isTrue();
    assertThat(c.getSecure()).isTrue();
    assertThat(c.getDomain()).isEqualTo("example.com");
  }

  @Test
  void buildCookieDefaultsPathAndNoDomain() {
    Cookie c = CookieUtils.buildCookie("N2", "V2", 0, true, false, "Strict", null);
    assertThat(c.getPath()).isEqualTo("/");
    assertThat(c.getDomain()).isNull();
    assertThat(c.getSecure()).isFalse();
  }

  @Test
  void buildCookieTreatsBlankPathAsRoot() {
    Cookie c = CookieUtils.buildCookie("N3", "V3", 1, true, true, "Strict", "example.com", "");
    assertThat(c.getPath()).isEqualTo("/");
  }
}
