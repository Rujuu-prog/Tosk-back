package com.tosk.app;

import com.tosk.app.common.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Entry point for the Tosk application. */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ToskApplication {
  public static void main(final String[] args) {
    SpringApplication.run(ToskApplication.class, args);
  }
}
