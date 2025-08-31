package com.tosk.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.tosk.app.security.CookieUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

class BuildSetCookieReflectionTest {
  private static final String COOKIE_NAME = "N";
  private static final String COOKIE_VALUE = "V";
  private static final String COOKIE_PATH = "/x";
  private static final String SAME_SITE = "Lax";

  private String buildSetCookieForTesting(Cookie c, String sameSite) {
    return AuthController.buildSetCookie(c, sameSite);
  }

  @Test
  void buildSetCookieContainsNameAndValue() {
    Cookie c =
        CookieUtils.buildCookie(
            COOKIE_NAME, COOKIE_VALUE, 60, false, false, null, null, COOKIE_PATH);
    String h = buildSetCookieForTesting(c, SAME_SITE);
    assertThat(h).contains("N=V");
  }

  @Test
  void buildSetCookieContainsPath() {
    Cookie c =
        CookieUtils.buildCookie(
            COOKIE_NAME, COOKIE_VALUE, 60, false, false, null, null, COOKIE_PATH);
    String h = buildSetCookieForTesting(c, SAME_SITE);
    assertThat(h).contains("Path=/x");
  }

  @Test
  void buildSetCookieOmitsSecureWhenFalse() {
    Cookie c =
        CookieUtils.buildCookie(
            COOKIE_NAME, COOKIE_VALUE, 60, false, false, null, null, COOKIE_PATH);
    String h = buildSetCookieForTesting(c, SAME_SITE);
    assertThat(h).doesNotContain("Secure");
  }

  @Test
  void buildSetCookieOmitsHttpOnlyWhenFalse() {
    Cookie c =
        CookieUtils.buildCookie(
            COOKIE_NAME, COOKIE_VALUE, 60, false, false, null, null, COOKIE_PATH);
    String h = buildSetCookieForTesting(c, SAME_SITE);
    assertThat(h).doesNotContain("HttpOnly");
  }

  @Test
  void buildSetCookieContainsSameSite() {
    Cookie c =
        CookieUtils.buildCookie(
            COOKIE_NAME, COOKIE_VALUE, 60, false, false, null, null, COOKIE_PATH);
    String h = buildSetCookieForTesting(c, SAME_SITE);
    assertThat(h).contains("SameSite=Lax");
  }

  @Test
  void buildSetCookieOmitsDomainWhenNotSet() {
    Cookie c =
        CookieUtils.buildCookie(
            COOKIE_NAME, COOKIE_VALUE, 60, false, false, null, null, COOKIE_PATH);
    String h = buildSetCookieForTesting(c, SAME_SITE);
    assertThat(h).doesNotContain("Domain=");
  }
}
