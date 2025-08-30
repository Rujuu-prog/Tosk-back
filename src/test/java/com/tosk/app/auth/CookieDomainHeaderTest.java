package com.tosk.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
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
      "spring.flyway.enabled=false",
      "app.jwt.cookie-domain=localhost"
    })
class CookieDomainHeaderTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper om;

  @Test
  void signupIncludesDomainInSetCookie() throws Exception {
    SignupRequestDTO sr =
        SignupRequestDTO.builder()
            .email(
                "cdomain+"
                    + java.util.UUID.randomUUID().toString().substring(0, 8)
                    + "@example.com")
            .username("cdomain" + java.util.UUID.randomUUID().toString().substring(0, 6))
            .displayName("CDomain")
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
    String joined = String.join(";", r.getResponse().getHeaders("Set-Cookie"));
    assertThat(joined).contains("Domain=localhost");
  }
}
