package com.tosk.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.tosk.app.security.CookieUtils;
import jakarta.servlet.http.Cookie;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class BuildSetCookieReflectionTest {
  @Test
  void buildSetCookieOmitsSecureAndHttpOnlyWhenFalse() throws Exception {
    Cookie c = CookieUtils.buildCookie("N", "V", 60, false, false, null, null, "/x");
    Method m = AuthController.class.getDeclaredMethod("buildSetCookie", Cookie.class, String.class);
    m.setAccessible(true);
    String h = (String) m.invoke(null, c, "Lax");
    assertThat(h).contains("N=V").contains("Path=/x");
    assertThat(h).doesNotContain("Secure");
    assertThat(h).doesNotContain("HttpOnly");
    assertThat(h).contains("SameSite=Lax");
    assertThat(h).doesNotContain("Domain=");
  }
}
