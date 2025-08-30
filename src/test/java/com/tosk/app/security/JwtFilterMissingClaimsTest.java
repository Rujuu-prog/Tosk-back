package com.tosk.app.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tosk.app.user.UserEntity;
import com.tosk.app.user.UserRepository;
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
class JwtFilterMissingClaimsTest {
  @Autowired MockMvc mockMvc;
  @Autowired JwtService jwtService;
  @Autowired UserRepository userRepository;

  @Test
  void missingSidInAccessTokenReturns401() throws Exception {
    UserEntity u = new UserEntity();
    u.setEmail("no-sid@example.com");
    u.setUsername("nosid");
    u.setDisplayName("No Sid");
    u = userRepository.save(u);
    String at = jwtService.issueAccessToken(u); // no sid
    Cookie atCookie = new Cookie("AT", at);
    atCookie.setHttpOnly(true);
    atCookie.setPath("/");
    mockMvc
        .perform(get("/api/auth/me").cookie(atCookie).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}
