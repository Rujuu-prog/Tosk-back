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
      "spring.flyway.enabled=false"
    })
class AuthRefreshRevokedSessionTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;

  @Test
  void refreshAfterLogoutReturns401() throws Exception {
    SignupRequestDTO sr =
        SignupRequestDTO.builder()
            .email("revoke-refresh@example.com")
            .username("revoke_refresh")
            .displayName("Revoke Refresh")
            .password("password-1234")
            .build();
    MvcResult r =
        mockMvc
            .perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(sr)))
            .andExpect(status().isCreated())
            .andReturn();

    var rt = r.getResponse().getCookie("RT");
    // logout to revoke the session
    mockMvc
        .perform(post("/api/auth/logout").cookie(r.getResponse().getCookies()))
        .andExpect(status().isNoContent());

    // try refresh with the old RT cookie
    mockMvc.perform(post("/api/auth/refresh").cookie(rt)).andExpect(status().isUnauthorized());
  }
}
