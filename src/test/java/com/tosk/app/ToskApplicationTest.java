package com.tosk.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ToskApplicationTest {

  @Test
  // アプリケーションコンテキストが正常に起動できることを検証する。
  void contextLoads() {
    // If the application context fails to start, this test will fail.
  }
}
