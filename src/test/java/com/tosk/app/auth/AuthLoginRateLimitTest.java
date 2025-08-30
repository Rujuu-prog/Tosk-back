package com.tosk.app.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosk.app.auth.dto.LoginRequestDTO;
import com.tosk.app.auth.dto.SignupRequestDTO;
import org.junit.jupiter.api.BeforeEach;
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
      "spring.flyway.enabled=false",
      "app.security.login.max-attempts=3",
      "app.security.login.window-seconds=300"
    })
class AuthLoginRateLimitTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;

  @BeforeEach
  void setup(@Autowired MockMvc mockMvc) throws Exception {
    // create a valid user
    SignupRequestDTO sr =
        SignupRequestDTO.builder()
            .email("rate@example.com")
            .username("rate")
            .displayName("Rate")
            .password("password-1234")
            .build();
    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(sr)))
        .andExpect(status().isCreated());
  }

  @Test
  void tooManyLoginAttemptsReturns429() throws Exception {
    LoginRequestDTO bad =
        LoginRequestDTO.builder().email("rate@example.com").password("wrong").build();
    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(om.writeValueAsString(bad)))
          .andExpect(status().isUnauthorized());
    }
    // 4th should be 429
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(bad)))
        .andExpect(status().isTooManyRequests());
  }
}
