package com.tosk.app.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosk.app.auth.dto.SignupRequestDTO;
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
class AuthLogoutInvalidRtTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;

  @Test
  void logoutIgnoresInvalidRefreshTokenCookie() throws Exception {
    // create a user and get some cookies (not used for logout invalid case)
    SignupRequestDTO sr =
        SignupRequestDTO.builder()
            .email("logout@example.com")
            .username("logoutu")
            .displayName("Logout U")
            .password("password-1234")
            .build();
    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(sr)))
        .andExpect(status().isCreated());

    // Send logout with bogus RT; endpoint should still return 204 (branch in catch)
    Cookie bogusRt = new Cookie("RT", "not-a-jwt");
    bogusRt.setPath("/auth");
    mockMvc.perform(post("/api/auth/logout").cookie(bogusRt)).andExpect(status().isNoContent());
  }
}
