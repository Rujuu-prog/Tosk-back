package com.tosk.app.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosk.app.auth.dto.LoginRequestDTO;
import com.tosk.app.auth.dto.SignupRequestDTO;
import com.tosk.app.user.UserRepository;
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
class AuthPasswordResetConfirmTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;
  @Autowired PasswordResetTokenRepository resetRepo;
  @Autowired UserRepository userRepo;

  @Test
  void passwordResetConfirmAllowsLoginWithNewPassword() throws Exception {
    SignupRequestDTO sr =
        SignupRequestDTO.builder()
            .email("reset@example.com")
            .username("resetu")
            .displayName("Reset U")
            .password("password-OLD")
            .build();
    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(sr)))
        .andExpect(status().isCreated());

    // start reset
    mockMvc
        .perform(post("/api/auth/password/reset/start").param("email", "reset@example.com"))
        .andExpect(status().isAccepted());
    var user = userRepo.findByEmailIgnoreCase("reset@example.com").orElseThrow();
    var token =
        resetRepo.findAll().stream()
            .filter(t -> t.getUserId().equals(user.getId()))
            .findFirst()
            .orElseThrow()
            .getToken();

    // confirm
    mockMvc
        .perform(
            post("/api/auth/password/reset/confirm")
                .param("token", token)
                .param("password", "password-NEW"))
        .andExpect(status().isOk());

    // login with new password
    LoginRequestDTO lr =
        LoginRequestDTO.builder().email("reset@example.com").password("password-NEW").build();
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(lr)))
        .andExpect(status().isOk());
  }
}
