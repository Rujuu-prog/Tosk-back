package com.tosk.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class AuthMailFlowTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;
  @Autowired EmailVerificationTokenRepository emailRepo;
  @Autowired PasswordResetTokenRepository resetRepo;
  @Autowired UserRepository userRepo;

  @Test
  void verificationAndPasswordResetSendsMail() throws Exception {
    SignupRequestDTO sr =
        SignupRequestDTO.builder()
            .email("mail@example.com")
            .username("mailu")
            .displayName("Mail U")
            .password("password-1234")
            .build();
    MvcResult signup =
        mockMvc
            .perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(sr)))
            .andExpect(status().isCreated())
            .andReturn();

    mockMvc
        .perform(post("/api/auth/verification/start").cookie(signup.getResponse().getCookies()))
        .andExpect(status().isAccepted());
    var user = userRepo.findByEmailIgnoreCase("mail@example.com").orElseThrow();
    assertThat(emailRepo.findAll().stream().anyMatch(t -> t.getUserId().equals(user.getId())))
        .isTrue();

    mockMvc
        .perform(post("/api/auth/password/reset/start").param("email", "mail@example.com"))
        .andExpect(status().isAccepted());
    assertThat(resetRepo.findAll().stream().anyMatch(t -> t.getUserId().equals(user.getId())))
        .isTrue();
  }
}
