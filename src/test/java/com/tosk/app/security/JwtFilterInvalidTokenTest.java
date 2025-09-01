package com.tosk.app.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tosk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false"
    })
class JwtFilterInvalidTokenTest {
  @Autowired MockMvc mockMvc;
  @Autowired JwtProperties props;

  @Test
  void invalidAccessTokenYields401Json() throws Exception {
    Cookie bad = new Cookie(props.getAccessCookie(), "not-a-jwt");
    bad.setPath("/");
    mockMvc
        .perform(get("/api/auth/me").cookie(bad).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType("application/json"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        com.tosk.app.common.ApiConstants.ERR_TOKEN_INVALID)));
  }
}
