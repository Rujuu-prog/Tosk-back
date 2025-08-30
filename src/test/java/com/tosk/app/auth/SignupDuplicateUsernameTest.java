package com.tosk.app.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class SignupDuplicateUsernameTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;

  @Test
  void duplicateUsernameReturns400ValidationError() throws Exception {
    SignupRequestDTO sr1 =
        SignupRequestDTO.builder()
            .email("dupuser1@example.com")
            .username("dupuser")
            .displayName("Dup U1")
            .password("password-1234")
            .build();
    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(sr1)))
        .andExpect(status().isCreated());

    SignupRequestDTO sr2 =
        SignupRequestDTO.builder()
            .email("dupuser2@example.com")
            .username("dupuser") // duplicate username
            .displayName("Dup U2")
            .password("password-1234")
            .build();

    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(sr2)))
        .andExpect(status().isBadRequest())
        .andExpect(
            content().string(containsString(com.tosk.app.common.ApiConstants.ERR_VALIDATION)));
  }
}
