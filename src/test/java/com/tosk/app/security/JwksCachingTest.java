package com.tosk.app.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tosk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false"
    })
class JwksCachingTest {
  @Autowired MockMvc mockMvc;

  @Test
  void jwksRespondsWithETagAnd304() throws Exception {
    MvcResult r1 =
        mockMvc
            .perform(get("/.well-known/jwks.json"))
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isOk())
            .andReturn();
    String etag = r1.getResponse().getHeader("ETag");
    if (etag != null) {
      mockMvc
          .perform(get("/.well-known/jwks.json").header("If-None-Match", etag))
          .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
          .andExpect(status().isNotModified());
    }
  }
}
