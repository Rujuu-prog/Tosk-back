package com.tosk.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosk.app.auth.dto.LoginRequestDTO;
import com.tosk.app.auth.dto.SignupRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tosk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false"
    })
class AuthControllerIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;

  @Test
  void signupLoginMeFlow() throws Exception {
    SignupRequestDTO sr = new SignupRequestDTO();
    sr.setEmail("user1@example.com");
    sr.setUsername("user1");
    sr.setDisplayName("User One");
    sr.setPassword("password-1234");

    MvcResult r1 =
        mockMvc
            .perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(sr)))
            .andExpect(status().isCreated())
            .andExpect(cookie().exists("AT"))
            .andExpect(cookie().exists("RT"))
            .andReturn();

    String atCookie = r1.getResponse().getCookie("AT").getValue();
    assertThat(atCookie).isNotBlank();

    // me should be OK with AT cookie
    mockMvc
        .perform(get("/api/auth/me").cookie(r1.getResponse().getCookies()))
        .andExpect(status().isOk());

    // login
    LoginRequestDTO lr = new LoginRequestDTO();
    lr.setEmail("user1@example.com");
    lr.setPassword("password-1234");
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(lr)))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("AT"))
        .andExpect(cookie().exists("RT"));
  }

  @Test
  void refreshRotationAndReuseDetection() throws Exception {
    // signup
    SignupRequestDTO sr = new SignupRequestDTO();
    sr.setEmail("user2@example.com");
    sr.setUsername("user2");
    sr.setDisplayName("User Two");
    sr.setPassword("password-1234");
    MvcResult r1 =
        mockMvc
            .perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(sr)))
            .andExpect(status().isCreated())
            .andExpect(cookie().exists("RT"))
            .andReturn();

    var oldRt = r1.getResponse().getCookie("RT");

    // first refresh rotates RT
    MvcResult r2 =
        mockMvc
            .perform(post("/api/auth/refresh").cookie(oldRt))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("RT"))
            .andReturn();

    var newRt = r2.getResponse().getCookie("RT");
    // reuse the old RT should be rejected with 409
    mockMvc.perform(post("/api/auth/refresh").cookie(oldRt)).andExpect(status().isConflict());
  }
}
