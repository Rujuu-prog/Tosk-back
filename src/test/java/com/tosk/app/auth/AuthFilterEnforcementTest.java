package com.tosk.app.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosk.app.auth.dto.SignupRequestDTO;
import com.tosk.app.user.UserRepository;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
      "app.jwt.access-ttl=1s"
    })
class AuthFilterEnforcementTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;
  @Autowired UserRepository userRepo;

  private MvcResult signup(String email, String username) throws Exception {
    SignupRequestDTO sr =
        SignupRequestDTO.builder()
            .email(email)
            .username(username)
            .displayName("Flt")
            .password("password-1234")
            .build();
    return mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(sr)))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
  }

  @Test
  void tokenVersionMismatchReturns401() throws Exception {
    String email = "flt+" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    String username = "flt" + java.util.UUID.randomUUID().toString().substring(0, 6);
    MvcResult r = signup(email, username);
    var user = userRepo.findByEmailIgnoreCase(email).orElseThrow();
    user.setTokenVersion(user.getTokenVersion() + 1);
    userRepo.save(user);
    mockMvc
        .perform(get("/api/auth/me").cookie(r.getResponse().getCookies()))
        .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void expiredAccessTokenReturns401() throws Exception {
    String email = "flt+" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    String username = "flt" + java.util.UUID.randomUUID().toString().substring(0, 6);
    MvcResult r = signup(email, username);
    TimeUnit.MILLISECONDS.sleep(2000);
    mockMvc
        .perform(get("/api/auth/me").cookie(r.getResponse().getCookies()))
        .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  void revokedSessionReturns401() throws Exception {
    String email = "flt+" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    String username = "flt" + java.util.UUID.randomUUID().toString().substring(0, 6);
    MvcResult r = signup(email, username);
    mockMvc
        .perform(post("/api/auth/logout").cookie(r.getResponse().getCookies()))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/auth/me").cookie(r.getResponse().getCookies()))
        .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
        .andExpect(status().isUnauthorized());
  }
}
