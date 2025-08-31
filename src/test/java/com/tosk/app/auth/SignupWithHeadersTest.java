package com.tosk.app.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosk.app.auth.dto.SignupRequestDTO;
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
class SignupWithHeadersTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;

  @Test
  void signupRespectsForwardedForAndUserAgent() throws Exception {
    SignupRequestDTO sr =
        SignupRequestDTO.builder()
            .email("hdr+" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com")
            .username("hdr" + java.util.UUID.randomUUID().toString().substring(0, 6))
            .displayName("Hdr")
            .password("password-1234")
            .build();
    mockMvc
        .perform(
            post("/api/auth/signup")
                .header("X-Forwarded-For", "203.0.113.1, 198.51.100.2")
                .header("User-Agent", "JUnit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(sr)))
        .andExpect(status().isCreated());
  }
}
