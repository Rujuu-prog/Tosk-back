package com.tosk.app.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
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
class JwtFilterDbChecksTest {
  @Autowired MockMvc mockMvc;
  @Autowired JwtService jwtService;
  @Autowired JwtProperties props;

  static class DummyUser implements JwtService.WithSession {
    private final UUID id = UUID.randomUUID();
    private final UUID sid = UUID.randomUUID();

    @Override
    public UUID getId() {
      return id;
    }

    @Override
    public String getEmail() {
      return "nobody@example.com";
    }

    @Override
    public Integer getTokenVersion() {
      return 0;
    }

    @Override
    public UUID getSessionId() {
      return sid;
    }
  }

  @Test
  void userNotFoundCauses401() throws Exception {
    String at = jwtService.issueAccessToken(new DummyUser());
    Cookie c = new Cookie(props.getAccessCookie(), at);
    c.setPath("/");
    mockMvc
        .perform(get("/api/auth/me").cookie(c).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}
