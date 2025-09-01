package com.tosk.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
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
class AuthSessionsApiTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;

  @Test
  void listDetailAndRevokeSessions() throws Exception {
    SignupRequestDTO sr =
        SignupRequestDTO.builder()
            .email("sess@example.com")
            .username("sess")
            .displayName("Sess")
            .password("password-1234")
            .build();
    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(sr)))
        .andExpect(status().isCreated());

    // create another session (login) so that current remains valid after revoking the first
    var login =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            com.tosk.app.auth.dto.LoginRequestDTO.builder()
                                .email("sess@example.com")
                                .password("password-1234")
                                .build())))
            .andExpect(status().isOk())
            .andReturn();

    // list using current cookies
    MvcResult list =
        mockMvc
            .perform(get("/api/auth/sessions").cookie(login.getResponse().getCookies()))
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isOk())
            .andReturn();
    JsonNode arr = om.readTree(list.getResponse().getContentAsString());
    assertThat(arr.isArray()).isTrue();
    int size = arr.size();
    // pick the older session (created at signup) so current login remains valid
    String sid = arr.get(size - 1).get("id").asText();

    // detail
    mockMvc
        .perform(get("/api/auth/sessions/" + sid).cookie(login.getResponse().getCookies()))
        .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
        .andExpect(status().isOk());

    // revoke one
    mockMvc
        .perform(delete("/api/auth/sessions/" + sid).cookie(login.getResponse().getCookies()))
        .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
        .andExpect(status().isNoContent());

    // revoke all (noop now)
    mockMvc
        .perform(
            delete("/api/auth/sessions")
                .param("keepCurrent", "true")
                .cookie(login.getResponse().getCookies()))
        .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
        .andExpect(status().isNoContent());
  }
}
