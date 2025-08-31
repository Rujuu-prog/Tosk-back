package com.tosk.app.common;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class ValidationErrorHandlingTest {
  @Autowired MockMvc mockMvc;

  @Test
  void signupWithInvalidBodyReturns400WithValidationCode() throws Exception {
    // Missing required fields will trigger MethodArgumentNotValidException
    String invalidJson = "{}";
    mockMvc
        .perform(
            post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/json"))
        .andExpect(
            content().string(containsString("\"errorCode\":\"" + ApiConstants.ERR_VALIDATION)));
  }
}
